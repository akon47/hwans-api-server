package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Post;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Optional<Post> findByBlogIdAndPostUrl(String blogId, String postUrl);

    Optional<Post> findByBlogIdAndPostUrlAndDeletedIsFalse(String blogId, String postUrl);

    @Query("select x from Post as x where x.deleted = false order by x.createdAt desc, x.id desc")
    List<Post> findAllByOrderByIdDesc(Pageable page);

    @Query("select x from Post as x where x.deleted = false and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Post> findByIdLessThanOrderByIdDesc(@Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);
}
