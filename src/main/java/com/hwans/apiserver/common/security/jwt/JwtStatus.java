package com.hwans.apiserver.common.security.jwt;

/**
 * Jwt 토큰의 상태 값
 */
public enum JwtStatus {
    /**
     * 유효한 토큰
     */
    ACCESS,

    /**
     * 만료된 토큰
     */
    EXPIRED,

    /**
     * 잘못된 토큰
     */
    DENIED
}
