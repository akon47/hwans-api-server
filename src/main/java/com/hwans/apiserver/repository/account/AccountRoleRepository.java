package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.AccountRole;
import com.hwans.apiserver.entity.account.AccountRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRoleRepository extends JpaRepository<AccountRole, AccountRoleId>, AccountRepositorySupport {

}
