package com.hwans.apiserver.entity.account.role;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoleType {
    USER("ROLE_USER", "일반 사용자 권한"),
    SOCIAL("ROLE_SOCIAL", "소셜 사용자 권한");

    private final String name;
    private final String displayName;
}
