package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String>, AccountRepositorySupport {
    Optional<Account> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByBlogId(String blogId);
}
