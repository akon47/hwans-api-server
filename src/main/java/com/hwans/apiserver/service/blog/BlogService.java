package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;

public interface BlogService {
    SliceDto<SimplePostDto> getAllPosts(String cursorId, Long size);
    PostDto createPost(String accountEmail, PostRequestDto postRequestDto);
    PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto);
    void deletePost(String blogId, String postUrl);
    PostDto getPost(String blogId, String postUrl);
    CommentDto createComment(String blogId, String postUrl, CommentRequestDto commentRequestDto);
    CommentDto modifyComment(String commentId, CommentRequestDto commentRequestDto);
    void deleteComment(String commentId);
}
