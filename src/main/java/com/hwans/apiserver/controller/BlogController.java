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
import com.nimbusds.oauth2.sdk.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

/**
 * 블로그 Controller
 */
@RestController
@Api(tags = "블로그")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;
    private final NotificationService notificationService;

    @ApiOperation(value = "전체 블로그 게시글 조회", notes = "전체 블로그 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/posts")
    public SliceDto<SimplePostDto> getAllPosts(@ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                               @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size,
                                               @ApiParam(value = "검색어") @RequestParam(required = false) String search) {
        return blogService.getAllPosts(search, cursorId, size);
    }

    @ApiOperation(value = "게시글 작성", notes = "게시글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/posts")
    public PostDto createPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                              @ApiParam(value = "게시글", required = true) @RequestBody @Valid final PostRequestDto postRequestDto) {
        return blogService.createPost(userAuthenticationDetails.getId(), postRequestDto);
    }

    @ApiOperation(value = "게시글 수정", notes = "게시글을 수정한다.", tags = "블로그")
    @PutMapping(value = "/v1/blog/posts/{postUrl}")
    public PostDto modifyPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                              @ApiParam(value = "게시글 Url") @PathVariable String postUrl,
                              @ApiParam(value = "게시글", required = true) @RequestBody @Valid final PostRequestDto postRequestDto) {
        return blogService.modifyPost(userAuthenticationDetails.getBlogId(), postUrl, postRequestDto);
    }

    @ApiOperation(value = "게시글 삭제", notes = "게시글을 삭제한다.", tags = "블로그")
    @DeleteMapping(value = "/v1/blog/posts/{postUrl}")
    public void modifyPost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                           @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        blogService.deletePost(userAuthenticationDetails.getBlogId(), postUrl);
    }

    @ApiOperation(value = "특정 블로그 전체 게시글 조회", notes = "특정 블로그 전체 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}/posts")
    public SliceDto<SimplePostDto> getBlogPosts(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                                @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                                @ApiParam(value = "특정 태그만 조회 시 해당 태그") @RequestParam(required = false) String tag,
                                                @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        return blogService.getBlogPosts(blogId, tag, cursorId, size, findPublicPostOnly);
    }

    @ApiOperation(value = "특정 블로그 주인이 좋아요 한 전체 게시글 조회", notes = "특정 블로그 주인이 좋아요 한 전체 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}/likes")
    public SliceDto<SimplePostDto> getBloggerLikePosts(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                                       @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                       @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return blogService.getBloggerLikePosts(blogId, cursorId, size);
    }

    @ApiOperation(value = "블로그 게시글 조회", notes = "블로그 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}")
    public PostDto getPost(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                           @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                           @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        var post = blogService.getPost(blogId, postUrl);
        if (findPublicPostOnly && post.getOpenType() != OpenType.PUBLIC) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST);
        }
        return post;
    }

    @ApiOperation(value = "블로그 정보 조회", notes = "블로그 정보를 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}")
    public BlogDetailsDto getBlogDetails(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                         @ApiParam(value = "블로그 Id") @PathVariable String blogId) {
        boolean findPublicPostOnly = userAuthenticationDetails == null || !userAuthenticationDetails.getBlogId().equals(blogId);
        return blogService.getBlogDetails(blogId, findPublicPostOnly);
    }

    @ApiOperation(value = "댓글 작성", notes = "게시글에 댓글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/comments")
    public CommentDto createComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                    @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                    @ApiParam(value = "게시글 Url") @PathVariable String postUrl,
                                    @ApiParam(value = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto) {
        var comment = blogService.createComment(userAuthenticationDetails.getId(), blogId, postUrl, commentRequestDto);
        notificationService.createCommentNotification(comment);
        return comment;
    }

    @ApiOperation(value = "비회원 댓글 작성", notes = "게시글에 비회원 댓글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/comments/guest")
    public CommentDto createComment(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                    @ApiParam(value = "게시글 Url") @PathVariable String postUrl,
                                    @ApiParam(value = "비회원 댓글", required = true) @RequestBody @Valid final GuestCommentRequestDto guestCommentRequestDto) {
        var comment = blogService.createGuestComment(blogId, postUrl, guestCommentRequestDto);
        notificationService.createCommentNotification(comment);
        return comment;
    }

    @ApiOperation(value = "댓글 수정", notes = "댓글을 수정한다.", tags = "블로그")
    @PutMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto modifyComment(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                                    @ApiParam(value = "댓글 Id") @PathVariable UUID commentId,
                                    @ApiParam(value = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto,
                                    @ApiParam(value = "비회원 댓글인 경우 비밀번호") @RequestParam(required = false) String password) {
        checkCommentPermission(commentId, userAuthenticationDetails, password);
        return blogService.modifyComment(commentId, commentRequestDto);
    }

    @ApiOperation(value = "댓글 삭제", notes = "댓글을 삭제한다.", tags = "블로그")
    @DeleteMapping(value = "/v1/blog/comments/{commentId}")
    public void deleteComment(@CurrentAuthenticationDetailsOrElseNull UserAuthenticationDetails userAuthenticationDetails,
                              @ApiParam(value = "댓글 Id") @PathVariable UUID commentId,
                              @ApiParam(value = "비회원 댓글인 경우 비밀번호") @RequestParam(required = false) String password) {
        checkCommentPermission(commentId, userAuthenticationDetails, password);
        blogService.deleteComment(commentId);
    }

    @ApiOperation(value = "댓글 조회", notes = "댓글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto getComment(@ApiParam(value = "댓글 Id") @PathVariable UUID commentId) {
        return blogService.getComment(commentId);
    }

    @ApiOperation(value = "대댓글 작성", notes = "댓글에 댓글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto createCommentToComment(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                             @ApiParam(value = "댓글 Id") @PathVariable UUID commentId,
                                             @ApiParam(value = "댓글", required = true) @RequestBody @Valid final CommentRequestDto commentRequestDto) {
        return blogService.createComment(userAuthenticationDetails.getId(), commentId, commentRequestDto);
    }

    @ApiOperation(value = "비회원 대댓글 작성", notes = "댓글에 댓글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/comments/{commentId}/guest")
    public CommentDto createCommentToComment(@ApiParam(value = "댓글 Id") @PathVariable UUID commentId,
                                             @ApiParam(value = "비회원 댓글", required = true) @RequestBody @Valid final GuestCommentRequestDto guestCommentRequestDto) {
        return blogService.createGuestComment(commentId, guestCommentRequestDto);
    }

    @ApiOperation(value = "게시글 좋아요 하기", notes = "게시글을 좋아요 한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public void likePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                         @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                         @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        blogService.likePost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @ApiOperation(value = "게시글 좋아요 취소하기", notes = "게시글 좋아요를 취소한다.", tags = "블로그")
    @DeleteMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public void unlikePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                           @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                           @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        blogService.unlikePost(userAuthenticationDetails.getId(), blogId, postUrl);
    }

    @ApiOperation(value = "게시글에 좋아요를 했는지 여부", notes = "게시글에 좋아요를 했는지 여부를 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}/likes")
    public ResponseEntity isLikePost(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                     @ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                     @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        var exists = blogService.isLikePost(userAuthenticationDetails.getId(), blogId, postUrl);
        return exists ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 전달된 인증정보 또는 비밀번호에 따라 해당 댓글에 대해서 수정/삭제 동작이 가능한지 검사한다.
     * 가능하지 않으면 예외를 발생시킨다.
     *
     * @param commentId                 댓글 Id
     * @param userAuthenticationDetails 인증정보에 따른 사용자 정보
     * @param password                  댓글 비밀번호
     */
    private void checkCommentPermission(UUID commentId, UserAuthenticationDetails userAuthenticationDetails, String password) {
        // 비밀번호가 전달되었는데 일치하지 않으면 예외를 발생시킨다.
        if (StringUtils.isNotBlank(password)) {
            if (!blogService.matchCommentAuthorPassword(commentId, password)) {
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
}
