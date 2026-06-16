package com.hwans.apiserver.service.follow;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.Follow;
import com.hwans.apiserver.entity.blog.Post;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.account.FollowRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 팔로우 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final AccountRepository accountRepository;
    private final PostRepository postRepository;
    private final NotificationService notificationService;
    private final AccountMapper accountMapper;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public void follow(UUID followerAccountId, String targetBlogId) {
        var target = getActiveNonGuestAccountByBlogId(targetBlogId);
        if (target.getId().equals(followerAccountId)) {
            // 자기 자신은 팔로우할 수 없다.
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        var follower = accountRepository
                .findByIdAndDeletedIsFalse(followerAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        if (followRepository.existsByFollowerIdAndFollowingId(followerAccountId, target.getId())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS);
        }

        followRepository.save(Follow.builder().follower(follower).following(target).build());
        notificationService.createFollowNotification(followerAccountId, target.getId());
    }

    @Override
    @Transactional
    public void unfollow(UUID followerAccountId, String targetBlogId) {
        var target = accountRepository
                .findByBlogId(targetBlogId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_BLOG));
        var follow = followRepository
                .findByFollowerIdAndFollowingId(followerAccountId, target.getId())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        followRepository.delete(follow);
    }

    @Override
    public boolean isFollowing(UUID followerAccountId, String targetBlogId) {
        var target = accountRepository
                .findByBlogId(targetBlogId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_BLOG));
        return followRepository.existsByFollowerIdAndFollowingId(followerAccountId, target.getId());
    }

    @Override
    public SliceDto<SimpleAccountDto> getFollowers(String blogId, Optional<UUID> cursorId, int size) {
        List<Follow> found;
        if (cursorId.isPresent()) {
            var cursor = followRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            found = followRepository.findFollowersByIdLessThanOrderByIdDesc(blogId, cursor.getId(), cursor.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            found = followRepository.findFollowersByOrderByIdDesc(blogId, PageRequest.of(0, size + 1));
        }
        return SliceDto.of(found, size, cursorId.isEmpty(), f -> accountMapper.toSimpleDto(f.getFollower()), Follow::getId);
    }

    @Override
    public SliceDto<SimpleAccountDto> getFollowings(String blogId, Optional<UUID> cursorId, int size) {
        List<Follow> found;
        if (cursorId.isPresent()) {
            var cursor = followRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            found = followRepository.findFollowingsByIdLessThanOrderByIdDesc(blogId, cursor.getId(), cursor.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            found = followRepository.findFollowingsByOrderByIdDesc(blogId, PageRequest.of(0, size + 1));
        }
        return SliceDto.of(found, size, cursorId.isEmpty(), f -> accountMapper.toSimpleDto(f.getFollowing()), Follow::getId);
    }

    @Override
    public SliceDto<SimplePostDto> getFollowingPosts(UUID followerAccountId, Optional<UUID> cursorId, int size) {
        List<Post> found;
        if (cursorId.isPresent()) {
            var cursor = postRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            found = postRepository.findFollowingPostsByCursorLessThanOrderByCreatedAtDesc(followerAccountId, cursor.getId(), cursor.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            found = postRepository.findFollowingPostsByOrderByCreatedAtDesc(followerAccountId, PageRequest.of(0, size + 1));
        }
        return SliceDto.of(found, size, cursorId.isEmpty(), postMapper::EntityToSimplePostDto, Post::getId);
    }

    @Override
    public long getFollowerCount(UUID accountId) {
        return followRepository.countByFollowingId(accountId);
    }

    @Override
    public long getFollowingCount(UUID accountId) {
        return followRepository.countByFollowerId(accountId);
    }

    private Account getActiveNonGuestAccountByBlogId(String blogId) {
        var account = accountRepository
                .findByBlogId(blogId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_BLOG));
        if (account.isDeleted() || account.isGuest()) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND_BLOG);
        }
        return account;
    }
}
