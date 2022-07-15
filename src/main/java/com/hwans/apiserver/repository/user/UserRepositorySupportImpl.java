package com.hwans.apiserver.repository.user;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class UserRepositorySupportImpl implements UserRepositorySupport {

    @PersistenceContext
    private EntityManager entityManager;

}
