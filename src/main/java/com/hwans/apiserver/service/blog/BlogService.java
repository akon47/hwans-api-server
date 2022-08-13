package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

public interface BlogService {
    SliceDto<SimplePostDto> getAllPosts(Optional<UUID> cursorId, int size);
    PostDto createPost(String accountEmail, PostRequestDto postRequestDto);
    PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto);
    void deletePost(String blogId, String postUrl);
    SliceDto<SimplePostDto> getPosts(String blogId, Optional<UUID> cursorId, int size);
    PostDto getPost(String blogId, String postUrl);
    void likePost(String accountEmail, String blogId, String postUrl);
    void unlikePost(String accountEmail, String blogId, String postUrl);
    CommentDto createComment(String blogId, String postUrl, CommentRequestDto commentRequestDto);
    CommentDto modifyComment(String commentId, CommentRequestDto commentRequestDto);
    void deleteComment(String commentId);
}
