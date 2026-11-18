package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiMetaApplyRequestDto;
import com.hwans.apiserver.dto.ai.AiMetaCandidatesDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.TagDto;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.blog.tag.TagRepository;
import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 제목·요약·태그 한꺼번에 채우기(계약 12~14번)에서 글을 읽고 고치는 부분. 글은 항상 그 회원의 블로그 Id 로 찾으므로 남의 글은 보이지 않는다.
 * <ul>
 *   <li>읽기({@link #find})는 저장소에서 바로 읽는다 — 글 조회 API 와 달리 조회수·조회 통계를 올리지 않는다.</li>
 *   <li>고치기({@link #apply})는 기존 글 수정 로직({@link BlogService#modifyPost})에 지금 값을 그대로 넘기고 온 필드만 바꾼다.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AiPostMeta {
    public static final int CANDIDATES_MAX = 500;
    public static final int SHORT_SUMMARY_CHARS = 40;
    static final int TITLE_MAX = 2000;
    static final int SUMMARY_MAX = 255;
    static final int TAGS_MAX = 10;
    static final int TAG_NAME_MAX = 100;
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final BlogService blogService;

    /**
     * 제안을 만들 글 (본문 포함)
     */
    public record Target(String postUrl, String title, String summary, List<String> tags, String content) {
    }

    /**
     * 적용할 값. null 인 필드는 그대로 둔다.
     */
    public record Change(String title, String summary, List<String> tags) {
    }

    @Transactional(readOnly = true)
    public Optional<Target> find(String blogId, String postUrl) {
        return postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .map(post -> new Target(post.getPostUrl(), post.getTitle(), post.getSummary(),
                        post.getTags().stream().map(Tag::getName).sorted().toList(), post.getContent()));
    }

    /**
     * 태그가 없거나 요약이 40자 미만(없음 포함)인 그 회원의 글 (모든 공개 유형, 최신 글부터 최대 500개). total 은 전체 수.
     */
    @Transactional(readOnly = true)
    public AiMetaCandidatesDto candidates(UUID accountId) {
        var tagsByPost = tagRepository.findPostTagNamesByAccountId(accountId).stream()
                .collect(Collectors.groupingBy(TagRepository.PostTagNameRow::getPostId,
                        Collectors.mapping(TagRepository.PostTagNameRow::getName, Collectors.toCollection(TreeSet::new))));
        var candidates = postRepository.findMetaRowsByAccountId(accountId).stream()
                .map(row -> {
                    var tags = List.copyOf(tagsByPost.getOrDefault(row.getId(), new TreeSet<>()));
                    var summary = row.getSummary() == null ? "" : row.getSummary();
                    return AiMetaCandidatesDto.Post.builder()
                            .postUrl(row.getPostUrl())
                            .title(row.getTitle())
                            .openType(row.getOpenType() == null ? null : row.getOpenType().name())
                            .createdAt(row.getCreatedAt() == null ? null : row.getCreatedAt().format(CREATED_AT_FORMAT))
                            .summary(summary)
                            .tags(tags)
                            .noTags(tags.isEmpty())
                            .shortSummary(isShortSummary(summary))
                            .build();
                })
                .filter(post -> post.isNoTags() || post.isShortSummary())
                .toList();
        return AiMetaCandidatesDto.builder()
                .total(candidates.size())
                .posts(candidates.stream().limit(CANDIDATES_MAX).toList())
                .build();
    }

    static boolean isShortSummary(String summary) {
        return summary == null || summary.strip().codePointCount(0, summary.strip().length()) < SHORT_SUMMARY_CHARS;
    }

    /**
     * 받은 값을 검증·정리한다(앞뒤 공백 제거, 태그 '#'·중복 제거). 문제가 있으면 {@code INVALID_PARAMETER}.
     */
    static Change validate(AiMetaApplyRequestDto.Item item) {
        if (item == null || item.getPostUrl() == null || item.getPostUrl().isBlank()) {
            throw invalid("글 주소가 없어요.");
        }
        String title = null;
        if (item.getTitle() != null) {
            title = item.getTitle().strip();
            if (title.isEmpty()) {
                throw invalid("제목이 비어 있어요.");
            }
            if (title.length() > TITLE_MAX) {
                throw invalid("제목은 " + TITLE_MAX + "자까지 쓸 수 있어요.");
            }
        }
        String summary = null;
        if (item.getSummary() != null) {
            summary = item.getSummary().strip();
            if (summary.isEmpty()) {
                throw invalid("요약이 비어 있어요.");
            }
            if (summary.length() > SUMMARY_MAX) {
                throw invalid("요약은 " + SUMMARY_MAX + "자까지 쓸 수 있어요.");
            }
        }
        List<String> tags = null;
        if (item.getTags() != null) {
            var unique = new LinkedHashMap<String, String>();
            for (var raw : item.getTags()) {
                if (raw == null) {
                    continue;
                }
                var tag = raw.strip().replaceFirst("^#+", "").strip();
                if (tag.isEmpty()) {
                    continue;
                }
                if (tag.length() > TAG_NAME_MAX) {
                    throw invalid("태그는 " + TAG_NAME_MAX + "자까지 쓸 수 있어요.");
                }
                unique.putIfAbsent(tag.replaceAll("\\s+", "").toLowerCase(Locale.ROOT), tag);
            }
            if (unique.size() > TAGS_MAX) {
                throw invalid("태그는 " + TAGS_MAX + "개까지 붙일 수 있어요.");
            }
            tags = List.copyOf(unique.values());
        }
        if (title == null && summary == null && tags == null) {
            throw invalid("바꿀 항목(title, summary, tags)이 없어요.");
        }
        return new Change(title, summary, tags);
    }

    /**
     * 온 필드만 바꾼다. 본문·공개 상태·시리즈·예약 시각·썸네일·주소는 지금 값을 그대로 넘긴다(기존 글 수정 로직).
     */
    @Transactional
    public void apply(String blogId, String postUrl, Change change) {
        var post = postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST, "글을 찾을 수 없어요."));
        var tagNames = change.tags() != null ? change.tags() : post.getTags().stream().map(Tag::getName).toList();
        var request = PostRequestDto.builder()
                .postUrl(post.getPostUrl())
                .title(change.title() != null ? change.title() : post.getTitle())
                .content(post.getContent())
                .openType(post.getOpenType())
                .summary(change.summary() != null ? change.summary() : post.getSummary())
                // null 이면 modifyPost 는 썸네일을 그대로 둔다
                .thumbnailFileId(null)
                .tags(tagNames.stream().map(TagDto::new).collect(Collectors.toCollection(LinkedHashSet::new)))
                .seriesUrl(post.getSeriesUrl())
                .scheduledAt(post.getScheduledAt())
                .build();
        blogService.modifyPost(blogId, postUrl, request);
    }

    private static RestApiException invalid(String message) {
        return new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, message);
    }
}
