package com.hwans.apiserver.common.security;

import javax.servlet.http.HttpServletRequest;

/**
 * 프록시 뒤에 있을 경우를 고려하여 클라이언트 IP 를 찾는다.
 * (웹소켓의 {@code ClientInfoHandshakeInterceptor} 와 같은 순서: X-Forwarded-For 첫 IP → X-Real-IP → 소켓 원격 주소)
 */
public final class ClientIp {
    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        var forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        var realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        var remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
