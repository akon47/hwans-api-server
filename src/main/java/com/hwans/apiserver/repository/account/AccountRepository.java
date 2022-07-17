package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String>, AccountRepositorySupport {

}
