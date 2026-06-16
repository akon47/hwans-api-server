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

    // 관리자 통계용: 삭제되지 않은 전체 댓글 수
    long countByDeletedIsFalse();

    // 작성자 통계용: 해당 블로그 게시글에 달린 총 댓글 수
    @org.springframework.data.jpa.repository.Query("select count(x) from Comment as x where x.deleted = false and x.post.deleted = false and x.post.account.blogId = :blogId")
    long countByBlogId(@org.springframework.data.repository.query.Param("blogId") String blogId);
}
