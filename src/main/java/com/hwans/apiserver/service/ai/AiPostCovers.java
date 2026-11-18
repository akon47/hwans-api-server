package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiMissingCoversDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.dto.blog.TagDto;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 커버 일괄 만들기에서 글을 읽고 고치는 부분(짧은 트랜잭션). 글은 항상 그 회원의 블로그 Id 로 찾으므로 남의 글은 보이지 않는다.
 * 썸네일·본문은 기존 글 수정 로직({@link BlogService#modifyPost})으로 바꾸고, 나머지 필드는 지금 값을 그대로 넘긴다.
 */
@Component
@RequiredArgsConstructor
public class AiPostCovers {
    public static final int MISSING_MAX = 500;
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final PostRepository postRepository;
    private final BlogService blogService;

    /**
     * 커버를 만들 글
     *
     * @param hasThumbnail 이미 썸네일이 있는지
     */
    public record Target(String postUrl, String title, String summary, List<String> tags, boolean hasThumbnail) {
    }

    @Transactional(readOnly = true)
    public Optional<Target> find(String blogId, String postUrl) {
        return postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .map(post -> new Target(post.getPostUrl(), post.getTitle(), post.getSummary(),
                        post.getTags().stream().map(Tag::getName).toList(), post.getThumbnailImage() != null));
    }

    /**
     * 만든 커버를 글에 반영한다.
     *
     * @param fileId            커버 첨부 Id (그 회원 소유, 글마다 새로 만든 것이라 썸네일 @OneToOne 과 겹치지 않는다)
     * @param imageUrl          본문에 넣을 절대 URL
     * @param useAsThumbnail    썸네일로 지정할지
     * @param insertIntoContent 본문 맨 앞에 이미지를 넣을지
     */
    @Transactional
    public void apply(String blogId, String postUrl, UUID fileId, String imageUrl, boolean useAsThumbnail, boolean insertIntoContent) {
        if (!useAsThumbnail && !insertIntoContent) {
            return;
        }
        var post = postRepository.findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        var request = PostRequestDto.builder()
                .postUrl(post.getPostUrl())
                .title(post.getTitle())
                .content(insertIntoContent ? prependImage(post.getContent(), post.getTitle(), imageUrl) : post.getContent())
                .openType(post.getOpenType())
                .summary(post.getSummary())
                // null 이면 modifyPost 는 썸네일을 그대로 둔다
                .thumbnailFileId(useAsThumbnail ? fileId : null)
                .tags(post.getTags().stream().map(tag -> new TagDto(tag.getName())).collect(Collectors.toSet()))
                .seriesUrl(post.getSeriesUrl())
                .scheduledAt(post.getScheduledAt())
                .build();
        blogService.modifyPost(blogId, postUrl, request);
    }

    /**
     * 본문 맨 앞에 {@code ![제목](URL)} 과 빈 줄을 넣는다. 제목의 대괄호는 이스케이프한다([WPF] 같은 제목이 링크 문법을 깨지 않게).
     */
    static String prependImage(String content, String title, String imageUrl) {
        var alt = (title == null ? "" : title).replaceAll("\\s+", " ").strip()
                .replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]");
        return "![" + alt + "](" + imageUrl + ")\n\n" + (content == null ? "" : content);
    }

    /**
     * 그 회원의 썸네일 없는 글 (공개·비공개·임시저장·예약 모두, 최신 글부터 최대 500개). total 은 전체 수.
     */
    @Transactional(readOnly = true)
    public AiMissingCoversDto missing(UUID accountId) {
        var rows = postRepository.findWithoutThumbnailByAccountId(accountId, PageRequest.of(0, MISSING_MAX));
        return AiMissingCoversDto.builder()
                .total(postRepository.countWithoutThumbnailByAccountId(accountId))
                .posts(rows.stream().map(row -> AiMissingCoversDto.Post.builder()
                        .postUrl(row.getPostUrl())
                        .title(row.getTitle())
                        .openType(row.getOpenType() == null ? null : row.getOpenType().name())
                        .createdAt(row.getCreatedAt() == null ? null : row.getCreatedAt().format(CREATED_AT_FORMAT))
                        .build()).toList())
                .build();
    }
}
