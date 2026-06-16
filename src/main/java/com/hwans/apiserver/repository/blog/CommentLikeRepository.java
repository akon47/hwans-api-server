package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    Optional<CommentLike> findByAccountIdAndCommentId(UUID accountId, UUID commentId);

    boolean existsByAccountIdAndCommentId(UUID accountId, UUID commentId);
}
