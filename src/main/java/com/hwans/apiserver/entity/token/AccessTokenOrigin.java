package com.hwans.apiserver.entity.token;

/**
 * 액세스 토큰이 어떻게 발급되었는지
 */
public enum AccessTokenOrigin {
    /**
     * 웹에서 사람이 직접 발급
     */
    CONSOLE,
    /**
     * 기기 로그인 (에이전트가 요청하고 사람이 웹에서 승인)
     */
    DEVICE
}
