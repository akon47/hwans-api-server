package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.blog.CommentRequestDto;
import com.hwans.apiserver.dto.blog.PostDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;

public interface BlogService {
    PostDto createPost(String blogId, PostRequestDto postRequestDto);
    PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto);
    void deletePost(String blogId, String postUrl);
    PostDto getPost(String blogId, String postUrl);
    CommentDto createComment(String blogId, String postUrl, CommentRequestDto commentRequestDto);
    CommentDto modifyComment(String commentId, CommentRequestDto commentRequestDto);
    void deleteComment(String commentId);
}
