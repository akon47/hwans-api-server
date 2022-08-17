package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.entity.blog.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Optional<Comment> findByIdAndDeletedIsFalse(UUID id);
}
