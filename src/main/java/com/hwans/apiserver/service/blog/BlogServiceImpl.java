package com.hwans.apiserver.service.blog;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.entity.blog.Like;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.entity.blog.Tag;
import com.hwans.apiserver.event.blog.CreateCommentEvent;
import com.hwans.apiserver.event.blog.CreatePostEvent;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.mapper.CommentMapper;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.mapper.SeriesMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.LikeRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.blog.SeriesRepository;
import com.hwans.apiserver.repository.blog.tag.TagRepository;
import com.hwans.apiserver.repository.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 블로그 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class BlogServiceImpl implements BlogService {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;
    private final LikeRepository likeRepository;
    private final AttachmentRepository attachmentRepository;
    private final SeriesRepository seriesRepository;
    private final AccountMapper accountMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final SeriesMapper seriesMapper;
    private final RedisTemplate<String, Integer> redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 레디스에 조회수 저장을 위한 키값
     */
    private static final String POST_HITS_KEY = "post-hits";

    @Override
    public BlogDetailsDto getBlogDetails(String blogId, boolean findPublicPostOnly) {
        var foundAccount = accountRepository
                .findByBlogId(blogId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        if (foundAccount.isGuest()) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND);
        }

        var posts = postRepository.findAllByBlogId(blogId, findPublicPostOnly);
        var tagCounts = posts.stream().flatMap(x -> x.getTags().stream())
                .collect(Collectors.groupingBy(Tag::getName, Collectors.summingInt(x -> 1)))
                .entrySet().stream()
                .map(x -> new TagCountDto(x.getKey(), x.getValue()))
                .collect(Collectors.toList());

        return BlogDetailsDto.builder()
                .owner(accountMapper.toDto(foundAccount))
                .postCount(posts.size())
                .tagCounts(tagCounts)
                .build();
    }

    @Override
    public SliceDto<SimplePostDto> getAllPosts(String search, Optional<UUID> cursorId, int size, String sortBy) {
        List<Post> foundPosts;

        // 조회수 순 정렬 조회인지 여부, 아니라면 생성순이다.
        var isSortByHits = Objects.equals(sortBy, "hits");
        if (cursorId.isPresent()) {
            var foundCursorPost = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            if (search == null) {
                if(isSortByHits) {
                    foundPosts = postRepository.findByCursorLessThanOrderByHitsDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), foundCursorPost.getHits(), PageRequest.of(0, size + 1));
                } else {
                    foundPosts = postRepository.findByCursorLessThanOrderByCreatedAtDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), PageRequest.of(0, size + 1));
                }
            } else {
                if(isSortByHits) {
                    foundPosts = postRepository.findByCursorLessThanOrderByHitsDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), foundCursorPost.getHits(), search, PageRequest.of(0, size + 1));
                } else {
                    foundPosts = postRepository.findByCursorLessThanOrderByCreatedAtDesc(foundCursorPost.getId(), foundCursorPost.getCreatedAt(), search, PageRequest.of(0, size + 1));
                }
            }
        } else {
            if (search == null) {
                if(isSortByHits) {
                    foundPosts = postRepository.findAllByOrderByHitsDesc(PageRequest.of(0, size + 1));
                } else {
                    foundPosts = postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size + 1));
                }
            } else {
                if(isSortByHits) {
                    foundPosts = postRepository.findAllByOrderByHitsDesc(search, PageRequest.of(0, size + 1));
                } else {
                    foundPosts = postRepository.findAllByOrderByCreatedAtDesc(search, PageRequest.of(0, size + 1));
                }
            }
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
        var foundAccount = accountRepository
                .findByIdAndDeletedIsFalse(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        if (foundAccount.isGuest()) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        postRepository
                .findByBlogIdAndPostUrl(foundAccount.getBlogId(), postRequestDto.getPostUrl())
                .ifPresent(x -> {
                    throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_POST_URL);
                });
        var post = postMapper.PostRequestDtoToEntity(postRequestDto);
        post.setAuthor(foundAccount);
        post.updatePostUrlIfNecessary();
        post.setTags(postRequestDto
                .getTags().stream()
                .map(TagDto::getName)
                .map(tagName -> tagRepository
                        .findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet()));

        if (postRequestDto.getSeriesUrl() == null) {
            post.setSeries(null);
        } else {
            var foundSeries = seriesRepository
                    .findByBlogIdAndSeriesUrl(foundAccount.getBlogId(), postRequestDto.getSeriesUrl())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_SERIES));
            post.setSeries(foundSeries);
        }

        if (postRequestDto.getThumbnailFileId() != null) {
            var attachment = attachmentRepository
                    .findById(postRequestDto.getThumbnailFileId())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            post.setThumbnailImage(attachment);
        }

        var savedPost = postRepository.save(post);
        eventPublisher.publishEvent(new CreatePostEvent(this, savedPost));
        return postMapper.EntityToPostDto(savedPost);
    }

    @Override
    @Transactional
    public PostDto modifyPost(String blogId, String postUrl, PostRequestDto postRequestDto) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        if (!foundPost.getPostUrl().equals(postRequestDto.getPostUrl())) {
            postRepository
                    .findByBlogIdAndPostUrl(blogId, postRequestDto.getPostUrl())
                    .ifPresent(x -> {
                        throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_POST_URL);
                    });
            foundPost.setPostUrl(postRequestDto.getPostUrl());
        }

        foundPost.setSummary(postRequestDto.getSummary());
        foundPost.setTitle(postRequestDto.getTitle());
        foundPost.setContent(postRequestDto.getContent());
        foundPost.setOpenType(postRequestDto.getOpenType());
        foundPost.updatePostUrlIfNecessary();
        foundPost.setTags(postRequestDto
                .getTags().stream()
                .map(TagDto::getName)
                .map(tagName -> tagRepository
                        .findByName(tagName)
                        .orElseGet(() -> tagRepository.save(new Tag(tagName))))
                .collect(Collectors.toSet()));

        if (postRequestDto.getSeriesUrl() == null) {
            foundPost.setSeries(null);
        } else if (!postRequestDto.getSeriesUrl().equals(foundPost.getSeriesUrl())) {
            var foundSeries = seriesRepository
                    .findByBlogIdAndSeriesUrl(blogId, postRequestDto.getSeriesUrl())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_SERIES));
            foundPost.setSeries(foundSeries);
        }

        if (postRequestDto.getThumbnailFileId() != null) {
            var attachment = attachmentRepository
                    .findById(postRequestDto.getThumbnailFileId())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPost.setThumbnailImage(attachment);
        }

        return postMapper.EntityToPostDto(foundPost);
    }

    @Override
    @Transactional
    public void deletePost(String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        foundPost.setDeleted();
    }

    @Override
    public SliceDto<SimplePostDto> getBlogPosts(String blogId, String tag, Optional<UUID> cursorId, int size, boolean findPublicPostOnly) {
        List<Post> foundPosts;
        if (cursorId.isPresent()) {
            var foundCursorPost = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPosts = postRepository
                    .findByIdLessThanOrderByIdDesc(blogId, tag, foundCursorPost.getId(), foundCursorPost.getCreatedAt(), findPublicPostOnly, PageRequest.of(0, size + 1));
        } else {
            foundPosts = postRepository
                    .findAllByOrderByIdDesc(blogId, tag, findPublicPostOnly, PageRequest.of(0, size + 1));
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
    public SliceDto<SimplePostDto> getBloggerLikePosts(String blogId, Optional<UUID> cursorId, int size) {
        List<Like> foundLikes;
        if (cursorId.isPresent()) {
            var foundCursorLike = likeRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundLikes = likeRepository
                    .findByIdLessThanOrderByIdDesc(blogId, foundCursorLike.getId(), foundCursorLike.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            foundLikes = likeRepository
                    .findAllByOrderByIdDesc(blogId, PageRequest.of(0, size + 1));
        }
        var last = foundLikes.size() <= size;
        return SliceDto.<SimplePostDto>builder()
                .data(foundLikes.stream().limit(size).map(x -> postMapper.EntityToSimplePostDto(x.getPost())).toList())
                .size((int) foundLikes.stream().limit(size).count())
                .empty(foundLikes.isEmpty())
                .first(cursorId.isEmpty())
                .last(last)
                .cursorId(last ? null : foundLikes.stream().limit(size).skip(size - 1).findFirst().map(Like::getId).orElse(null))
                .build();
    }

    @Override
    public PostDto getPost(String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        var cachedHits = getPostHitsFromCache(foundPost);
        if(cachedHits != null) {
            return postMapper.EntityToPostDto(foundPost).withHits(cachedHits.intValue());
        } else {
            return postMapper.EntityToPostDto(foundPost);
        }
    }

    /**
     * 게시글 작성자의 Id를 조회합니다.
     *
     * @param postId 게시글 Id
     * @return 작성자 Id
     */
    @Override
    public UUID getPostAuthorId(UUID postId) {
        var foundPost = postRepository
                .findById(postId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        return foundPost.getAuthor().getId();
    }

    /**
     * 게시글 작성자의 Id를 조회합니다.
     *
     * @param blogId  조회를 원하는 게시글의 blogId
     * @param postUrl 조회를 원하는 게시글의 postUrl
     * @return 작성자 Id
     */
    @Override
    public UUID getPostAuthorId(String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrl(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        return foundPost.getAuthor().getId();
    }

    @Override
    @Transactional
    public void likePost(UUID actorAccountId, String blogId, String postUrl) {
        var account = accountRepository
                .findByIdAndDeletedIsFalse(actorAccountId)
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
                .findByIdAndDeletedIsFalse(actorAccountId)
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
    public boolean isLikePost(UUID accountId, String blogId, String postUrl) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));

        return likeRepository.existsByAccountIdAndPostId(accountId, foundPost.getId());
    }

    @Override
    @Transactional
    public CommentDto createComment(UUID authorAccountId, String blogId, String postUrl, CommentRequestDto commentRequestDto) {
        var authorAccount = accountRepository
                .findByIdAndDeletedIsFalse(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        var comment = commentMapper.toEntity(commentRequestDto);
        comment.setAuthor(authorAccount);
        comment.setPost(foundPost);
        var savedComment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CreateCommentEvent(this, savedComment));
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto createGuestComment(String blogId, String postUrl, GuestCommentRequestDto guestCommentRequestDto) {
        var foundPost = postRepository
                .findByBlogIdAndPostUrlAndDeletedIsFalse(blogId, postUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));

        // 비회원 계정 정보 생성
        var guestAccount = createGuestAccount(guestCommentRequestDto.getName(), guestCommentRequestDto.getPassword());

        // 비회원 댓글 정보 생성
        var comment = commentMapper.toEntity(guestCommentRequestDto);
        comment.setAuthor(guestAccount);
        comment.setPost(foundPost);
        var savedComment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CreateCommentEvent(this, savedComment));
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto createComment(UUID authorAccountId, UUID commentId, CommentRequestDto commentRequestDto) {
        var authorAccount = accountRepository
                .findByIdAndDeletedIsFalse(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        var foundComment = commentRepository
                .findByIdAndDeletedIsFalse(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));

        var comment = commentMapper.toEntity(commentRequestDto);
        comment.setAuthor(authorAccount);
        comment.setPost(foundComment.getPost());
        comment.setParent(foundComment);
        var savedComment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CreateCommentEvent(this, savedComment));
        return commentMapper.toDto(savedComment);
    }

    @Override
    @Transactional
    public CommentDto createGuestComment(UUID commentId, GuestCommentRequestDto guestCommentRequestDto) {
        var foundComment = commentRepository
                .findByIdAndDeletedIsFalse(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));

        // 비회원 계정 정보 생성
        var guestAccount = createGuestAccount(guestCommentRequestDto.getName(), guestCommentRequestDto.getPassword());

        // 비회원 댓글 정보 생성
        var comment = commentMapper.toEntity(guestCommentRequestDto);
        comment.setAuthor(guestAccount);
        comment.setPost(foundComment.getPost());
        comment.setParent(foundComment);
        var savedComment = commentRepository.save(comment);
        eventPublisher.publishEvent(new CreateCommentEvent(this, savedComment));
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
    public CommentDto getComment(UUID commentId) {
        var foundComment = commentRepository
                .findByIdAndDeletedIsFalse(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        return commentMapper.toDto(foundComment);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        var foundComment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        foundComment.setDeleted();
    }

    @Override
    public UUID getCommentAuthorId(UUID commentId) {
        var foundComment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        return foundComment.getAuthor().getId();
    }

    @Override
    public boolean matchCommentAuthorPassword(UUID commentId, String password) {
        var foundComment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_COMMENT));
        return passwordEncoder.matches(password, foundComment.getAuthor().getPassword());
    }

    @Override
    @Transactional
    public SeriesDto createSeries(UUID authorAccountId, SeriesRequestDto seriesRequestDto) {
        var foundAccount = accountRepository
                .findByIdAndDeletedIsFalse(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        if (foundAccount.isGuest()) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        seriesRepository
                .findByBlogIdAndSeriesUrl(foundAccount.getBlogId(), seriesRequestDto.getSeriesUrl())
                .ifPresent(x -> {
                    throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_SERIES_URL);
                });
        var series = seriesMapper.SeriesRequestDtoToEntity(seriesRequestDto);
        series.setAuthor(foundAccount);

        var savedSeries = seriesRepository.save(series);
        return seriesMapper.EntityToSeriesDto(savedSeries);
    }

    @Override
    @Transactional
    public SeriesDto modifySeries(String blogId, String seriesUrl, SeriesRequestDto seriesRequestDto) {
        var foundSeries = seriesRepository
                .findByBlogIdAndSeriesUrl(blogId, seriesUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_SERIES));
        if (!foundSeries.getSeriesUrl().equals(seriesRequestDto.getSeriesUrl())) {
            seriesRepository
                    .findByBlogIdAndSeriesUrl(blogId, seriesRequestDto.getSeriesUrl())
                    .ifPresent(x -> {
                        throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_SERIES_URL);
                    });
            foundSeries.setSeriesUrl(seriesRequestDto.getSeriesUrl());
        }

        foundSeries.setTitle(seriesRequestDto.getTitle());

        return seriesMapper.EntityToSeriesDto(foundSeries);
    }

    @Override
    @Transactional
    public void deleteSeries(String blogId, String seriesUrl) {
        var foundSeries = seriesRepository
                .findByBlogIdAndSeriesUrl(blogId, seriesUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_SERIES));
        seriesRepository.delete(foundSeries);
    }

    @Override
    public SeriesDto getSeries(String blogId, String seriesUrl) {
        var foundSeries = seriesRepository
                .findByBlogIdAndSeriesUrl(blogId, seriesUrl)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_SERIES));
        return seriesMapper.EntityToSeriesDto(foundSeries);
    }

    @Override
    public List<SimpleSeriesDto> getBlogSeries(String blogId) {
        return seriesRepository
                .findByBlogId(blogId)
                .stream()
                .map(seriesMapper::EntityToSimpleSeriesDto)
                .toList();
    }

    @Override
    public List<SimplePostDto> getBlogSeriesPosts(String blogId, String seriesUrl, boolean findPublicPostOnly) {
        return postRepository
                .findByBlogIdAndSeriesUrl(blogId, seriesUrl, findPublicPostOnly)
                .stream()
                .map(postMapper::EntityToSimplePostDto)
                .toList();
    }

    @Override
    public void increasePostHits(UUID postId) {
        var foundPost = postRepository
                .findById(postId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        foundPost.setHits(increaseHits(foundPost).intValue());
    }

    @Override
    @Transactional
    public void updatePostHitsFromCache() {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        hashOperations.keys(POST_HITS_KEY).forEach((postId) -> {
            var hits = hashOperations.get(POST_HITS_KEY, postId);
            hashOperations.delete(POST_HITS_KEY, postId);
            if (hits != null) {
                postRepository.updateHits(UUID.fromString(postId), Integer.valueOf(hits));
            }
        });
    }

    private Long getPostHitsFromCache(Post post) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        final String hashKey = post.getId().toString();
        if (!hashOperations.hasKey(POST_HITS_KEY, hashKey)) {
            return null;
        }

        return hashOperations.increment(POST_HITS_KEY, hashKey, 0L);
    }

    /**
     * 게시글의 조회수를 1 증가시킵니다.
     *
     * @param post 증가시킬 게시글
     * @return 증가된 후 게시글의 조회수
     */
    private Long increaseHits(Post post) {
        HashOperations<String, String, String> hashOperations = redisTemplate.opsForHash();
        final String hashKey = post.getId().toString();
        if (!hashOperations.hasKey(POST_HITS_KEY, hashKey)) {
            Optional.ofNullable(post.getHits()).ifPresent((hits) -> {
                hashOperations.put(POST_HITS_KEY, hashKey, hits.toString());
            });
        }
        return hashOperations.increment(POST_HITS_KEY, hashKey, 1L);
    }

    /**
     * 비회원 계정을 생성합니다.
     *
     * @param name     이름
     * @param password 비밀번호
     */
    private Account createGuestAccount(String name, String password) {
        var guestAccount = Account.createGuestAccount(name, passwordEncoder.encode(password));
        var savedAccount = accountRepository.save(guestAccount);
        var userRole = roleRepository.saveIfNotExist(RoleType.GUEST.getName());
        savedAccount.addRole(userRole);
        return savedAccount;
    }
}
