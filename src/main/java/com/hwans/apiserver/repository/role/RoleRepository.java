package com.hwans.apiserver.repository.role;

import com.hwans.apiserver.entity.account.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 사용자 계정 역할 Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String>, RoleRepositorySupport {

}
