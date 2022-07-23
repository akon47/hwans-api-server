package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.authentication.AccountRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRefreshTokenRepository extends JpaRepository<AccountRefreshToken, Long> {
}
