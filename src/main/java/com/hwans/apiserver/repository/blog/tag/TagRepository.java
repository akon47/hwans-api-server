package com.hwans.apiserver.repository.blog.tag;

import com.hwans.apiserver.entity.blog.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID>, TagRepositorySupport {
    Optional<Tag> findByName(String name);

    // 그 회원이 글에 붙인 태그 이름 (많이 쓴 순). AI 가 태그를 고를 때 기존 표기를 따르게 한다.
    @Query("select t.name from PostTag as pt join pt.tag as t where pt.post.account.id = :accountId and pt.post.deleted = false group by t.name order by count(pt) desc, t.name asc")
    List<String> findTagNamesByAccountId(@Param("accountId") UUID accountId, Pageable page);
}
