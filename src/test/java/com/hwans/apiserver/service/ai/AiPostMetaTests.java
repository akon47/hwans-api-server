package com.hwans.apiserver.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiMetaApplyRequestDto;
import com.hwans.apiserver.dto.ai.AiMetaDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.TagDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.attachment.Attachment;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.stats.PostDailyViewRepository;
import com.hwans.apiserver.service.blog.BlogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 제목·요약·태그 한꺼번에 채우기를 실제 글 수정 로직(BlogService.modifyPost)·H2 로 확인한다.
 * (ApiServerApplicationTests 와 같은 설정이라 스프링 컨텍스트를 같이 쓴다)
 */
@SpringBootTest(properties = "spring.mail.host=localhost")
class AiPostMetaTests {
    private static final String BLOG_ID = "@kim-hwan";          // data.sql 의 회원 0
    private static final String OTHER_BLOG_ID = "@kim-hwan-2";  // data.sql 의 회원 1
    private static final String LONG_SUMMARY = "이 글은 요약이 충분히 길어서 후보에 들지 않아야 하는 글이에요. 사십 자를 넘깁니다.";

    @Autowired
    private AiPostMeta postMeta;
    @Autowired
    private AiService aiService;
    @Autowired
    private BlogService blogService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private AttachmentRepository attachmentRepository;
    @Autowired
    private PostDailyViewRepository postDailyViewRepository;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Account account;
    private Account other;
    private String prefix;

    private record Snapshot(UUID id, String postUrl, String title, String summary, String content, OpenType openType,
                            LocalDateTime scheduledAt, String seriesUrl, Set<String> tags, UUID thumbnailId,
                            Integer hits, LocalDateTime updatedAt) {
    }

    private Snapshot snapshot(String postUrl) {
        return transactionTemplate.execute(status -> {
            var post = postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(BLOG_ID, postUrl).orElseThrow();
            return new Snapshot(post.getId(), post.getPostUrl(), post.getTitle(), post.getSummary(), post.getContent(), post.getOpenType(),
                    post.getScheduledAt(), post.getSeriesUrl(), post.getTags().stream().map(Tag::getName).collect(Collectors.toSet()),
                    post.getThumbnailImage() == null ? null : post.getThumbnailImage().getId(), post.getHits(), post.getUpdatedAt());
        });
    }

    private String createPost(Account owner, String name, String summary, Set<String> tags, OpenType openType, boolean withThumbnail) {
        var postUrl = prefix + name;
        UUID thumbnailId = null;
        if (withThumbnail) {
            thumbnailId = attachmentRepository.save(new Attachment(owner, new File("ai-meta-test-" + UUID.randomUUID() + ".png"), "t.png", "image/png")).getId();
        }
        blogService.createPost(owner.getId(), PostRequestDto.builder()
                .postUrl(postUrl)
                .title("제목 " + name)
                .content("본문 " + name + "\n\n## 단락\n\n내용")
                .openType(openType)
                .summary(summary)
                .tags(tags.stream().map(TagDto::new).collect(Collectors.toSet()))
                .seriesUrl(owner == account ? "series_url" : null)
                .scheduledAt(openType == OpenType.SCHEDULED ? LocalDateTime.now().plusDays(10).truncatedTo(ChronoUnit.SECONDS) : null)
                .thumbnailFileId(thumbnailId)
                .build());
        return postUrl;
    }

    @BeforeEach
    void setUp() {
        account = accountRepository.findByBlogId(BLOG_ID).orElseThrow();
        other = accountRepository.findByBlogId(OTHER_BLOG_ID).orElseThrow();
        prefix = "ai-meta-" + UUID.randomUUID().toString().substring(0, 6) + "-";
    }

    @Test
    void candidatesAreOwnPostsWithNoTagsOrShortSummaryNewestFirst() {
        var noTags = createPost(account, "no-tags", LONG_SUMMARY, Set.of(), OpenType.PUBLIC, false);
        var shortSummary = createPost(account, "short", "짧은 요약 서른 자 안쪽이에요", Set.of("Java"), OpenType.DRAFT, false);
        var complete = createPost(account, "complete", LONG_SUMMARY, Set.of("Java", "보안"), OpenType.PRIVATE, false);
        var both = createPost(account, "both", "짧아요", Set.of(), OpenType.SCHEDULED, false);
        var othersPost = createPost(other, "other", "짧아요", Set.of(), OpenType.PUBLIC, false);

        var result = postMeta.candidates(account.getId());
        var mine = result.getPosts().stream().filter(p -> p.getPostUrl().startsWith(prefix)).toList();

        // 최신 글부터: both → shortSummary → noTags (complete 는 빠지고, 남의 글은 없다)
        assertEquals(List.of(both, shortSummary, noTags), mine.stream().map(p -> p.getPostUrl()).toList());
        assertTrue(result.getPosts().stream().noneMatch(p -> p.getPostUrl().equals(complete) || p.getPostUrl().equals(othersPost)));
        assertEquals(result.getPosts().size(), result.getTotal(), "fewer than 500, so total equals the list size");

        var b = mine.get(0);
        assertTrue(b.isNoTags());
        assertTrue(b.isShortSummary());
        assertEquals("SCHEDULED", b.getOpenType());
        assertEquals("짧아요", b.getSummary());
        assertEquals(List.of(), b.getTags());
        assertTrue(b.getCreatedAt().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}"));
        var s = mine.get(1);
        assertFalse(s.isNoTags());
        assertTrue(s.isShortSummary());
        assertEquals(List.of("Java"), s.getTags());
        assertEquals("DRAFT", s.getOpenType());
        var n = mine.get(2);
        assertTrue(n.isNoTags());
        assertFalse(n.isShortSummary());
        assertEquals(LONG_SUMMARY, n.getSummary());

        // 다른 회원의 후보에는 그 회원 글만
        assertTrue(postMeta.candidates(other.getId()).getPosts().stream().anyMatch(p -> p.getPostUrl().equals(othersPost)));
        assertTrue(postMeta.candidates(other.getId()).getPosts().stream().noneMatch(p -> p.getPostUrl().startsWith(prefix + "no-tags")));
    }

    @Test
    void suggestionBatchNeverWritesPosts() {
        var postUrl = createPost(account, "batch", "짧아요", Set.of(), OpenType.SCHEDULED, true);
        var before = snapshot(postUrl);
        var dailyViewsBefore = postDailyViewRepository.count();

        var sink = new ArrayList<String>();
        var session = new AiSseSession(new ObjectMapper(), new AiSseSession.FrameSink() {
            @Override
            public void write(String frame) {
                sink.add(frame);
            }

            @Override
            public void complete() {
            }
        });
        var seen = new ArrayList<AiPostMeta.Target>();
        var result = new AiMetaBatch(
                url -> postMeta.find(BLOG_ID, url),
                (post, usage) -> {
                    seen.add(post);
                    return AiMetaDto.builder().title("제안 제목").summary("제안 요약이에요").tags(List.of("Java")).build();
                },
                () -> {
                },
                AiModel.HAIKU).run(List.of(postUrl), Set.copyOf(AiMetaBatch.ALL_FIELDS), session);

        assertEquals(1, result.suggested());
        assertEquals("본문 batch\n\n## 단락\n\n내용", seen.get(0).content(), "content is read for the prompt");
        assertTrue(sink.stream().anyMatch(f -> f.contains("\"suggested\":{\"title\":\"제안 제목\"")));
        // 글은 그대로(수정 시각 포함), 조회수·조회 통계도 그대로
        assertEquals(before, snapshot(postUrl));
        assertEquals(dailyViewsBefore, postDailyViewRepository.count());
        assertFalse(redisTemplate.opsForHash().hasKey("post-hits", before.id().toString()));
    }

    @Test
    void applyChangesOnlyTheGivenFields() {
        var postUrl = createPost(account, "apply", "짧아요", Set.of("WPF"), OpenType.SCHEDULED, true);
        var before = snapshot(postUrl);
        assertNotNull(before.thumbnailId());

        // 요약만
        var result = aiService.metaApply(account.getId(), new AiMetaApplyRequestDto(List.of(
                AiMetaApplyRequestDto.Item.builder().postUrl(postUrl).summary("  새 요약이에요  ").build())));
        assertEquals(1, result.getApplied());
        assertEquals(List.of(), result.getFailed());
        var afterSummary = snapshot(postUrl);
        assertEquals("새 요약이에요", afterSummary.summary());
        assertSameExcept(before, afterSummary, "summary");

        // 태그만 (앞뒤 공백·'#'·중복 정리)
        aiService.metaApply(account.getId(), new AiMetaApplyRequestDto(List.of(
                AiMetaApplyRequestDto.Item.builder().postUrl(postUrl).tags(List.of(" Java ", "#보안", "java")).build())));
        var afterTags = snapshot(postUrl);
        assertEquals(Set.of("Java", "보안"), afterTags.tags());
        assertSameExcept(afterSummary, afterTags, "tags");

        // 제목만
        aiService.metaApply(account.getId(), new AiMetaApplyRequestDto(List.of(
                AiMetaApplyRequestDto.Item.builder().postUrl(postUrl).title("새 제목").build())));
        var afterTitle = snapshot(postUrl);
        assertEquals("새 제목", afterTitle.title());
        assertSameExcept(afterTags, afterTitle, "title");
    }

    @Test
    void applyChecksOwnershipAndValidatesEachItem() {
        var mine = createPost(account, "own", "짧아요", Set.of("WPF"), OpenType.PUBLIC, false);
        var mine2 = createPost(account, "own2", "짧아요", Set.of(), OpenType.PUBLIC, false);
        var before = snapshot(mine);
        var before2 = snapshot(mine2);

        // 다른 회원은 내 글을 바꿀 수 없다
        var stranger = aiService.metaApply(other.getId(), new AiMetaApplyRequestDto(List.of(
                AiMetaApplyRequestDto.Item.builder().postUrl(mine).summary("남이 바꾼 요약").build())));
        assertEquals(0, stranger.getApplied());
        assertEquals(mine, stranger.getFailed().get(0).getPostUrl());
        assertEquals("글을 찾을 수 없어요.", stranger.getFailed().get(0).getMessage());
        assertEquals(before, snapshot(mine));
        assertTrue(postMeta.find(OTHER_BLOG_ID, mine).isEmpty());
        assertThrows(RestApiException.class, () -> postMeta.apply(OTHER_BLOG_ID, mine, new AiPostMeta.Change(null, "x", null)));

        // 한 항목의 검증 실패는 그 항목만 실패
        var mixed = aiService.metaApply(account.getId(), new AiMetaApplyRequestDto(List.of(
                AiMetaApplyRequestDto.Item.builder().postUrl(mine).summary("가".repeat(256)).build(),
                AiMetaApplyRequestDto.Item.builder().postUrl(mine2).tags(List.of("Java")).build())));
        assertEquals(1, mixed.getApplied());
        assertEquals(1, mixed.getFailed().size());
        assertEquals("요약은 255자까지 쓸 수 있어요.", mixed.getFailed().get(0).getMessage());
        assertEquals(before, snapshot(mine));
        assertEquals(Set.of("Java"), snapshot(mine2).tags());
        assertSameExcept(before2, snapshot(mine2), "tags");

        assertThrows(RestApiException.class, () -> aiService.metaApply(account.getId(), new AiMetaApplyRequestDto(List.of())));
    }

    /**
     * changed 로 적은 필드(와 수정 시각) 말고는 모두 같은지
     */
    private static void assertSameExcept(Snapshot before, Snapshot after, String changed) {
        assertEquals(before.id(), after.id());
        assertEquals(before.postUrl(), after.postUrl());
        assertEquals(before.content(), after.content());
        assertEquals(before.openType(), after.openType());
        assertEquals(before.scheduledAt(), after.scheduledAt());
        assertEquals(before.seriesUrl(), after.seriesUrl());
        assertEquals(before.thumbnailId(), after.thumbnailId());
        assertEquals(before.hits(), after.hits());
        if (!changed.equals("title")) {
            assertEquals(before.title(), after.title());
        }
        if (!changed.equals("summary")) {
            assertEquals(before.summary(), after.summary());
        }
        if (!changed.equals("tags")) {
            assertEquals(before.tags(), after.tags());
        }
    }
}
