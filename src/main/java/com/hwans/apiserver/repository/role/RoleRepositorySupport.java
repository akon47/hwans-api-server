package com.hwans.apiserver.repository.role;

import com.hwans.apiserver.entity.account.Role;

public interface RoleRepositorySupport {
    Role saveIfNotExist(String name);
}
