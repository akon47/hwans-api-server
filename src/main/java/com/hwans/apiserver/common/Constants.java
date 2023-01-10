package com.hwans.apiserver.common;

/**
 * 공통적으로 사용할 상수 필드
 */
public final class Constants {
    /**
     * Controller 엔드포인트 앞에 붙일 Prefix
     */
    public static final String API_PREFIX = "/api";
    /**
     * 인증 토큰을 의미하는 HTTP 헤더 이름
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    /**
     * 리프레시 인증 토큰을 의미하는 HTTP 헤더 이름
     */
    public static final String REFRESH_TOKEN_HEADER = "X-Auth-Refresh-Token";
    /**
     * Access Token의 만료 시간 (30분)
     */
    public static final Long ACCESS_TOKEN_EXPIRES_TIME = 60 * 30 * 1000L;
    /**
     * Refresh Token의 만료 시간 (30일)
     */
    public static final Long REFRESH_TOKEN_EXPIRES_TIME = 60 * 60 * 24 * 30 * 1000L;
    /**
     * Register Token의 만료 시간 (1일)
     */
    public static final Long REGISTER_TOKEN_EXPIRES_TIME = 60 * 60 * 24 * 1000L;
    /**
     * 이메일 인증 코드의 만료 시간 (3분)
     */
    public static final Long EMAIL_VERIFY_CODE_EXPIRES_TIME = 60 * 3 * 1000L;
    /**
     * 비밀번호 초기화 Token의 만료 시간 (3분)
     */
    public static final Long PASSWORD_RESET_TOKEN_EXPIRES_TIME = 60 * 3 * 1000L;

    /**
     * 최대 첨부파일의 크기
     */
    public static final Long MAX_ATTACHMENT_FILE_SIZE = 1024 * 1024 * 100L; // 100MB
}
