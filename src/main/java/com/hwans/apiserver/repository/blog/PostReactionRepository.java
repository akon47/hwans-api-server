package com.hwans.apiserver.repository.blog;

import com.hwans.apiserver.entity.blog.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostReactionRepository extends JpaRepository<PostReaction, UUID> {
    Optional<PostReaction> findByAccountIdAndPostIdAndReactionKey(UUID accountId, UUID postId, String reactionKey);

    List<PostReaction> findByPostId(UUID postId);
}
