package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}
