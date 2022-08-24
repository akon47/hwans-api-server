package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

public interface BlogService {
    BlogDetailsDto getBlogDetails(String blogId);
    SliceDto<SimplePostDto> getAllPosts(Optional<UUID> cursorId, int size);
    PostDto createPost(UUID authorAccountId, PostRequestDto postRequestDto);
    PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto);
    void deletePost(String blogId, String postUrl);
    SliceDto<SimplePostDto> getBlogPosts(String blogId, Optional<UUID> cursorId, int size);
    SliceDto<SimplePostDto> getBloggerLikePosts(String blogId, Optional<UUID> cursorId, int size);
    PostDto getPost(String blogId, String postUrl);
    void likePost(UUID actorAccountId, String blogId, String postUrl);
    void unlikePost(UUID actorAccountId, String blogId, String postUrl);
    boolean isLikePost(UUID accountId, String blogId, String postUrl);
    CommentDto createComment(UUID authorAccountId, String blogId, String postUrl, CommentRequestDto commentRequestDto);
    CommentDto createComment(UUID authorAccountId, UUID commentId, CommentRequestDto commentRequestDto);
    CommentDto modifyComment(UUID commentId, CommentRequestDto commentRequestDto);
    CommentDto getComment(UUID commentId);
    void deleteComment(UUID commentId);
}
