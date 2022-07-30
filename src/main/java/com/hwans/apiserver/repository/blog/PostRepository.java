package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
}
