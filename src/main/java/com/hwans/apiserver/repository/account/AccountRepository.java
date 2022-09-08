package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID>, AccountRepositorySupport {
    Optional<Account> findByEmailAndDeletedIsFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndDeletedIsFalse(String email);

    boolean existsByBlogId(String blogId);

    Optional<Account> findByBlogId(String blogId);
}
