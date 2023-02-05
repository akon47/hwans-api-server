package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class AccountRepositorySupportImpl implements AccountRepositorySupport {
    @PersistenceContext
    private EntityManager entityManager;
}
