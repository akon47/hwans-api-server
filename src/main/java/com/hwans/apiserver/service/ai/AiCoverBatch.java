package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.attachment.FileDto;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 커버 일괄 만들기(계약 11번)의 글별 흐름. 글 읽기·고치기, 커버 만들기, 한도 확인은 함수로 받아 단위 테스트할 수 있다.
 * <ol>
 *   <li>글마다 차례로: 클라이언트가 끊었으면 멈춘다(지금 하던 글은 끝낸 뒤).</li>
 *   <li>내 글이 아니거나 없으면 실패 item, 썸네일로 쓸 건데 이미 썸네일이 있으면 건너뜀 item.</li>
 *   <li>이번 달 한도 확인 — 넘으면 예외로 일괄 작업을 끝낸다(이미 만든 글은 반영된 채로).</li>
 *   <li>커버 만들기(사용량은 글마다 cover 로 기록) → 썸네일 지정·본문 앞에 넣기 → 성공 item.
 *       한 글의 실패(Anthropic 오류 등)는 실패 item 으로 남기고 다음 글로 간다.</li>
 *   <li>끝나면 {@code done} (개수와 합친 사용량).</li>
 * </ol>
 */
@Slf4j
public class AiCoverBatch {
    public static final int MAX_POSTS = 100;

    /**
     * 그 회원의 글만 다룬다.
     */
    public interface Posts {
        Optional<AiPostCovers.Target> find(String postUrl);

        void apply(String postUrl, UUID fileId, String imageUrl, boolean useAsThumbnail, boolean insertIntoContent);
    }

    /**
     * 커버를 만들어 그 회원 첨부로 저장한다. 사용량은 (뒤에서 실패하더라도) usage 로 알린다.
     */
    @FunctionalInterface
    public interface Covers {
        FileDto create(AiPostCovers.Target post, Consumer<AiTokenUsage> usage);
    }

    /**
     * 이번 달 한도를 확인한다. 넘었으면 {@code AI_LIMIT_EXCEEDED} 예외.
     */
    @FunctionalInterface
    public interface Quota {
        void check();
    }

    public record Result(int total, int created, int skipped, int failed, AiTokenUsage usage) {
    }

    private final Posts posts;
    private final Covers covers;
    private final Quota quota;
    private final AiModel model;
    private final String imageUrlPrefix;

    /**
     * @param imageUrlPrefix 본문에 넣을 절대 URL 앞부분 ({@code <blog.api-base-url>/api/v1}) — 첨부 url 앞에 붙인다
     */
    public AiCoverBatch(Posts posts, Covers covers, Quota quota, AiModel model, String imageUrlPrefix) {
        this.posts = posts;
        this.covers = covers;
        this.quota = quota;
        this.model = model;
        this.imageUrlPrefix = imageUrlPrefix;
    }

    /**
     * @return 결과. 클라이언트가 끊어 도중에 멈췄으면 null ({@code done} 도 보내지 않는다)
     * @throws RestApiException 한도 초과 등 일괄 작업 전체를 끝내야 하는 오류 (호출하는 쪽이 {@code error} 이벤트로 보낸다)
     */
    public Result run(List<String> postUrls, boolean useAsThumbnail, boolean insertIntoContent, AiSseSession session) {
        var targets = List.copyOf(new LinkedHashSet<>(postUrls));
        var total = targets.size();
        var created = 0;
        var skipped = 0;
        var failed = 0;
        var usage = new AiTokenUsage[]{AiTokenUsage.ZERO};

        for (var i = 0; i < total; i++) {
            if (session.isCancelled()) {
                log.info("cover batch stopped by client after {}/{} posts", i, total);
                return null;
            }
            var postUrl = targets.get(i);
            var found = posts.find(postUrl);
            if (found.isEmpty()) {
                session.item(failure(postUrl, "", "글을 찾을 수 없어요."));
                failed++;
                continue;
            }
            var post = found.get();
            if (useAsThumbnail && post.hasThumbnail()) {
                session.item(skippedItem(post));
                skipped++;
                continue;
            }

            quota.check();
            session.status((i + 1) + "/" + total + " '" + post.title() + "' 커버 만드는 중");
            // 끊김은 쓰기가 실패할 때 알 수 있다 — 방금 상태를 못 보냈으면 새 글을 시작하지 않는다(비용 방지)
            if (session.isCancelled()) {
                log.info("cover batch stopped by client after {}/{} posts", i, total);
                return null;
            }
            try {
                var file = covers.create(post, u -> usage[0] = usage[0].plus(u));
                posts.apply(postUrl, file.getId(), imageUrlPrefix + file.getUrl(), useAsThumbnail, insertIntoContent);
                session.item(success(post, file));
                created++;
            } catch (RestApiException e) {
                log.warn("cover batch: post {} failed: {}", postUrl, e.getMessage());
                session.item(failure(postUrl, post.title(), e.getMessage()));
                failed++;
            } catch (RuntimeException e) {
                log.warn("cover batch: post {} failed", postUrl, e);
                session.item(failure(postUrl, post.title(), "커버를 만들지 못했어요."));
                failed++;
            }
        }

        var result = new Result(total, created, skipped, failed, usage[0]);
        if (!session.isCancelled()) {
            session.done(donePayload(result, model));
        }
        return result;
    }

    static Map<String, Object> donePayload(Result result, AiModel model) {
        var usageMap = new LinkedHashMap<String, Object>();
        usageMap.put("inputTokens", result.usage().totalInputTokens());
        usageMap.put("outputTokens", result.usage().outputTokens());
        usageMap.put("costUsd", result.usage().costUsd(model).doubleValue());
        var payload = new LinkedHashMap<String, Object>();
        payload.put("total", result.total());
        payload.put("created", result.created());
        payload.put("skipped", result.skipped());
        payload.put("failed", result.failed());
        payload.put("usage", usageMap);
        return payload;
    }

    private static Map<String, Object> success(AiPostCovers.Target post, FileDto file) {
        var item = new LinkedHashMap<String, Object>();
        item.put("postUrl", post.postUrl());
        item.put("title", post.title());
        item.put("ok", true);
        item.put("skipped", false);
        item.put("fileId", file.getId().toString());
        item.put("url", file.getUrl());
        return item;
    }

    private static Map<String, Object> skippedItem(AiPostCovers.Target post) {
        var item = new LinkedHashMap<String, Object>();
        item.put("postUrl", post.postUrl());
        item.put("title", post.title());
        item.put("ok", true);
        item.put("skipped", true);
        item.put("message", "이미 썸네일이 있어요");
        return item;
    }

    private static Map<String, Object> failure(String postUrl, String title, String message) {
        var item = new LinkedHashMap<String, Object>();
        item.put("postUrl", postUrl);
        item.put("title", title == null ? "" : title);
        item.put("ok", false);
        item.put("message", message == null || message.isBlank() ? "커버를 만들지 못했어요." : message);
        return item;
    }

    /**
     * 요청 검증: 1~100개, 빈 값 제외
     */
    static List<String> validate(List<String> postUrls) {
        if (postUrls == null || postUrls.size() > MAX_POSTS) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 는 1~" + MAX_POSTS + "개여야 해요.");
        }
        var cleaned = postUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 는 1~" + MAX_POSTS + "개여야 해요.");
        }
        if (cleaned.stream().anyMatch(url -> url.length() > 320)) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 에 너무 긴 주소가 있어요.");
        }
        return cleaned;
    }
}
