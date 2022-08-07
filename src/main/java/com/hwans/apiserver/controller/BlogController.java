package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.service.blog.BlogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Set;

@RestController
@Api(tags = "블로그")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class BlogController {
    private final BlogService blogService;

    @ApiOperation(value = "전체 블로그 게시글 조회", notes = "전체 블로그 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/posts")
    public Set<SimplePostDto> getAllPost() {
        return Collections.emptySet();
    }

    @ApiOperation(value = "게시글 작성", notes = "게시글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/{blogId}/posts")
    public PostDto createPost(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                              @ApiParam(value = "게시글", required = true) @RequestBody PostRequestDto postRequestDto) {
        return blogService.createPost(blogId, postRequestDto);
    }

    @ApiOperation(value = "게시글 수정", notes = "게시글을 수정한다.", tags = "블로그")
    @PutMapping(value = "/v1/blog/{blogId}/posts")
    public PostDto modifyPost(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                              @ApiParam(value = "게시글 Url") @PathVariable String postUrl,
                              @ApiParam(value = "게시글", required = true) @RequestBody PostRequestDto postRequestDto) {
        return blogService.modifyPost(blogId, postUrl, postRequestDto);
    }

    @ApiOperation(value = "게시글 삭제", notes = "게시글을 삭제한다.", tags = "블로그")
    @DeleteMapping(value = "/v1/blog/{blogId}/posts")
    public void modifyPost(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                           @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        blogService.deletePost(blogId, postUrl);
    }

    @ApiOperation(value = "블로그 게시글 조회", notes = "블로그 게시글을 조회한다.", tags = "블로그")
    @GetMapping(value = "/v1/blog/{blogId}/posts/{postUrl}")
    public PostDto getPost(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                           @ApiParam(value = "게시글 Url") @PathVariable String postUrl) {
        return blogService.getPost(blogId, postUrl);
    }

    @ApiOperation(value = "댓글 작성", notes = "게시글에 댓글을 작성한다.", tags = "블로그")
    @PostMapping(value = "/v1/blog/{blogId}/{postUrl}/comments")
    public CommentDto createPost(@ApiParam(value = "블로그 Id") @PathVariable String blogId,
                                 @ApiParam(value = "게시글 Url") @PathVariable String postUrl,
                                 @ApiParam(value = "댓글", required = true) @RequestBody CommentRequestDto commentRequestDto) {
        return blogService.createComment(blogId, postUrl, commentRequestDto);
    }

    @ApiOperation(value = "댓글 수정", notes = "댓글을 수정한다.", tags = "블로그")
    @PutMapping(value = "/v1/blog/comments/{commentId}")
    public CommentDto createPost(@ApiParam(value = "댓글 Id") @PathVariable String commentId,
                                 @ApiParam(value = "댓글", required = true) @RequestBody CommentRequestDto commentRequestDto) {
        return blogService.modifyComment(commentId, commentRequestDto);
    }

    @ApiOperation(value = "댓글 삭제", notes = "댓글을 수정한다.", tags = "블로그")
    @DeleteMapping(value = "/v1/blog/comments/{commentId}")
    public void createPost(@ApiParam(value = "댓글 Id") @PathVariable String commentId) {
        blogService.deleteComment(commentId);
    }
}
