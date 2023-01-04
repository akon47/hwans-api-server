package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.entity.blog.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 댓글 Repository
 */
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    /**
     * @param id 댓글 Id로 댓글을 찾아 반환한다.
     * @return 존재하는 경우 댓글 엔티티
     */
    Optional<Comment> findByIdAndDeletedIsFalse(UUID id);
}
