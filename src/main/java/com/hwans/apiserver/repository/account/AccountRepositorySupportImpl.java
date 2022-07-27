package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
public class AccountRepositorySupportImpl implements AccountRepositorySupport {
    @PersistenceContext
    private EntityManager entityManager;
}
