package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.blog.PostDto;

public interface BlogService {
    PostDto createPost(PostDto postDto);
    PostDto modifyPost(PostDto postDto);
    void deletePost(String postId);
    CommentDto createComment(CommentDto commentDto);
    CommentDto modifyComment(CommentDto commentDto);
    void deleteComment(String commentId);
}
