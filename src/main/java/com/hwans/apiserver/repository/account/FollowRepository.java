package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Follow;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId);

    // 특정 블로그(사용자)를 팔로우하는 사람 수
    long countByFollowingId(UUID followingId);

    // 특정 블로그(사용자)가 팔로우하는 사람 수
    long countByFollowerId(UUID followerId);

    // 특정 블로그(사용자)를 팔로우하는 사람 목록 (최신순)
    @Query("select f from Follow as f where f.following.blogId = :blogId and f.follower.deleted = false order by f.createdAt desc, f.id desc")
    List<Follow> findFollowersByOrderByIdDesc(@Param("blogId") String blogId, Pageable page);

    @Query("select f from Follow as f where f.following.blogId = :blogId and f.follower.deleted = false and ((f.createdAt < :createdAt and f.id < :id) or (f.createdAt < :createdAt)) order by f.createdAt desc, f.id desc")
    List<Follow> findFollowersByIdLessThanOrderByIdDesc(@Param("blogId") String blogId, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);

    // 특정 블로그(사용자)가 팔로우하는 사람 목록 (최신순)
    @Query("select f from Follow as f where f.follower.blogId = :blogId and f.following.deleted = false order by f.createdAt desc, f.id desc")
    List<Follow> findFollowingsByOrderByIdDesc(@Param("blogId") String blogId, Pageable page);

    @Query("select f from Follow as f where f.follower.blogId = :blogId and f.following.deleted = false and ((f.createdAt < :createdAt and f.id < :id) or (f.createdAt < :createdAt)) order by f.createdAt desc, f.id desc")
    List<Follow> findFollowingsByIdLessThanOrderByIdDesc(@Param("blogId") String blogId, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);
}
