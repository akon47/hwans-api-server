package com.hwans.apiserver.common.security.token;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 개인 액세스 토큰으로 인증된 요청의 부가 정보. {@code Authentication.getDetails()} 에 담긴다.
 * (영속성 컨텍스트 밖에서도 쓸 수 있도록 엔티티가 아닌 값만 담는다)
 */
@Getter
@Builder
public class AccessTokenDetails implements Serializable {
    /**
     * 토큰 공개 Id
     */
    private final String tokenId;
    /**
     * 토큰 주인 계정 Id
     */
    private final UUID accountId;
    /**
     * 토큰 주인 계정 이메일
     */
    private final String accountEmail;
    /**
     * 토큰 권한 키 목록 (posts, social)
     */
    private final Set<String> scopes;
    /**
     * 토큰 주인 계정의 역할 이름 목록 (ROLE_USER 등)
     */
    private final Set<String> roles;
}
