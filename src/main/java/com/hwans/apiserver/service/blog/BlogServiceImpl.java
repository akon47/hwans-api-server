package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.entity.blog.Like;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.mapper.CommentMapper;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.LikeRepository;
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
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final LikeRepository likeRepository;
    private final AttachmentRepository attachmentRepository;
    private final AccountMapper accountMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    public BlogDetailsDto getBlogDetails(String blogId) {
        var account = accountRepository
                .findByBlogId(blogId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        return BlogDetailsDto.builder()
                .owner(accountMapper.toDto(account))
                .build();
    }

    @Override
    public SliceDto<SimplePostDto> getAllPosts(Optional<UUID> cursorId, int size) {
        List<Post> foundPosts;
        if (cursorId.isPresent()) {
            var foundCursorPost = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPosts = postRepository
                    .findByIdLessThanOrderByIdDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            foundPosts = postRepository
                    .findAllByOrderByIdDesc(PageRequest.of(0, size + 1));
        }
        var last = foundPosts.size() <= size;
        return SliceDto.<SimplePostDto>builder()
                .data(foundPosts.stream().limit(size).map(postMapper::EntityToSimplePostDto).toList())
                .size((int) foundPosts.stream().limit(size).count())
                .empty(foundPosts.isEmpty())
                .first(cursorId.isEmpty())
                .last(last)
                .cursorId(last ? null : foundPosts.stream().limit(size).skip(size - 1).findFirst().map(Post::getId).orElse(null))
                .build();
    }

    @Override
    @Transactional
    public PostDto createPost(UUID authorAccountId, PostRequestDto postRequestDto) {
        var account = accountRepository
                .findById(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        postRepository.findByBlogIdAndPostUrl(account.getBlogId(), postRequestDto.getPostUrl())
                .ifPresent(x -> {
                    throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_POST_URL);
                });
        var post = postMapper.PostRequestDtoToEntity(postRequestDto);
        post.setAuthor(account);
        post.updatePostUrlIfNecessary();
        post.updateSummaryIfNecessary();
        post.setTags(postRequestDto
                .getTags().stream()
                .map(TagDto::getName)
                .map(tagName -> tagRepository
                        .findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet()));

        if(postRequestDto.getThumbnailFileId() != null) {
            var attachment = attachmentRepository
                    .findById(postRequestDto.getThumbnailFileId())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            post.setThumbnailImageImage(attachment);
        }

        var savedPost = postRepository.save(post);
        return postMapper.EntityToPostDto(savedPost);
    }

    @Override
    @Transactional
    public PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        if (!foundPost.getPostUrl().equals(postRequestDto.getPostUrl())) {
            postRepository.findByBlogIdAndPostUrl(blogId, foundPost.getPostUrl())
                    .ifPresent(x -> {
                        throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_POST_URL);
                    });
        } else {
            foundPost.setPostUrl(postRequestDto.getPostUrl());
        }

        foundPost.setTitle(postRequestDto.getTitle());
        foundPost.setContent(postRequestDto.getContent());
        foundPost.updatePostUrlIfNecessary();
        foundPost.updateSummaryIfNecessary();
        foundPost.setTags(postRequestDto
                .getTags().stream()
                .map(TagDto::getName)
                .map(tagName -> tagRepository
                        .findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet()));

        if(postRequestDto.getThumbnailFileId() != null) {
            var attachment = attachmentRepository
                    .findById(postRequestDto.getThumbnailFileId())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPost.setThumbnailImageImage(attachment);
        }

        return postMapper.EntityToPostDto(foundPost);
    }

    @Override
    @Transactional
    public void deletePost(String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        foundPost.delete();
    }

    @Override
    public SliceDto<SimplePostDto> getBlogPosts(String blogId, Optional<UUID> cursorId, int size) {
        List<Post> foundPosts;
        if (cursorId.isPresent()) {
            var foundCursorPost = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPosts = postRepository
                    .findByIdLessThanOrderByIdDesc(blogId, foundCursorPost.getId(), foundCursorPost.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            foundPosts = postRepository
                    .findAllByOrderByIdDesc(blogId, PageRequest.of(0, size + 1));
        }
        var last = foundPosts.size() <= size;
        return SliceDto.<SimplePostDto>builder()
                .data(foundPosts.stream().limit(size).map(postMapper::EntityToSimplePostDto).toList())
                .size((int) foundPosts.stream().limit(size).count())
                .empty(foundPosts.isEmpty())
                .first(cursorId.isEmpty())
                .last(last)
                .cursorId(last ? null : foundPosts.stream().limit(size).skip(size - 1).findFirst().map(Post::getId).orElse(null))
                .build();
    }

    @Override
    public PostDto getPost(String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        return postMapper.EntityToPostDto(foundPost);
    }

    @Override
    @Transactional
    public void likePost(UUID actorAccountId, String blogId, String postUrl) {
        var account = accountRepository
                .findById(actorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));

        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));

        likeRepository.save(Like.builder().account(account).post(foundPost).build());
    }

    @Override
    @Transactional
    public void unlikePost(UUID actorAccountId, String blogId, String postUrl) {
        var account = accountRepository
                .findById(actorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));

        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));

        var like = likeRepository
                .findByAccountIdAndPostId(account.getId(), foundPost.getId())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        likeRepository.delete(like);
    }

    @Override
    @Transactional
    public CommentDto createComment(UUID authorAccountId, String blogId, String postUrl, CommentRequestDto commentRequestDto) {
        var authorAccount = accountRepository
                .findById(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        var comment = commentMapper.toEntity(commentRequestDto);
        comment.setAuthor(authorAccount);
        comment.setPost(foundPost);
        var savedComment = commentRepository.save(comment);
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto modifyComment(UUID commentId, CommentRequestDto commentRequestDto) {
        var foundComment = commentRepository
                .findByIdAndDeletedIsFalse(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        foundComment.setContent(commentRequestDto.getContent());
        var savedComment = commentRepository.save(foundComment);
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        var foundComment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        foundComment.delete();
    }
}
