package com.hwans.apiserver.repository.blog.tag;

import com.hwans.apiserver.entity.blog.Tag;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class TagRepositorySupportImpl implements TagRepositorySupport {
    @PersistenceContext
    private EntityManager entityManager;
}
