package com.hwans.apiserver.service.follow;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

/**
 * 팔로우 서비스 인터페이스
 */
public interface FollowService {
    /**
     * 대상 블로그(사용자)를 팔로우한다.
     *
     * @param followerAccountId 팔로우를 하는 사용자 Id
     * @param targetBlogId      팔로우할 대상 블로그 Id
     */
    void follow(UUID followerAccountId, String targetBlogId);

    /**
     * 대상 블로그(사용자)의 팔로우를 취소한다.
     *
     * @param followerAccountId 팔로우를 취소하는 사용자 Id
     * @param targetBlogId      팔로우 취소할 대상 블로그 Id
     */
    void unfollow(UUID followerAccountId, String targetBlogId);

    /**
     * 대상 블로그(사용자)를 팔로우하고 있는지 여부를 반환한다.
     *
     * @param followerAccountId 확인할 사용자 Id
     * @param targetBlogId      대상 블로그 Id
     * @return 팔로우 중이면 true
     */
    boolean isFollowing(UUID followerAccountId, String targetBlogId);

    /**
     * 대상 블로그(사용자)를 팔로우하는 사용자 목록을 조회한다.
     */
    SliceDto<SimpleAccountDto> getFollowers(String blogId, Optional<UUID> cursorId, int size);

    /**
     * 대상 블로그(사용자)가 팔로우하는 사용자 목록을 조회한다.
     */
    SliceDto<SimpleAccountDto> getFollowings(String blogId, Optional<UUID> cursorId, int size);

    /**
     * 내가 팔로우하는 사용자들의 공개 게시글 피드를 조회한다.
     */
    SliceDto<SimplePostDto> getFollowingPosts(UUID followerAccountId, Optional<UUID> cursorId, int size);

    /**
     * 대상 블로그(사용자)의 팔로워 수를 반환한다.
     */
    long getFollowerCount(UUID accountId);

    /**
     * 대상 블로그(사용자)가 팔로우하는 사용자 수를 반환한다.
     */
    long getFollowingCount(UUID accountId);
}
