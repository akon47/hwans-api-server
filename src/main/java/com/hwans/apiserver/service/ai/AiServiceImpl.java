package com.hwans.apiserver.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.beta.messages.BetaContentBlockParam;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaWebFetchTool20260209;
import com.anthropic.models.beta.messages.BetaWebSearchTool20260209;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.token.RedisRateLimiter;
import com.hwans.apiserver.dto.ai.*;
import com.hwans.apiserver.entity.ai.AiSetting;
import com.hwans.apiserver.entity.ai.AiUsageKind;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.ai.AiSettingRepository;
import com.hwans.apiserver.repository.ai.AiUsageRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.blog.tag.TagRepository;
import com.hwans.apiserver.service.ai.cover.CoverDesign;
import com.hwans.apiserver.service.ai.cover.CoverIcon;
import com.hwans.apiserver.service.ai.cover.CoverRenderer;
import com.hwans.apiserver.service.ai.cover.CoverTheme;
import com.hwans.apiserver.service.attachment.AttachmentService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * AI 글쓰기 서비스 구현체. 계약은 {@code ai-writing-contract.md}(프론트와 공유) 참고.
 * <ul>
 *   <li>Anthropic 호출은 트랜잭션 밖에서 하고, 결과 저장만 {@link AiStore} 의 짧은 트랜잭션으로 한다.</li>
 *   <li>스트리밍은 전용 스레드 풀에서 돌리고(요청 스레드를 붙잡지 않음), 클라이언트가 끊으면 Anthropic 스트림도 닫는다.</li>
 *   <li>모든 호출 전에 회원당 분당 10회 제한과 이번 달 한도를 확인하고, 호출 뒤 사용량·비용을 {@code tb_ai_usage} 에 남긴다.</li>
 * </ul>
 */
@Service
@Slf4j
public class AiServiceImpl implements AiService {
    static final int REQUESTS_PER_MINUTE = 10;
    static final int MAX_CONTINUATIONS = 5;
    static final long STREAM_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(20);

    // 문체 분석·초안에 쓰는 예전 글
    static final int STYLE_MIN_CONTENT_CHARS = 300;
    static final int STYLE_MAX_POSTS = 10;
    static final int STYLE_POST_MAX_CHARS = 4000;
    static final int STYLE_PROFILE_MAX_CHARS = 2000;
    static final int DRAFT_EXCERPT_POSTS = 2;
    static final int DRAFT_EXCERPT_MAX_CHARS = 3000;
    static final int STYLE_REFRESH_DAYS = 30;
    static final int STYLE_REFRESH_NEW_POSTS = 3;

    private static final DateTimeFormatter ANALYZED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Set<String> LENGTHS = Set.of("short", "medium", "long");
    private static final Set<String> REWRITE_INSTRUCTIONS = Set.of("polish", "expand", "shorten", "fix", "custom");
    private static final List<AiSettingsDto.ModelInfo> MODELS = Arrays.stream(AiModel.values())
            .map(m -> AiSettingsDto.ModelInfo.builder()
                    .id(m.getId())
                    .label(m.getLabel())
                    .description(m.getDescription())
                    .inputPerMTok(m.getInputPerMTok())
                    .outputPerMTok(m.getOutputPerMTok())
                    .build())
            .toList();

    private final AiKeyCipher cipher;
    private final AnthropicGateway gateway;
    private final AiStore store;
    private final AiSettingRepository aiSettingRepository;
    private final AiUsageRepository aiUsageRepository;
    private final AccountRepository accountRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final AttachmentService attachmentService;
    private final CoverRenderer coverRenderer;
    private final RedisRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final String coverFooterHost;

    // 스트림 하나가 스레드 하나를 쓴다(SDK 스트림은 블로킹). 동시에 32개를 넘으면 받지 않는다.
    private final ThreadPoolExecutor streamExecutor = new ThreadPoolExecutor(2, 32, 60, TimeUnit.SECONDS,
            new SynchronousQueue<>(), namedThreads("ai-stream-"));
    private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor(namedThreads("ai-sse-ping-"));

    public AiServiceImpl(AiKeyCipher cipher, AnthropicGateway gateway, AiStore store,
                         AiSettingRepository aiSettingRepository, AiUsageRepository aiUsageRepository,
                         AccountRepository accountRepository, PostRepository postRepository, TagRepository tagRepository,
                         AttachmentService attachmentService, CoverRenderer coverRenderer, RedisRateLimiter rateLimiter,
                         ObjectMapper objectMapper,
                         @Value("${blog.web-base-url:https://hwanstory.kr}") String webBaseUrl) {
        this.cipher = cipher;
        this.gateway = gateway;
        this.store = store;
        this.aiSettingRepository = aiSettingRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.accountRepository = accountRepository;
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.attachmentService = attachmentService;
        this.coverRenderer = coverRenderer;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.coverFooterHost = footerHost(webBaseUrl);
    }

    @PreDestroy
    public void shutdown() {
        streamExecutor.shutdownNow();
        pingScheduler.shutdownNow();
    }

    // ===================== 설정 =====================

    @Override
    public AiSettingsDto getSettings(UUID accountId) {
        var setting = aiSettingRepository.findByAccount_Id(accountId).orElse(null);
        var month = YearMonth.now();
        var since = month.atDay(1).atStartOfDay();
        var cost = Optional.ofNullable(aiUsageRepository.sumCostSince(accountId, since)).orElse(BigDecimal.ZERO);
        var model = setting == null ? AiModel.DEFAULT : AiModel.fromIdOrDefault(setting.getModel());
        return AiSettingsDto.builder()
                .available(cipher.isEnabled())
                .configured(setting != null && setting.hasApiKey())
                .keyLast4(setting != null && setting.hasApiKey() ? setting.getKeyLast4() : null)
                .model(model.getId())
                .monthlyLimitUsd(monthlyLimit(setting).doubleValue())
                .usage(AiSettingsDto.Usage.builder()
                        .month(month.toString())
                        .costUsd(cost.setScale(4, RoundingMode.HALF_UP).doubleValue())
                        .requests(aiUsageRepository.countSince(accountId, since))
                        .build())
                .style(styleDto(setting))
                .models(MODELS)
                .build();
    }

    @Override
    public AiSettingsDto updateSettings(UUID accountId, AiSettingsRequestDto request) {
        requireEnabled();
        AiModel model = null;
        if (request.getModel() != null) {
            model = AiModel.fromId(request.getModel()).orElseThrow(() ->
                    new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "지원하지 않는 모델이에요: " + request.getModel()));
        }
        BigDecimal limit = null;
        if (request.getMonthlyLimitUsd() != null) {
            var value = request.getMonthlyLimitUsd();
            if (value.isNaN() || value < 0.5 || value > 500) {
                throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "월 사용 한도는 $0.5~$500 사이로 정해 주세요.");
            }
            limit = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
        }
        String encrypted = null;
        String last4 = null;
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            var apiKey = request.getApiKey().strip();
            if (apiKey.length() < 8 || apiKey.length() > 300 || apiKey.chars().anyMatch(Character::isWhitespace)) {
                throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "API 키 형식이 올바르지 않아요.");
            }
            requireRate(accountId);
            gateway.validateKey(apiKey);
            encrypted = cipher.encrypt(apiKey);
            last4 = apiKey.substring(apiKey.length() - 4);
        }
        store.saveSettings(accountId, encrypted, last4, model, limit);
        return getSettings(accountId);
    }

    @Override
    public void deleteApiKey(UUID accountId) {
        store.clearApiKey(accountId);
    }

    // ===================== 문체 분석 =====================

    @Override
    public AiStyleDto analyzeStyle(UUID accountId) {
        var ctx = prepare(accountId);
        var posts = eligiblePosts(accountId);
        if (posts.size() < 2) {
            throw new RestApiException(ErrorCodes.BadRequest.AI_NOT_ENOUGH_POSTS);
        }
        requireQuota(ctx);
        gateway.withClient(ctx.apiKey(), client -> analyzeStyle(client, ctx, posts));
        return styleDto(aiSettingRepository.findByAccount_Id(accountId).orElse(null));
    }

    /**
     * 문체 프로필을 만들어 저장하고 돌려준다. 사용량은 style 로 따로 남긴다.
     */
    private String analyzeStyle(AnthropicClient client, CallContext ctx, List<AiPrompts.PostExcerpt> posts) {
        var params = AnthropicGateway.baseParams(ctx.model(), 8000, AnthropicGateway.effort(BetaOutputConfig.Effort.LOW))
                .system(AiPrompts.STYLE_SYSTEM)
                .addUserMessage(AiPrompts.styleUser(posts))
                .build();
        BetaMessage message;
        try {
            message = client.beta().messages().create(params);
        } catch (RuntimeException e) {
            throw AnthropicGateway.upstream(e);
        }
        recordUsage(ctx, AiUsageKind.STYLE, AiTokenUsage.of(message.usage()));
        AnthropicGateway.requireCompleted(message);
        var profile = cut(AnthropicGateway.text(message).strip(), STYLE_PROFILE_MAX_CHARS);
        if (profile.isBlank()) {
            throw new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 가 문체 분석 결과를 돌려주지 않았어요. 다시 해 주세요.");
        }
        store.saveStyle(ctx.accountId(), profile, posts.size(), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return profile;
    }

    // ===================== 초안 (SSE) =====================

    @Override
    public ResponseEntity<ResponseBodyEmitter> draft(UUID accountId, AiDraftRequestDto request) {
        requireEnabled();
        var topic = required(request.getRequest(), "request", 2000);
        var notes = optional(request.getNotes(), "notes", 10000);
        var length = request.getLength() == null || request.getLength().isBlank() ? "medium" : request.getLength().strip();
        if (!LENGTHS.contains(length)) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "length 는 short, medium, long 중 하나여야 해요.");
        }
        var webResearch = Boolean.TRUE.equals(request.getWebResearch());
        var ctx = prepare(accountId);
        requireQuota(ctx);

        return startStream(ctx, AiUsageKind.DRAFT, (client, session, meter) -> {
            var posts = eligiblePosts(accountId);
            var style = ctx.setting().getStyleProfile();
            if (needsStyleRefresh(ctx.setting(), accountId) && posts.size() >= 2) {
                session.status("예전 글 " + posts.size() + "편으로 문체를 분석하는 중");
                try {
                    style = analyzeStyle(client, ctx, posts);
                    session.status("문체 분석을 마쳤어요");
                } catch (RuntimeException e) {
                    if (session.isCancelled()) {
                        throw e;
                    }
                    log.warn("style re-analysis before draft failed: {}", e.getMessage());
                    session.status(style == null || style.isBlank() ? "문체 분석에 실패해 기본 문체로 써요" : "문체 분석에 실패해 저장된 문체로 써요");
                }
            }
            var excerpts = posts.stream().limit(DRAFT_EXCERPT_POSTS)
                    .map(p -> new AiPrompts.PostExcerpt(p.title(), cut(p.content(), DRAFT_EXCERPT_MAX_CHARS)))
                    .toList();
            var system = AiPrompts.draftSystem(ctx.authorName(), ctx.blogId(), style, excerpts);
            var maxTokens = switch (length) {
                case "short" -> 12000L;
                case "long" -> 32000L;
                default -> 20000L;
            };
            var builder = AnthropicGateway.baseParams(ctx.model(), maxTokens, AnthropicGateway.effort(BetaOutputConfig.Effort.MEDIUM))
                    .systemOfBetaTextBlockParams(AnthropicGateway.cachedSystem(system));
            if (webResearch) {
                builder.addTool(BetaWebSearchTool20260209.builder().maxUses(5L).build())
                        .addTool(BetaWebFetchTool20260209.builder().maxUses(5L).maxContentTokens(30000L).build());
            }
            var params = builder
                    .addUserMessage(AiPrompts.draftUser(topic, notes, length, webResearch, LocalDate.now()))
                    .build();

            session.status(webResearch ? "자료를 찾아보고 초안을 쓰는 중" : "초안을 쓰는 중");
            var collector = new AiStreamCollector(listener(session), webResearch, true);
            meter.track(collector::usage);
            var stopReason = streamWithContinuation(client, params, collector, session);
            collector.finish();
            if (BetaStopReason.REFUSAL.equals(stopReason)) {
                throw AnthropicGateway.refusal();
            }

            List<AiReferences.Source> sources = List.of();
            if (webResearch) {
                var body = collector.body();
                if (!body.isBlank() && !AiReferences.hasSection(body)) {
                    var collected = collector.collectedSources(10);
                    if (!collected.isEmpty()) {
                        collector.appendText(AiReferences.buildSection(body, collected));
                    }
                }
                sources = AiReferences.parseSection(collector.body());
                if (sources.isEmpty()) {
                    sources = collector.collectedSources(10);
                }
            }
            session.done(donePayload(stopReason, meter.total(), ctx.model(), sources));
        });
    }

    // ===================== 다듬기 (SSE) =====================

    @Override
    public ResponseEntity<ResponseBodyEmitter> rewrite(UUID accountId, AiRewriteRequestDto request) {
        requireEnabled();
        var text = required(request.getText(), "text", 20000);
        var instruction = request.getInstruction() == null ? "" : request.getInstruction().strip();
        if (!REWRITE_INSTRUCTIONS.contains(instruction)) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "instruction 은 polish, expand, shorten, fix, custom 중 하나여야 해요.");
        }
        var custom = "custom".equals(instruction) ? required(request.getCustomInstruction(), "customInstruction", 1000) : null;
        // 프론트는 선택 앞뒤로 2000자씩 + "[다듬을 부분]" 표시를 보낸다. 표시 뒤쪽이 잘리지 않게 조금 넉넉히 받는다.
        var context = request.getContext() == null ? null : cut(request.getContext(), 4500);
        var ctx = prepare(accountId);
        requireQuota(ctx);

        return startStream(ctx, AiUsageKind.REWRITE, (client, session, meter) -> {
            var params = AnthropicGateway.baseParams(ctx.model(), 16000, AnthropicGateway.effort(BetaOutputConfig.Effort.LOW))
                    .systemOfBetaTextBlockParams(AnthropicGateway.cachedSystem(AiPrompts.rewriteSystem(ctx.setting().getStyleProfile())))
                    .addUserMessage(AiPrompts.rewriteUser(text, AiPrompts.rewriteInstruction(instruction, custom), context))
                    .build();
            var collector = new AiStreamCollector(listener(session), false, false);
            meter.track(collector::usage);
            var stopReason = streamWithContinuation(client, params, collector, session);
            collector.finish();
            if (BetaStopReason.REFUSAL.equals(stopReason)) {
                throw AnthropicGateway.refusal();
            }
            session.done(donePayload(stopReason, meter.total(), ctx.model(), List.of()));
        });
    }

    // ===================== 제목·요약·태그·주소 =====================

    @Override
    public AiMetaDto meta(UUID accountId, AiMetaRequestDto request) {
        requireEnabled();
        var content = cut(required(request.getContent(), "content", 200000), 40000);
        var title = request.getTitle() == null ? null : cut(request.getTitle(), 300);
        var ctx = prepare(accountId);
        requireQuota(ctx);
        var existingTags = tagRepository.findTagNamesByAccountId(accountId, PageRequest.of(0, 100));

        var schema = objectSchema(Map.of(
                "title", Map.of("type", "string", "description", "제목 (60자 안팎)"),
                "summary", Map.of("type", "string", "description", "요약 80~160자"),
                "tags", Map.of("type", "array", "items", Map.of("type", "string"), "description", "태그 3~6개"),
                "postUrl", Map.of("type", "string", "description", "영어 소문자·숫자·하이픈 slug")));
        var params = AnthropicGateway.baseParams(ctx.model(), 4000, AnthropicGateway.jsonOutput(BetaOutputConfig.Effort.LOW, schema))
                .system(AiPrompts.META_SYSTEM)
                .addUserMessage(AiPrompts.metaUser(title, content, existingTags))
                .build();
        var json = structuredCall(ctx, AiUsageKind.META, params);

        return AiMetaDto.builder()
                .title(cut(json.path("title").asText("").strip(), 100))
                .summary(cut(json.path("summary").asText("").strip(), 255))
                .tags(normalizeTags(json.path("tags"), existingTags))
                .postUrl(slug(json.path("postUrl").asText("")))
                .build();
    }

    // ===================== 영어 번역 (SSE) =====================

    @Override
    public ResponseEntity<ResponseBodyEmitter> translate(UUID accountId, AiTranslateRequestDto request) {
        requireEnabled();
        var content = required(request.getContent(), "content", 100000);
        var title = request.getTitle() == null ? "" : cut(request.getTitle().strip(), 300);
        var summary = request.getSummary() == null ? "" : cut(request.getSummary().strip(), 1000);
        var ctx = prepare(accountId);
        requireQuota(ctx);

        return startStream(ctx, AiUsageKind.TRANSLATE, (client, session, meter) -> {
            var params = AnthropicGateway.baseParams(ctx.model(), 32000, AnthropicGateway.effort(BetaOutputConfig.Effort.LOW))
                    .system(AiPrompts.TRANSLATE_SYSTEM)
                    .addUserMessage(AiPrompts.translateUser(title, summary, content))
                    .build();
            var collector = new AiStreamCollector(listener(session), false, true);
            meter.track(collector::usage);
            var stopReason = streamWithContinuation(client, params, collector, session);
            collector.finish();
            if (BetaStopReason.REFUSAL.equals(stopReason)) {
                throw AnthropicGateway.refusal();
            }

            var englishTitle = "";
            var englishSummary = "";
            if (!title.isBlank() || !summary.isBlank()) {
                try {
                    var schema = objectSchema(Map.of(
                            "title", Map.of("type", "string"),
                            "summary", Map.of("type", "string")));
                    var metaParams = AnthropicGateway.baseParams(ctx.model(), 2000, AnthropicGateway.jsonOutput(BetaOutputConfig.Effort.LOW, schema))
                            .system(AiPrompts.TRANSLATE_META_SYSTEM)
                            .addUserMessage(AiPrompts.translateMetaUser(title, summary, cut(collector.body(), 1500)))
                            .build();
                    var message = client.beta().messages().create(metaParams);
                    meter.add(AiTokenUsage.of(message.usage()));
                    AnthropicGateway.requireCompleted(message);
                    var json = objectMapper.readTree(AnthropicGateway.text(message));
                    englishTitle = json.path("title").asText("").strip();
                    englishSummary = json.path("summary").asText("").strip();
                } catch (Exception e) {
                    if (session.isCancelled()) {
                        throw e;
                    }
                    log.warn("translating title/summary failed: {}", e.getMessage());
                    session.status("제목·요약 번역은 실패했어요. 본문 번역은 끝났어요.");
                }
            }
            var payload = donePayload(stopReason, meter.total(), ctx.model(), List.of());
            payload.put("title", englishTitle);
            payload.put("summary", englishSummary);
            session.done(payload);
        });
    }

    // ===================== 커버 이미지 =====================

    @Override
    public AiCoverDto cover(UUID accountId, AiCoverRequestDto request) {
        requireEnabled();
        var title = cut(required(request.getTitle(), "title", 2000), 300);
        var summary = request.getSummary() == null ? "" : cut(request.getSummary(), 500);
        var tags = request.getTags() == null ? List.<String>of() : request.getTags().stream()
                .filter(Objects::nonNull).map(String::strip).filter(t -> !t.isEmpty()).limit(20).toList();
        var ctx = prepare(accountId);
        requireQuota(ctx);

        var schema = objectSchema(Map.of(
                "headline", Map.of("type", "string", "description", "28자 이내"),
                "subhead", Map.of("type", "string", "description", "40자 이내"),
                "badge", Map.of("type", "string", "description", "12자 이내(CVE 번호 등은 그대로), 없으면 빈 문자열"),
                "theme", Map.of("type", "string", "enum", CoverTheme.keys()),
                "icon", Map.of("type", "string", "enum", CoverIcon.keys())));
        var params = AnthropicGateway.baseParams(ctx.model(), 2000, AnthropicGateway.jsonOutput(BetaOutputConfig.Effort.LOW, schema))
                .system(AiPrompts.COVER_SYSTEM)
                .addUserMessage(AiPrompts.coverUser(title, summary, tags))
                .build();
        var json = structuredCall(ctx, AiUsageKind.COVER, params);
        var design = CoverDesign.of(
                json.path("headline").asText(title),
                json.path("subhead").asText(""),
                json.path("badge").asText(""),
                json.path("theme").asText(""),
                json.path("icon").asText(""));
        if (design.headline().isBlank()) {
            design = new CoverDesign(CoverDesign.of(title, "", "", "", "").headline(), design.subhead(), design.badge(), design.theme(), design.icon());
        }

        var png = coverRenderer.renderPng(design, coverFooterHost + "/@" + ctx.blogId());
        var file = attachmentService.saveBytes(accountId, png, "cover.png", "image/png");
        return AiCoverDto.builder()
                .fileId(file.getId().toString())
                .url(file.getUrl())
                .build();
    }

    // ===================== 공통 =====================

    /**
     * 호출에 필요한 것: 회원, 모델, 복호화한 키, 설정
     */
    record CallContext(UUID accountId, String authorName, String blogId, AiModel model, String apiKey, AiSetting setting) {
    }

    /**
     * 기능 켜짐·계정·키를 확인하고 키를 복호화한다.
     */
    private CallContext prepare(UUID accountId) {
        requireEnabled();
        var account = accountRepository.findByIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));
        var setting = aiSettingRepository.findByAccount_Id(accountId)
                .filter(AiSetting::hasApiKey)
                .orElseThrow(() -> new RestApiException(ErrorCodes.BadRequest.AI_NO_KEY));
        String apiKey;
        try {
            apiKey = cipher.decrypt(setting.getEncryptedApiKey());
        } catch (IllegalStateException e) {
            log.error("cannot decrypt stored Anthropic API key of account {}: {}", accountId, e.getMessage());
            throw new RestApiException(ErrorCodes.BadRequest.AI_NO_KEY, "저장된 API 키를 읽을 수 없어요. 설정에서 키를 다시 등록해 주세요.");
        }
        return new CallContext(accountId, account.getName(), account.getBlogId(),
                AiModel.fromIdOrDefault(setting.getModel()), apiKey, setting);
    }

    /**
     * 분당 요청 수와 이번 달 한도를 확인한다.
     */
    private void requireQuota(CallContext ctx) {
        requireRate(ctx.accountId());
        var since = YearMonth.now().atDay(1).atStartOfDay();
        var spent = Optional.ofNullable(aiUsageRepository.sumCostSince(ctx.accountId(), since)).orElse(BigDecimal.ZERO);
        var limit = monthlyLimit(ctx.setting());
        if (spent.compareTo(limit) >= 0) {
            throw new RestApiException(ErrorCodes.TooManyRequests.AI_LIMIT_EXCEEDED,
                    "이번 달 AI 사용 한도($" + limit.stripTrailingZeros().toPlainString() + ")에 닿았어요. 설정에서 한도를 늘릴 수 있어요.");
        }
    }

    private void requireRate(UUID accountId) {
        if (!rateLimiter.allow("ai", accountId.toString(), REQUESTS_PER_MINUTE)) {
            throw new RestApiException(ErrorCodes.TooManyRequests.AI_RATE_LIMITED);
        }
    }

    private void requireEnabled() {
        if (!cipher.isEnabled()) {
            throw new RestApiException(ErrorCodes.ServiceUnavailable.AI_DISABLED);
        }
    }

    private static BigDecimal monthlyLimit(AiSetting setting) {
        return setting == null || setting.getMonthlyLimitUsd() == null ? AiStore.DEFAULT_MONTHLY_LIMIT_USD : setting.getMonthlyLimitUsd();
    }

    /**
     * 구조화 출력 요청을 보내고 JSON 을 돌려준다. 사용량은 결과와 상관없이 남긴다.
     */
    private JsonNode structuredCall(CallContext ctx, AiUsageKind kind, MessageCreateParams params) {
        return gateway.withClient(ctx.apiKey(), client -> {
            BetaMessage message;
            try {
                message = client.beta().messages().create(params);
            } catch (RuntimeException e) {
                throw AnthropicGateway.upstream(e);
            }
            recordUsage(ctx, kind, AiTokenUsage.of(message.usage()));
            AnthropicGateway.requireCompleted(message);
            try {
                return objectMapper.readTree(AnthropicGateway.text(message));
            } catch (Exception e) {
                log.warn("AI returned invalid JSON for {}: {}", kind, e.getMessage());
                throw new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 응답을 읽지 못했어요. 다시 해 주세요.");
            }
        });
    }

    private void recordUsage(CallContext ctx, AiUsageKind kind, AiTokenUsage usage) {
        try {
            store.recordUsage(ctx.accountId(), kind, ctx.model(), usage);
        } catch (Exception e) {
            log.error("failed to record AI usage ({} {}): {}", kind, usage, e.getMessage());
        }
    }

    // ----- 스트리밍 -----

    /**
     * 스트리밍 작업. 사용량은 meter 에 모은다(실패·취소돼도 그때까지의 사용량을 남긴다).
     */
    @FunctionalInterface
    interface StreamJob {
        void run(AnthropicClient client, AiSseSession session, UsageMeter meter) throws Exception;
    }

    /**
     * 여러 호출의 사용량 합계. 진행 중인 스트림은 공급자로 등록해 마지막에 읽는다.
     */
    static final class UsageMeter {
        private AiTokenUsage fixed = AiTokenUsage.ZERO;
        private final List<Supplier<AiTokenUsage>> live = new ArrayList<>();

        synchronized void add(AiTokenUsage usage) {
            fixed = fixed.plus(usage);
        }

        synchronized void track(Supplier<AiTokenUsage> usage) {
            live.add(usage);
        }

        synchronized AiTokenUsage total() {
            var total = fixed;
            for (var usage : live) {
                total = total.plus(usage.get());
            }
            return total;
        }
    }

    private ResponseEntity<ResponseBodyEmitter> startStream(CallContext ctx, AiUsageKind kind, StreamJob job) {
        var session = AiSseSession.forEmitter(objectMapper, STREAM_TIMEOUT_MILLIS);
        try {
            streamExecutor.execute(() -> runStream(ctx, kind, session, job));
        } catch (RejectedExecutionException e) {
            throw new RestApiException(ErrorCodes.TooManyRequests.AI_RATE_LIMITED, "지금 AI 요청이 많아요. 잠시 후 다시 해 주세요.");
        }
        session.startPing(pingScheduler);
        return session.toResponseEntity();
    }

    private void runStream(CallContext ctx, AiUsageKind kind, AiSseSession session, StreamJob job) {
        var meter = new UsageMeter();
        AnthropicClient client = null;
        try {
            client = gateway.createClient(ctx.apiKey());
            job.run(client, session, meter);
        } catch (Throwable e) {
            if (session.isCancelled()) {
                log.info("AI {} stream cancelled by client (account {})", kind.getKey(), ctx.accountId());
            } else {
                var error = AnthropicGateway.upstream(e);
                session.error(error.getErrorCode().getName().toUpperCase(Locale.ROOT), error.getMessage());
            }
        } finally {
            if (client != null) {
                client.close();
            }
            session.complete();
            var usage = meter.total();
            if (!usage.equals(AiTokenUsage.ZERO)) {
                recordUsage(ctx, kind, usage);
            }
        }
    }

    /**
     * 스트림을 읽고, 서버 도구 루프가 멈추면(pause_turn) assistant 내용을 붙여 이어 받는다(최대 {@value #MAX_CONTINUATIONS}번).
     *
     * @return 마지막 stop_reason
     */
    private static BetaStopReason streamWithContinuation(AnthropicClient client, MessageCreateParams initial,
                                                         AiStreamCollector collector, AiSseSession session) {
        return streamWithContinuation(params -> client.beta().messages().createStreaming(params), initial, collector, session);
    }

    /**
     * @param open 요청을 보내 스트림을 여는 함수 (테스트에서는 미리 만든 이벤트)
     */
    static BetaStopReason streamWithContinuation(Function<MessageCreateParams, StreamResponse<BetaRawMessageStreamEvent>> open,
                                                 MessageCreateParams initial, AiStreamCollector collector, AiSseSession session) {
        var params = initial;
        var assistantContent = new ArrayList<BetaContentBlockParam>();
        for (var continuation = 0; ; continuation++) {
            collector.beginMessage();
            try (var stream = open.apply(params)) {
                session.attachUpstream(stream);
                for (var event : stream) {
                    if (session.isCancelled()) {
                        break;
                    }
                    collector.accept(event);
                }
            } finally {
                session.detachUpstream();
            }
            if (session.isCancelled()) {
                throw new CancellationException("client disconnected");
            }
            var message = collector.endMessage();
            var stopReason = message.stopReason().orElse(null);
            if (BetaStopReason.PAUSE_TURN.equals(stopReason) && continuation < MAX_CONTINUATIONS) {
                message.content().forEach(block -> assistantContent.add(block.toParam()));
                params = initial.toBuilder()
                        .addAssistantMessageOfContentBlockParams(List.copyOf(assistantContent))
                        .build();
                continue;
            }
            return stopReason;
        }
    }

    private static AiStreamCollector.Listener listener(AiSseSession session) {
        return new AiStreamCollector.Listener() {
            @Override
            public void status(String message) {
                session.status(message);
            }

            @Override
            public void text(String text) {
                session.delta(text);
            }
        };
    }

    static Map<String, Object> donePayload(BetaStopReason stopReason, AiTokenUsage usage, AiModel model, List<AiReferences.Source> sources) {
        var usageMap = new LinkedHashMap<String, Object>();
        usageMap.put("inputTokens", usage.totalInputTokens());
        usageMap.put("outputTokens", usage.outputTokens());
        usageMap.put("costUsd", usage.costUsd(model).doubleValue());
        var payload = new LinkedHashMap<String, Object>();
        payload.put("stopReason", stopReason == null ? "end_turn" : stopReason.asString());
        payload.put("usage", usageMap);
        payload.put("sources", sources.stream().map(s -> {
            var source = new LinkedHashMap<String, Object>();
            source.put("title", s.title());
            source.put("url", s.url());
            return source;
        }).toList());
        return payload;
    }

    // ----- 예전 글·문체 -----

    /**
     * 문체 분석에 쓸 글: 임시저장 제외, 최근 순, 본문 300자 이상, 최대 10편, 글마다 4000자까지
     */
    private List<AiPrompts.PostExcerpt> eligiblePosts(UUID accountId) {
        return postRepository.findRecentNonDraftByAccountId(accountId, PageRequest.of(0, 40)).stream()
                .filter(p -> p.getContent() != null && p.getContent().strip().length() >= STYLE_MIN_CONTENT_CHARS)
                .limit(STYLE_MAX_POSTS)
                .map(AiServiceImpl::toExcerpt)
                .toList();
    }

    private static AiPrompts.PostExcerpt toExcerpt(Post post) {
        return new AiPrompts.PostExcerpt(post.getTitle(), cut(post.getContent().strip(), STYLE_POST_MAX_CHARS));
    }

    /**
     * 문체 프로필이 없거나, 30일이 지났거나, 그 뒤로 새 글이 3편 이상이면 다시 분석한다.
     */
    private boolean needsStyleRefresh(AiSetting setting, UUID accountId) {
        if (!setting.hasStyle() || setting.getStyleAnalyzedAt() == null) {
            return true;
        }
        if (setting.getStyleAnalyzedAt().isBefore(LocalDateTime.now().minusDays(STYLE_REFRESH_DAYS))) {
            return true;
        }
        return postRepository.countNonDraftByAccountIdSince(accountId, setting.getStyleAnalyzedAt()) >= STYLE_REFRESH_NEW_POSTS;
    }

    private static AiStyleDto styleDto(AiSetting setting) {
        if (setting == null || !setting.hasStyle()) {
            return AiStyleDto.builder().analyzed(false).analyzedAt(null).postCount(0).profile(null).build();
        }
        return AiStyleDto.builder()
                .analyzed(true)
                .analyzedAt(setting.getStyleAnalyzedAt() == null ? null : setting.getStyleAnalyzedAt().format(ANALYZED_AT_FORMAT))
                .postCount(setting.getStylePostCount() == null ? 0 : setting.getStylePostCount())
                .profile(setting.getStyleProfile())
                .build();
    }

    // ----- 입력·출력 다듬기 -----

    private static String required(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, field + " 를 입력해 주세요.");
        }
        if (value.length() > maxLength) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, field + " 는 " + maxLength + "자까지 쓸 수 있어요.");
        }
        return value;
    }

    private static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return required(value, field, maxLength);
    }

    static String cut(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value;
        }
        var end = maxChars;
        if (Character.isHighSurrogate(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static Map<String, Object> objectSchema(Map<String, Object> properties) {
        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", new ArrayList<>(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    /**
     * 태그: 빈 값·중복 제거, 이 블로그에서 이미 쓰는 표기(대소문자·띄어쓰기 무시하고 같은 것)로 바꾸기, 최대 10개
     */
    static List<String> normalizeTags(JsonNode tags, List<String> existingTags) {
        var existing = new HashMap<String, String>();
        existingTags.forEach(tag -> existing.putIfAbsent(tagKey(tag), tag));
        var result = new LinkedHashMap<String, String>();
        if (tags != null && tags.isArray()) {
            for (var node : tags) {
                var tag = node.asText("").strip().replaceFirst("^#", "").strip();
                if (tag.isEmpty() || tag.length() > 100) {
                    continue;
                }
                var key = tagKey(tag);
                result.putIfAbsent(key, existing.getOrDefault(key, tag));
            }
        }
        return result.values().stream().limit(10).toList();
    }

    private static String tagKey(String tag) {
        return tag.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * 게시글 주소: 영어 소문자·숫자·하이픈만, 최대 80자
     */
    static String slug(String value) {
        var slug = value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > 80) {
            slug = slug.substring(0, 80).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "post-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) : slug;
    }

    private static String footerHost(String webBaseUrl) {
        try {
            var uri = URI.create(webBaseUrl);
            if (uri.getHost() != null) {
                return uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            }
        } catch (IllegalArgumentException ignored) {
            // 아래 기본값
        }
        return "hwanstory.kr";
    }

    private static ThreadFactory namedThreads(String prefix) {
        var counter = new AtomicInteger();
        return runnable -> {
            var thread = new Thread(runnable, prefix + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
