package com.hwans.apiserver.repository.blog.tag;

import com.hwans.apiserver.entity.blog.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID>, TagRepositorySupport {
    Optional<Tag> findByName(String name);
}
