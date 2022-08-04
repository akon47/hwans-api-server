package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.blog.PostDto;
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
    public PostDto createPost(PostDto postDto) {
        return null;
    }

    @Override
    public PostDto modifyPost(PostDto postDto) {
        return null;
    }

    @Override
    public void deletePost(String postId) {

    }

    @Override
    public CommentDto createComment(CommentDto commentDto) {
        return null;
    }

    @Override
    public CommentDto modifyComment(CommentDto commentDto) {
        return null;
    }

    @Override
    public void deleteComment(String commentId) {

    }
}
