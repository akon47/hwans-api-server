package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.TagDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.attachment.Attachment;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.service.blog.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 커버를 글에 반영하는 부분을 실제 글 수정 로직(BlogService.modifyPost)·H2 로 확인한다.
 * (ApiServerApplicationTests 와 같은 설정이라 스프링 컨텍스트를 같이 쓴다)
 */
@SpringBootTest(properties = "spring.mail.host=localhost")
class AiPostCoversTests {
    private static final String BLOG_ID = "@kim-hwan";          // data.sql 의 회원 0
    private static final String OTHER_BLOG_ID = "@kim-hwan-2";  // data.sql 의 회원 1
    private static final String API_PREFIX = "https://api.hwanstory.kr/api/v1";

    @Autowired
    private AiPostCovers postCovers;
    @Autowired
    private BlogService blogService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Account account;
    private String postUrl;
    private final LocalDateTime scheduledAt = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS);

    /**
     * 글의 모든 필드 (비교용)
     */
    private record Snapshot(String postUrl, String title, String summary, String content, OpenType openType,
                            LocalDateTime scheduledAt, String seriesUrl, Set<String> tags, UUID thumbnailId) {
    }

    private Snapshot snapshot() {
        return transactionTemplate.execute(status -> {
            var post = postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(BLOG_ID, postUrl).orElseThrow();
            return new Snapshot(post.getPostUrl(), post.getTitle(), post.getSummary(), post.getContent(), post.getOpenType(),
                    post.getScheduledAt(), post.getSeriesUrl(), post.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                    post.getThumbnailImage() == null ? null : post.getThumbnailImage().getId());
        });
    }

    private Attachment newCoverAttachment() {
        // 실제 파일은 쓰지 않는다 (없는 파일이면 크기 0) — 반영 로직만 본다
        return attachmentRepository.save(new Attachment(account, new File("ai-cover-test-" + UUID.randomUUID() + ".png"), "cover.png", "image/png"));
    }

    @BeforeEach
    void createPost() {
        account = accountRepository.findByBlogId(BLOG_ID).orElseThrow();
        postUrl = "ai-cover-test-" + UUID.randomUUID().toString().substring(0, 8);
        blogService.createPost(account.getId(), PostRequestDto.builder()
                .postUrl(postUrl)
                .title("[WPF] 타이머 사용하기")
                .content("첫 단락이에요.\n\n## 둘째\n\n내용")
                .openType(OpenType.SCHEDULED)
                .summary("요약이에요")
                .tags(Set.of(new TagDto("WPF"), new TagDto("C#")))
                .seriesUrl("series_url")
                .scheduledAt(scheduledAt)
                .build());
    }

    @Test
    void insertIntoContentPrependsOnceAndKeepsEveryOtherField() {
        var before = snapshot();
        assertNull(before.thumbnailId());
        assertEquals("series_url", before.seriesUrl());
        var cover = newCoverAttachment();
        var imageUrl = API_PREFIX + cover.getUrl();

        postCovers.apply(BLOG_ID, postUrl, cover.getId(), imageUrl, true, true);
        var after = snapshot();

        assertEquals("![\\[WPF\\] 타이머 사용하기](" + imageUrl + ")\n\n" + before.content(), after.content());
        assertEquals(1, after.content().split("!\\[", -1).length - 1, "image must be prepended exactly once");
        assertTrue(imageUrl.matches("https://api\\.hwanstory\\.kr/api/v1/attachments/[0-9a-f-]{36}/image\\.png"), imageUrl);
        assertEquals(cover.getId(), after.thumbnailId());
        // 다른 필드는 그대로
        assertEquals(before.postUrl(), after.postUrl());
        assertEquals(before.title(), after.title());
        assertEquals(before.summary(), after.summary());
        assertEquals(OpenType.SCHEDULED, after.openType());
        assertEquals(scheduledAt, after.scheduledAt());
        assertEquals(before.seriesUrl(), after.seriesUrl());
        assertEquals(Set.of("WPF", "C#"), after.tags());

        // 이제 썸네일이 있으므로 일괄 작업은 건너뛴다
        assertTrue(postCovers.find(BLOG_ID, postUrl).orElseThrow().hasThumbnail());
    }

    @Test
    void thumbnailOnlyLeavesTheContentAlone() {
        var before = snapshot();
        var cover = newCoverAttachment();

        postCovers.apply(BLOG_ID, postUrl, cover.getId(), API_PREFIX + cover.getUrl(), true, false);
        var after = snapshot();

        assertEquals(before.content(), after.content());
        assertEquals(cover.getId(), after.thumbnailId());
        assertEquals(new Snapshot(before.postUrl(), before.title(), before.summary(), before.content(), before.openType(),
                before.scheduledAt(), before.seriesUrl(), before.tags(), cover.getId()), after);
    }

    @Test
    void contentOnlyKeepsTheThumbnailAsIs() {
        var cover = newCoverAttachment();
        postCovers.apply(BLOG_ID, postUrl, cover.getId(), API_PREFIX + cover.getUrl(), false, true);
        var after = snapshot();
        assertNull(after.thumbnailId());
        assertTrue(after.content().startsWith("![\\[WPF\\] 타이머 사용하기](" + API_PREFIX + cover.getUrl() + ")\n\n첫 단락이에요."));
    }

    @Test
    void onlyOwnPostsAreVisible() {
        var target = postCovers.find(BLOG_ID, postUrl).orElseThrow();
        assertEquals("[WPF] 타이머 사용하기", target.title());
        assertEquals("요약이에요", target.summary());
        assertEquals(Set.of("WPF", "C#"), Set.copyOf(target.tags()));
        assertFalse(target.hasThumbnail());

        // 다른 회원의 블로그 Id 로는 보이지도, 고쳐지지도 않는다
        assertTrue(postCovers.find(OTHER_BLOG_ID, postUrl).isEmpty());
        var cover = newCoverAttachment();
        assertThrows(RestApiException.class, () ->
                postCovers.apply(OTHER_BLOG_ID, postUrl, cover.getId(), API_PREFIX + cover.getUrl(), true, true));
        assertNull(snapshot().thumbnailId());
    }

    @Test
    void missingListsOwnPostsWithoutThumbnailNewestFirst() {
        var missing = postCovers.missing(account.getId());
        assertTrue(missing.getTotal() >= missing.getPosts().size());
        var first = missing.getPosts().get(0);
        assertEquals(postUrl, first.getPostUrl(), "the post just created is the newest");
        assertEquals("SCHEDULED", first.getOpenType());
        assertTrue(first.getCreatedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"), first.getCreatedAt());

        var cover = newCoverAttachment();
        postCovers.apply(BLOG_ID, postUrl, cover.getId(), API_PREFIX + cover.getUrl(), true, false);
        var after = postCovers.missing(account.getId());
        assertEquals(missing.getTotal() - 1, after.getTotal());
        assertTrue(after.getPosts().stream().noneMatch(p -> p.getPostUrl().equals(postUrl)));
        // 다른 회원의 글은 없다
        var other = accountRepository.findByBlogId(OTHER_BLOG_ID).orElseThrow();
        assertTrue(postCovers.missing(other.getId()).getPosts().stream().noneMatch(p -> p.getPostUrl().equals(postUrl)));
    }
}
