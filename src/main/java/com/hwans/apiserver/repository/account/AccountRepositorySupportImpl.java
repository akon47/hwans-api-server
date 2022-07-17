package com.hwans.apiserver.repository.account;

import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class AccountRepositorySupportImpl implements AccountRepositorySupport {

    @PersistenceContext
    private EntityManager entityManager;

}
