package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Like;
import com.hwans.apiserver.entity.blog.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByAccountIdAndPostId(UUID accountId, UUID postId);

    boolean existsByAccountIdAndPostId(UUID accountId, UUID postId);

    @Query("select x from Like as x where x.post.deleted = false and x.account.blogId = :blogId order by x.createdAt desc, x.id desc")
    List<Like> findAllByOrderByIdDesc(@Param("blogId") String blogId, Pageable page);

    @Query("select x from Like as x where x.post.deleted = false and x.account.blogId = :blogId and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Like> findByIdLessThanOrderByIdDesc(@Param("blogId") String blogId, @Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);
}
