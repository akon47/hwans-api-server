package com.hwans.apiserver.common;

public final class Constants {
    public static final String API_PREFIX = "/api";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String REFRESH_TOKEN_HEADER = "X-Auth-Refresh-Token";
    public static final Long ACCESS_TOKEN_EXPIRES_TIME = 60 * 10 * 1000L;
    public static final Long REFRESH_TOKEN_EXPIRES_TIME = 60 * 60 * 24 * 30 * 1000L;
    public static final Long EMAIL_VERIFY_CODE_EXPIRES_TIME = 60 * 3 * 1000L;
}
