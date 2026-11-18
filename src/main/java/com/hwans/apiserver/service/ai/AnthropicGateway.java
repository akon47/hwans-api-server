package com.hwans.apiserver.service.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.beta.messages.BetaCacheControlEphemeral;
import com.anthropic.models.beta.messages.BetaContentBlock;
import com.anthropic.models.beta.messages.BetaJsonOutputFormat;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.BetaTextBlock;
import com.anthropic.models.beta.messages.BetaTextBlockParam;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Anthropic Java SDK 를 감싼다. 회원마다 자기 API 키를 쓰므로 요청마다 클라이언트를 만들고 다 쓰면 닫는다.
 * <p>
 * 메시지 호출은 모두 beta 경로({@code client.beta().messages()})를 쓴다 — Opus/Sonnet 5.5 의 안전 분류기가
 * (CVE 해설 같은) 정상 요청을 거절했을 때 서버 쪽에서 대체 모델로 다시 시도하는 {@code fallbacks: "default"}
 * (beta {@code server-side-fallback-2026-07-01})가 beta 요청에만 있기 때문이다.
 */
@Component
@Slf4j
public class AnthropicGateway {
    public static final String FALLBACK_BETA = "server-side-fallback-2026-07-01";
    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    /**
     * 비어 있으면 SDK 기본 주소(https://api.anthropic.com). 로컬에서 가짜 서버로 시험할 때만 쓴다.
     */
    private final String baseUrl;

    public AnthropicGateway() {
        this("");
    }

    @Autowired
    public AnthropicGateway(@Value("${ai.anthropic-base-url:}") String baseUrl) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.strip();
        if (!this.baseUrl.isEmpty()) {
            log.warn("ai.anthropic-base-url is set: Anthropic requests go to {}", this.baseUrl);
        }
    }

    /**
     * API 키로 클라이언트를 만든다. 다 쓰면 {@link AnthropicClient#close()} 해야 한다.
     */
    public AnthropicClient createClient(String apiKey) {
        var builder = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(TIMEOUT)
                .maxRetries(2);
        if (!baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }
        return builder.build();
    }

    /**
     * Models API(목록 조회, 무료)로 키가 유효한지 확인한다. 401 이면 {@code AI_INVALID_KEY}.
     */
    public void validateKey(String apiKey) {
        var client = createClient(apiKey);
        try {
            client.models().list();
        } catch (UnauthorizedException e) {
            throw new RestApiException(ErrorCodes.BadRequest.AI_INVALID_KEY);
        } catch (RuntimeException e) {
            throw upstream(e);
        } finally {
            client.close();
        }
    }

    /**
     * 모든 메시지 요청의 공통 부분: 모델, 최대 토큰, effort, (Opus/Sonnet) 거절 시 서버 쪽 대체 모델.
     * Opus 5.5 는 생각(thinking)이 항상 켜져 있으므로 thinking 은 보내지 않고 effort 로만 조절한다.
     */
    public static MessageCreateParams.Builder baseParams(AiModel model, long maxTokens, BetaOutputConfig outputConfig) {
        var builder = MessageCreateParams.builder()
                .model(model.getId())
                .maxTokens(maxTokens)
                .outputConfig(outputConfig);
        if (model.isServerSideFallback()) {
            builder.addBeta(FALLBACK_BETA).fallbacksDefault();
        }
        return builder;
    }

    public static BetaOutputConfig effort(BetaOutputConfig.Effort effort) {
        return BetaOutputConfig.builder().effort(effort).build();
    }

    /**
     * 구조화 출력(JSON 스키마) + effort. 강제 tool_choice 는 Opus/Sonnet 5.5 에서 400 이라 이 방식으로 JSON 을 받는다.
     */
    public static BetaOutputConfig jsonOutput(BetaOutputConfig.Effort effort, Map<String, Object> schema) {
        var schemaBuilder = BetaJsonOutputFormat.Schema.builder();
        schema.forEach((key, value) -> schemaBuilder.putAdditionalProperty(key, JsonValue.from(value)));
        return BetaOutputConfig.builder()
                .effort(effort)
                .format(BetaJsonOutputFormat.builder().schema(schemaBuilder.build()).build())
                .build();
    }

    /**
     * 프롬프트 캐시를 거는 시스템 블록 (기본 5분)
     */
    public static List<BetaTextBlockParam> cachedSystem(String text) {
        return List.of(BetaTextBlockParam.builder()
                .text(text)
                .cacheControl(BetaCacheControlEphemeral.builder().build())
                .build());
    }

    /**
     * 응답의 text 블록을 이어 붙인다.
     */
    public static String text(BetaMessage message) {
        return message.content().stream()
                .map(BetaContentBlock::text)
                .flatMap(Optional::stream)
                .map(BetaTextBlock::text)
                .collect(Collectors.joining());
    }

    /**
     * 끝난 이유가 정상(end_turn 등)이 아니면 사용자에게 보여 줄 오류로 바꾼다.
     */
    public static void requireCompleted(BetaMessage message) {
        var reason = message.stopReason().orElse(null);
        if (BetaStopReason.REFUSAL.equals(reason)) {
            throw refusal();
        }
        if (BetaStopReason.MAX_TOKENS.equals(reason)) {
            throw new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 응답이 너무 길어 중간에 끊겼어요. 내용을 줄여 다시 해 주세요.");
        }
    }

    public static RestApiException refusal() {
        return new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM,
                "AI 가 안전 정책 때문에 이 요청에 답하지 않았어요. 요청 내용을 바꿔 다시 해 주세요.");
    }

    /**
     * SDK 예외를 {@code AI_UPSTREAM}(502) 오류로 바꾼다. 메시지는 사용자에게 그대로 보인다(키 같은 비밀은 들어가지 않는다).
     */
    public static RestApiException upstream(Throwable e) {
        if (e instanceof RestApiException restApiException) {
            return restApiException;
        }
        String reason;
        if (e instanceof AnthropicServiceException service) {
            var status = service.statusCode();
            reason = switch (status) {
                case 400 -> "요청이 올바르지 않대요(400)" + detail(service);
                case 401 -> "API 키가 거절됐어요(401). 설정에서 키를 다시 등록해 주세요";
                case 402 -> "Anthropic 계정의 결제·크레딧을 확인해 주세요(402)";
                case 403 -> "이 키로는 쓸 수 없는 기능이래요(403)" + detail(service);
                case 404 -> "모델이나 기능을 찾을 수 없대요(404)" + detail(service);
                case 413 -> "보낸 글이 너무 길어요(413)";
                case 429 -> "Anthropic 사용량 제한에 걸렸어요(429). 잠시 후 다시 해 주세요";
                case 500, 502, 503, 504, 529 -> "Anthropic 서버가 지금 바빠요(" + status + "). 잠시 후 다시 해 주세요";
                default -> "Anthropic 이 오류를 돌려줬어요(" + status + ")" + detail(service);
            };
            log.warn("Anthropic API error {}: {}", status, e.getMessage());
        } else if (e instanceof AnthropicIoException) {
            reason = "Anthropic 에 연결하지 못했어요";
            log.warn("Anthropic connection error: {}", e.getMessage());
        } else {
            reason = "알 수 없는 오류가 났어요";
            log.warn("AI call failed", e);
        }
        return new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 호출에 실패했어요: " + reason + ".");
    }

    private static String detail(AnthropicServiceException e) {
        var message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "";
        }
        message = message.replaceAll("\\s+", " ").strip();
        return " — " + (message.length() > 200 ? message.substring(0, 200) + "…" : message);
    }

    /**
     * 클라이언트를 만들어 일을 시키고 닫는다.
     */
    public <T> T withClient(String apiKey, Function<AnthropicClient, T> work) {
        var client = createClient(apiKey);
        try {
            return work.apply(client);
        } finally {
            client.close();
        }
    }
}
