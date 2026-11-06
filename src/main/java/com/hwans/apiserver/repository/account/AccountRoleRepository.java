package com.hwans.apiserver.repository.account;

import com.hwans.apiserver.entity.account.role.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AccountRoleRepository extends JpaRepository<AccountRole, UUID> {

    java.util.Optional<AccountRole> findByAccount_IdAndRole_Name(UUID accountId, String roleName);
}
