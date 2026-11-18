package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiMetaDto;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 제목·요약·태그 일괄 제안(계약 13번)의 글별 흐름. <b>글을 바꾸지 않는다</b> — 읽기({@link Posts#find})만 받는다.
 * 글마다: 내 글이 아니면 실패 → 본문이 비었으면 실패 → 이번 달 한도 확인(넘으면 예외로 끝) → 제안(7번과 같은 방식) → item.
 * 한 글의 실패는 실패 item 으로 남기고 다음 글로 간다. 클라이언트가 끊으면 지금 글까지만 하고 멈춘다.
 */
@Slf4j
public class AiMetaBatch {
    public static final List<String> ALL_FIELDS = List.of("title", "summary", "tags");

    /**
     * 그 회원의 글 읽기 (조회수·통계를 올리지 않는 내부 조회)
     */
    @FunctionalInterface
    public interface Posts {
        Optional<AiPostMeta.Target> find(String postUrl);
    }

    /**
     * 제안을 만든다. 사용량은 (뒤에서 실패하더라도) usage 로 알린다.
     */
    @FunctionalInterface
    public interface Suggester {
        AiMetaDto suggest(AiPostMeta.Target post, Consumer<AiTokenUsage> usage);
    }

    public record Result(int total, int suggested, int failed, AiTokenUsage usage) {
    }

    private final Posts posts;
    private final Suggester suggester;
    private final AiCoverBatch.Quota quota;
    private final AiModel model;

    public AiMetaBatch(Posts posts, Suggester suggester, AiCoverBatch.Quota quota, AiModel model) {
        this.posts = posts;
        this.suggester = suggester;
        this.quota = quota;
        this.model = model;
    }

    /**
     * @return 결과. 클라이언트가 끊어 도중에 멈췄으면 null
     * @throws RestApiException 한도 초과 등 일괄 작업 전체를 끝내야 하는 오류 (호출하는 쪽이 {@code error} 이벤트로 보낸다)
     */
    public Result run(List<String> postUrls, Set<String> fields, AiSseSession session) {
        var targets = List.copyOf(new LinkedHashSet<>(postUrls));
        var total = targets.size();
        var suggested = 0;
        var failed = 0;
        var usage = new AiTokenUsage[]{AiTokenUsage.ZERO};

        for (var i = 0; i < total; i++) {
            if (session.isCancelled()) {
                log.info("meta batch stopped by client after {}/{} posts", i, total);
                return null;
            }
            var postUrl = targets.get(i);
            var found = posts.find(postUrl);
            if (found.isEmpty()) {
                session.item(failure(postUrl, "글을 찾을 수 없어요."));
                failed++;
                continue;
            }
            var post = found.get();
            if (post.content() == null || post.content().isBlank()) {
                session.item(failure(postUrl, "본문이 비어 있어 제안할 수 없어요."));
                failed++;
                continue;
            }

            quota.check();
            session.status((i + 1) + "/" + total + " '" + post.title() + "' 제안 만드는 중");
            if (session.isCancelled()) {
                log.info("meta batch stopped by client after {}/{} posts", i, total);
                return null;
            }
            try {
                var suggestion = suggester.suggest(post, u -> usage[0] = usage[0].plus(u));
                session.item(success(post, suggestion, fields));
                suggested++;
            } catch (RestApiException e) {
                log.warn("meta batch: post {} failed: {}", postUrl, e.getMessage());
                session.item(failure(postUrl, e.getMessage()));
                failed++;
            } catch (RuntimeException e) {
                log.warn("meta batch: post {} failed", postUrl, e);
                session.item(failure(postUrl, "제안을 만들지 못했어요."));
                failed++;
            }
        }

        var result = new Result(total, suggested, failed, usage[0]);
        if (!session.isCancelled()) {
            var payload = new LinkedHashMap<String, Object>();
            payload.put("total", result.total());
            payload.put("suggested", result.suggested());
            payload.put("failed", result.failed());
            payload.put("usage", AiBatchSupport.usageMap(result.usage(), model));
            session.done(payload);
        }
        return result;
    }

    /**
     * 요청한 항목만 제안으로 채우고 나머지는 지금 값과 같게. 빈 제안은 지금 값을 그대로 둔다.
     */
    static Map<String, Object> success(AiPostMeta.Target post, AiMetaDto suggestion, Set<String> fields) {
        var currentTitle = post.title() == null ? "" : post.title();
        var currentSummary = post.summary() == null ? "" : post.summary();
        var currentTags = post.tags() == null ? List.<String>of() : post.tags();

        var current = new LinkedHashMap<String, Object>();
        current.put("title", currentTitle);
        current.put("summary", currentSummary);
        current.put("tags", currentTags);

        var suggestedMap = new LinkedHashMap<String, Object>();
        suggestedMap.put("title", fields.contains("title") && notBlank(suggestion.getTitle()) ? suggestion.getTitle() : currentTitle);
        suggestedMap.put("summary", fields.contains("summary") && notBlank(suggestion.getSummary()) ? suggestion.getSummary() : currentSummary);
        suggestedMap.put("tags", fields.contains("tags") && suggestion.getTags() != null && !suggestion.getTags().isEmpty()
                ? suggestion.getTags() : currentTags);

        var item = new LinkedHashMap<String, Object>();
        item.put("postUrl", post.postUrl());
        item.put("ok", true);
        item.put("current", current);
        item.put("suggested", suggestedMap);
        return item;
    }

    private static Map<String, Object> failure(String postUrl, String message) {
        var item = new LinkedHashMap<String, Object>();
        item.put("postUrl", postUrl);
        item.put("ok", false);
        item.put("message", message == null || message.isBlank() ? "제안을 만들지 못했어요." : message);
        return item;
    }

    /**
     * fields ⊆ {title, summary, tags}. 없으면 셋 다.
     */
    static Set<String> validateFields(List<String> fields) {
        if (fields == null) {
            return Set.copyOf(ALL_FIELDS);
        }
        var result = new LinkedHashSet<String>();
        for (var field : fields) {
            var key = field == null ? "" : field.strip();
            if (!ALL_FIELDS.contains(key)) {
                throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "fields 는 title, summary, tags 중에서 골라야 해요.");
            }
            result.add(key);
        }
        if (result.isEmpty()) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "fields 는 title, summary, tags 중에서 하나 이상 골라야 해요.");
        }
        return result;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
