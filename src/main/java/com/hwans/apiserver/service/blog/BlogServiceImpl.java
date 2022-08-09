package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.blog.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final AccountRepository accountRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;

    @Override
    public SliceDto<SimplePostDto> getAllPosts(String cursorId, Long size) {
        return SliceDto.<SimplePostDto>builder().build();
    }

    @Override
    @Transactional
    public PostDto createPost(String accountEmail, PostRequestDto postRequestDto) {
        var account = accountRepository
                .findByEmail(accountEmail)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var post = postMapper.PostRequestDtoToEntity(postRequestDto);
        post.setAuthor(account);
        var savedPost = postRepository.save(post);
        return postMapper.PostEntityToPostDto(savedPost);
    }

    @Override
    @Transactional
    public PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto) {
        var foundPost = postRepository.findByBlogIdAndPostUrl(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        foundPost.modify(postRequestDto);
        return postMapper.PostEntityToPostDto(foundPost);
    }

    @Override
    @Transactional
    public void deletePost(String blogId, String postUrl) {
        var foundPost = postRepository.findByBlogIdAndPostUrl(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        postRepository.delete(foundPost);
    }

    @Override
    public PostDto getPost(String blogId, String postUrl) {
        return null;
    }

    @Override
    @Transactional
    public CommentDto createComment(String blogId, String postUrl, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    @Transactional
    public CommentDto modifyComment(String commentId, CommentRequestDto commentRequestDto) {
        return null;
    }

    @Override
    @Transactional
    public void deleteComment(String commentId) {

    }
}
