package com.hwans.apiserver.service.blog;

import com.google.common.collect.Streams;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.blog.tag.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final AccountRepository accountRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;

    @Override
    public SliceDto<SimplePostDto> getAllPosts(Optional<UUID> cursorId, int size) {
        List<Post> foundPosts;
        if(cursorId.isPresent()) {
            var foundCursorPost = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPosts = postRepository.findByIdLessThanOrderByIdDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            foundPosts = postRepository.findAllByOrderByIdDesc(PageRequest.of(0, size + 1));
        }
        boolean isLast = foundPosts.size() <= size;
        var lastPost = Streams.findLast(foundPosts.stream().limit(size));
        return SliceDto.<SimplePostDto>builder()
                .data(foundPosts.stream().limit(size).map(x -> postMapper.EntityToSimplePostDto(x)).toList())
                .first(cursorId.isEmpty())
                .last(isLast)
                .empty(foundPosts.isEmpty())
                .size((int)foundPosts.stream().limit(size).count())
                .cursorId(lastPost.isPresent() ? lastPost.get().getId() : null)
                .build();
    }

    @Override
    @Transactional
    public PostDto createPost(String accountEmail, PostRequestDto postRequestDto) {
        var account = accountRepository
                .findByEmail(accountEmail)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var post = postMapper.PostRequestDtoToEntity(postRequestDto);
        post.setAuthor(account);
        post.setTags(postRequestDto.getTags().stream()
                .map(tag -> tag.getName())
                .map(tagName -> tagRepository
                        .findByName(tagName)
                        .orElse(tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet()));
        var savedPost = postRepository.save(post);
        return postMapper.EntityToPostDto(savedPost);
    }

    @Override
    @Transactional
    public PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto) {
        var foundPost = postRepository.findByBlogIdAndPostUrl(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        foundPost.modify(postRequestDto);
        return postMapper.EntityToPostDto(foundPost);
    }

    @Override
    @Transactional
    public void deletePost(String blogId, String postUrl) {
        var foundPost = postRepository.findByBlogIdAndPostUrl(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        foundPost.setDelete();
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
