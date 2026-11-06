package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetailsOrElseNull;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.blog.BlogService;
import com.hwans.apiserver.service.notification.NotificationService;
import com.hwans.apiserver.service.websocket.WebSocketService;
import org.springframework.util.StringUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 블로그 Controller
 */
@Validated
@RestController
@Tag(name = "블로그")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;
    private final NotificationService notificationService;
    private final WebSocketService webSocketService;

    @Operation(summary = "전체 블로그 게시글 조회", description = "전체 블로그 게시글을 조회한다.")
    @GetMapping(value = "/v1/blog/posts")
    public SliceDto<SimplePostDto> getAllPosts(@Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                               @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size,
                                               @Parameter(description = "검색어") @RequestParam(required = false) String search,
                                               @Parameter(description = "정렬") @RequestParam(required = false) @Pattern(regexp = "^createdAt|hits$") String sortBy) {
        return blogService.getAllPosts(search, cursorId, size, sortBy);
    }

    @Operation(summary = "현재 보고 있는 게시글 조회", description = "현재 한 명 이상이 보고 있는 공개 게시글을 시청자 수가 많은 순으로 조회한다.")
    @GetMapping(value = "/v1/blog/posts/viewing")
    public List<SimplePostDto> getViewingPosts() {
        var viewerCounts = webSocketService.getActiveViewerCounts();
        if (viewerCounts.isEmpty()) {
            return List.of();
        }
        var postIds = viewerCounts.keySet().stream()
                .map(this::parsePostId)
                .filter(java.util.Objects::nonNull)
                .toList();
        return blogService.getPostsByIds(postIds).stream()
                .sorted(java.util.Comparator
                        .comparingInt((SimplePostDto post) -> viewerCounts.getOrDefault(post.getId().toString(), 0))
                        .reversed())
                .toList();
    }

    private UUID parsePostId(String postId) {
        try {
            return UUID.fromString(postId);
        } catch (Exception e) {
            return null;
        }
    }

    @Operation(summary = "게시글 작성", description = "게시글을 작성한다.")
    @PostMapping(value = "/v1/blog/posts")
    public PostDto createPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                              @Parameter(description = "게시글", required = true) @RequestBody @Valid final PostRequestDto postRequestDto) {
        return blogService.createPost(userAuthenticationDetails.getId(), postRequestDto);
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정한다.")
    @PutMapping(value = "/v1/blog/posts/{postUrl}")
    public PostDto modifyPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                              @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                              @Parameter(description = "게시글", required = true) @RequestBody @Valid final PostRequestDto postRequestDto) {
        return blogService.modifyPost(userAuthenticationDetails.getBlogId(), postUrl, postRequestDto);
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제한다.")
    @DeleteMapping(value = "/v1/blog/posts/{postUrl}")
    public void deletePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                           @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        blogService.deletePost(userAuthenticationDetails.getBlogId(), postUrl);
    }

    @Operation(summary = "특정 블로그 전체 게시글 조회", description = "특정 블로그 전체 게시글을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts")
    public SliceDto<SimplePostDto> getBlogPosts(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                                @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                                @Parameter(description = "특정 태그만 조회 시 해당 태그") @RequestParam(required = false) String tag,
                                                @Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        return blogService.getBlogPosts(blogId, tag, cursorId, size, findPublicPostOnly);
    }

    @Operation(summary = "특정 블로그 주인이 좋아요 한 전체 게시글 조회", description = "특정 블로그 주인이 좋아요 한 전체 게시글을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/likes")
    public SliceDto<SimplePostDto> getBloggerLikePosts(@Parameter(description = "블로그 Id") @PathVariable String blogId,
                                                       @Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                       @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return blogService.getBloggerLikePosts(blogId, cursorId, size);
    }

    @Operation(summary = "블로그 게시글 조회", description = "블로그 게시글을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}")
    public PostDto getPost(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                           @Parameter(description = "블로그 Id") @PathVariable String blogId,
                           @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        var post = blogService.getPost(blogId, postUrl);
        if (findPublicPostOnly && post.getOpenType() != OpenType.PUBLIC) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST);
        }
        blogService.increasePostHits(post.getId());

        return post.withHits(post.getHits() + 1);
    }

    @Operation(summary = "관련 게시글 조회", description = "해당 게시글과 태그가 겹치는 공개 게시글을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/related")
    public List<SimplePostDto> getRelatedPosts(@Parameter(description = "블로그 Id") @PathVariable String blogId,
                                               @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                                               @Parameter(description = "조회할 최대 개수") @RequestParam(required = false, defaultValue = "5") int size) {
        return blogService.getRelatedPosts(blogId, postUrl, size);
    }

    @Operation(summary = "블로그 정보 조회", description = "블로그 정보를 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}")
    public BlogDetailsDto getBlogDetails(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                         @Parameter(description = "블로그 Id") @PathVariable String blogId) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        return blogService.getBlogDetails(blogId, findPublicPostOnly);
    }

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성한다.")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/comments")
    public CommentDto createComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                    @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                    @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                                    @Parameter(description = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto) {
        var comment = blogService.createComment(userAuthenticationDetails.getId(), blogId, postUrl, commentRequestDto);
        notificationService.createCommentNotification(comment);
        return comment;
    }

    @Operation(summary = "비회원 댓글 작성", description = "게시글에 비회원 댓글을 작성한다.")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/comments/guest")
    public CommentDto createComment(@Parameter(description = "블로그 Id") @PathVariable String blogId,
                                    @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                                    @Parameter(description = "비회원 댓글", required = true) @RequestBody @Valid final GuestCommentRequestDto guestCommentRequestDto) {
        var comment = blogService.createGuestComment(blogId, postUrl, guestCommentRequestDto);
        notificationService.createCommentNotification(comment);
        return comment;
    }

    @Operation(summary = "댓글 수정", description = "댓글을 수정한다.")
    @PutMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto modifyComment(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                    @Parameter(description = "댓글 Id") @PathVariable UUID commentId,
                                    @Parameter(description = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto,
                                    @Parameter(description = "비회원 댓글인 경우 비밀번호") @RequestParam(required = false) String password) {
        checkCommentModifyPermission(commentId, userAuthenticationDetails, password);
        return blogService.modifyComment(commentId, commentRequestDto);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제한다.")
    @DeleteMapping(value = "/v1/blog/comments/{commentId}")
    public void deleteComment(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                              @Parameter(description = "댓글 Id") @PathVariable UUID commentId,
                              @Parameter(description = "비회원 댓글인 경우 비밀번호") @RequestParam(required = false) String password) {
        checkCommentDeletePermission(commentId, userAuthenticationDetails, password);
        blogService.deleteComment(commentId);
    }

    @Operation(summary = "댓글 조회", description = "댓글을 조회한다.")
    @GetMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto getComment(@Parameter(description = "댓글 Id") @PathVariable UUID commentId) {
        return blogService.getComment(commentId);
    }

    @Operation(summary = "대댓글 작성", description = "댓글에 댓글을 작성한다.")
    @PostMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto createCommentToComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                             @Parameter(description = "댓글 Id") @PathVariable UUID commentId,
                                             @Parameter(description = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto) {
        return blogService.createComment(userAuthenticationDetails.getId(), commentId, commentRequestDto);
    }

    @Operation(summary = "비회원 대댓글 작성", description = "댓글에 댓글을 작성한다.")
    @PostMapping(value = "/v1/blog/comments/{commentId}/guest")
    public CommentDto createCommentToComment(@Parameter(description = "댓글 Id") @PathVariable UUID commentId,
                                             @Parameter(description = "비회원 댓글", required = true) @RequestBody @Valid final GuestCommentRequestDto guestCommentRequestDto) {
        return blogService.createGuestComment(commentId, guestCommentRequestDto);
    }

    @Operation(summary = "게시글 좋아요 하기", description = "게시글을 좋아요 한다.")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public void likePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                         @Parameter(description = "블로그 Id") @PathVariable String blogId,
                         @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        blogService.likePost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @Operation(summary = "게시글 좋아요 취소하기", description = "게시글 좋아요를 취소한다.")
    @DeleteMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public void unlikePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                           @Parameter(description = "블로그 Id") @PathVariable String blogId,
                           @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        blogService.unlikePost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @Operation(summary = "게시글에 좋아요를 했는지 여부", description = "게시글에 좋아요를 했는지 여부를 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public ResponseEntity isLikePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                     @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                     @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        var exists = blogService.isLikePost(userAuthenticationDetails.getId(), blogId, postUrl);
        return exists ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "게시글 북마크 하기", description = "게시글을 북마크(나중에 읽기)에 추가한다.")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/bookmarks")
    public void bookmarkPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                             @Parameter(description = "블로그 Id") @PathVariable String blogId,
                             @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        blogService.bookmarkPost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @Operation(summary = "게시글 북마크 취소하기", description = "게시글 북마크를 취소한다.")
    @DeleteMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/bookmarks")
    public void unbookmarkPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                               @Parameter(description = "블로그 Id") @PathVariable String blogId,
                               @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        blogService.unbookmarkPost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @Operation(summary = "게시글을 북마크 했는지 여부", description = "게시글을 북마크 했는지 여부를 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/bookmarks")
    public ResponseEntity isBookmarked(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                       @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                       @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        var exists = blogService.isBookmarked(userAuthenticationDetails.getId(), blogId, postUrl);
        return exists ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "내가 북마크 한 게시글 목록 조회", description = "현재 사용자가 북마크 한 게시글 목록을 조회한다.")
    @GetMapping(value = "/v1/blog/me/bookmarks")
    public SliceDto<SimplePostDto> getMyBookmarks(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                                  @Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                  @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return blogService.getMyBookmarkedPosts(userAuthenticationDetails.getId(), cursorId, size);
    }

    @Operation(summary = "게시글에 이모지 반응 추가", description = "게시글에 이모지 반응을 추가한다.")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/reactions")
    public void addReaction(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                            @Parameter(description = "블로그 Id") @PathVariable String blogId,
                            @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                            @Parameter(description = "이모지", required = true) @RequestBody @Valid final ReactionRequestDto reactionRequestDto) {
        blogService.addReaction(userAuthenticationDetails.getId(), blogId, postUrl, reactionRequestDto.getEmoji());
    }

    @Operation(summary = "게시글 이모지 반응 취소", description = "게시글의 이모지 반응을 취소한다.")
    @DeleteMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/reactions")
    public void removeReaction(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                               @Parameter(description = "블로그 Id") @PathVariable String blogId,
                               @Parameter(description = "게시글 Url") @PathVariable String postUrl,
                               @Parameter(description = "이모지", required = true) @RequestParam String emoji) {
        blogService.removeReaction(userAuthenticationDetails.getId(), blogId, postUrl, emoji);
    }

    @Operation(summary = "게시글 이모지 반응 집계 조회", description = "게시글의 이모지 반응 집계를 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/reactions")
    public List<ReactionDto> getReactions(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                          @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                          @Parameter(description = "게시글 Url") @PathVariable String postUrl) {
        var accountId = userAuthenticationDetails == null ? null : userAuthenticationDetails.getId();
        return blogService.getReactions(accountId, blogId, postUrl);
    }

    @Operation(summary = "댓글 좋아요 하기", description = "댓글을 좋아요 한다.")
    @PostMapping(value = "/v1/blog/comments/{commentId}/likes")
    public void likeComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                            @Parameter(description = "댓글 Id") @PathVariable UUID commentId) {
        blogService.likeComment(userAuthenticationDetails.getId(), commentId);
    }

    @Operation(summary = "댓글 좋아요 취소하기", description = "댓글 좋아요를 취소한다.")
    @DeleteMapping(value = "/v1/blog/comments/{commentId}/likes")
    public void unlikeComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                              @Parameter(description = "댓글 Id") @PathVariable UUID commentId) {
        blogService.unlikeComment(userAuthenticationDetails.getId(), commentId);
    }

    @Operation(summary = "댓글에 좋아요를 했는지 여부", description = "댓글에 좋아요를 했는지 여부를 조회한다.")
    @GetMapping(value = "/v1/blog/comments/{commentId}/likes")
    public ResponseEntity isCommentLiked(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                         @Parameter(description = "댓글 Id") @PathVariable UUID commentId) {
        var exists = blogService.isCommentLiked(userAuthenticationDetails.getId(), commentId);
        return exists ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @Operation(summary = "내 블로그 통계 조회", description = "현재 사용자(블로그 작성자)의 통계 정보를 조회한다.")
    @GetMapping(value = "/v1/blog/me/statistics")
    public BlogStatisticsDto getMyStatistics(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails) {
        return blogService.getMyBlogStatistics(userAuthenticationDetails.getId());
    }

    @Operation(summary = "시리즈 생성", description = "시리즈를 작성한다.")
    @PostMapping(value = "/v1/blog/series")
    public SeriesDto createSeries(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                  @Parameter(description = "시리즈", required = true) @RequestBody @Valid final SeriesRequestDto seriesRequestDto) {
        return blogService.createSeries(userAuthenticationDetails.getId(), seriesRequestDto);
    }

    @Operation(summary = "시리즈 수정", description = "시리즈를 수정한다.")
    @PutMapping(value = "/v1/blog/series/{seriesUrl}")
    public SeriesDto modifySeries(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                  @Parameter(description = "시리즈 Url") @PathVariable String seriesUrl,
                                  @Parameter(description = "시리즈", required = true) @RequestBody @Valid final SeriesRequestDto seriesRequestDto) {
        return blogService.modifySeries(userAuthenticationDetails.getBlogId(), seriesUrl, seriesRequestDto);
    }

    @Operation(summary = "시리즈 삭제", description = "시리즈를 삭제한다.")
    @DeleteMapping(value = "/v1/blog/series/{seriesUrl}")
    public void deleteSeries(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                             @Parameter(description = "시리즈 Url") @PathVariable String seriesUrl) {
        blogService.deleteSeries(userAuthenticationDetails.getBlogId(), seriesUrl);
    }

    @Operation(summary = "특정 블로그 시리즈 상세 조회", description = "특정 블로그의 시리즈를 상세 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/series/{seriesUrl}")
    public SeriesDto getBlogSeriesDetail(@Parameter(description = "블로그 Id") @PathVariable String blogId,
                                         @Parameter(description = "시리즈 Url") @PathVariable String seriesUrl) {
        return blogService.getSeries(blogId, seriesUrl);
    }

    @Operation(summary = "특정 블로그 시리즈 목록 조회", description = "특정 블로그 시리즈 목록을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/series")
    public List<SimpleSeriesDto> getBlogSeries(@Parameter(description = "블로그 Id") @PathVariable String blogId) {
        return blogService.getBlogSeries(blogId);
    }

    @Operation(summary = "특정 블로그 시리즈 게시글 목록 조회", description = "특정 블로그 시리즈 게시글 목록을 조회한다.")
    @GetMapping(value = "/v1/blog/{blogId}/series/{seriesUrl}/posts")
    public List<SimplePostDto> getBlogSeriesPosts(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                                  @Parameter(description = "블로그 Id") @PathVariable String blogId,
                                                  @Parameter(description = "시리즈 Url") @PathVariable String seriesUrl) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        return blogService.getBlogSeriesPosts(blogId, seriesUrl, findPublicPostOnly);
    }

    /**
     * 전달된 인증정보 또는 비밀번호에 따라 해당 댓글에 대해서 수정 동작이 가능한지 검사한다.
     * 가능하지 않으면 예외를 발생시킨다.
     *
     * @param commentId                 댓글 Id
     * @param userAuthenticationDetails 인증정보에 따른 사용자 정보
     * @param password                  댓글 비밀번호
     */
    private void checkCommentModifyPermission(UUID commentId, UserAuthenticationDetails userAuthenticationDetails, String password) {
        var comment = blogService.getComment(commentId);

        // 비회원 댓글인 경우 항상 비밀번호 일치 여부를 검사한다.
        if (comment.getAuthor().isGuest()) {
            if (!StringUtils.hasText(password) || !blogService.matchCommentAuthorPassword(commentId, password)) {
                throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
            } else {
                return;
            }
        }

        // 인증정보가 없거나 댓글의 글쓴이가 아니라면 예외를 발생시킨다.
        var authorId = blogService.getCommentAuthorId(commentId);
        if (userAuthenticationDetails == null || !userAuthenticationDetails.getId().equals(authorId)) {
            throw new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED);
        }
    }

    /**
     * 전달된 인증정보 또는 비밀번호에 따라 해당 댓글에 대해서 삭제 동작이 가능한지 검사한다.
     * 가능하지 않으면 예외를 발생시킨다.
     *
     * @param commentId                 댓글 Id
     * @param userAuthenticationDetails 인증정보에 따른 사용자 정보
     * @param password                  댓글 비밀번호
     */
    private void checkCommentDeletePermission(UUID commentId, UserAuthenticationDetails userAuthenticationDetails, String password) {
        var comment = blogService.getComment(commentId);

        // 댓글의 글쓴이가 나거나 내 게시글이라면 통과시킨다.
        var authorId = blogService.getCommentAuthorId(commentId);
        var postAuthorId = blogService.getPostAuthorId(comment.getPost().getId());
        if (userAuthenticationDetails != null && (userAuthenticationDetails.getId().equals(authorId) || userAuthenticationDetails.getId().equals(postAuthorId))) {
            return;
        }

        // 비회원 댓글인 경우 항상 비밀번호 일치 여부를 검사한다.
        if (comment.getAuthor().isGuest()) {
            if (!StringUtils.hasText(password) || !blogService.matchCommentAuthorPassword(commentId, password)) {
                throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
            } else {
                return;
            }
        }

        throw new RestApiException(ErrorCodes.Forbidden.FORBIDDEN);
    }
}
