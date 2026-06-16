package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Bookmark;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByAccountIdAndPostId(UUID accountId, UUID postId);

    boolean existsByAccountIdAndPostId(UUID accountId, UUID postId);

    @Query("select x from Bookmark as x where x.post.deleted = false and x.account.id = :accountId order by x.createdAt desc, x.id desc")
    List<Bookmark> findAllByOrderByIdDesc(@Param("accountId") UUID accountId, Pageable page);

    @Query("select x from Bookmark as x where x.post.deleted = false and x.account.id = :accountId and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Bookmark> findByIdLessThanOrderByIdDesc(@Param("accountId") UUID accountId, @Param("id") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);
}
