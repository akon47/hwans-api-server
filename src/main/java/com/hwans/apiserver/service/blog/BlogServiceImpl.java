package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.blog.CommentRequestDto;
import com.hwans.apiserver.dto.blog.PostDto;
import com.hwans.apiserver.dto.blog.PostRequestDto;
import com.hwans.apiserver.repository.blog.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final PostRepository postRepository;

    @Override
    public PostDto createPost(String blogId, PostRequestDto postRequestDto) {


        return null;
    }

    @Override
    public PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto) {
        return null;
    }

    @Override
    public void deletePost(String blogId, String postUrl) {

    }

    @Override
    public PostDto getPost(String blogId, String postUrl) {
        return null;
    }

    @Override
    public CommentDto createComment(String blogId, String postUrl, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    public CommentDto modifyComment(String commentId, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    public void deleteComment(String commentId) {

    }
}
