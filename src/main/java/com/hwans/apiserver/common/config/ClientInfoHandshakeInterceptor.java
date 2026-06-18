package com.hwans.apiserver.common.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 웹소켓 핸드셰이크 시점에 클라이언트 IP와 User-Agent를 추출하여 세션 attributes에 저장한다.
 * (웹소켓 세션 자체는 클라이언트 IP/User-Agent를 보관하지 않으므로 핸드셰이크에서 미리 캡처한다.)
 * attributes에 담긴 값은 이후 {@code session.getAttributes()}로 접근할 수 있다.
 */
public class ClientInfoHandshakeInterceptor implements HandshakeInterceptor {
    // 세션 attributes 키
    public static final String CLIENT_IP = "clientIp";
    public static final String USER_AGENT = "userAgent";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        attributes.put(CLIENT_IP, resolveClientIp(request));
        attributes.put(USER_AGENT, resolveUserAgent(request));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 후처리할 작업 없음
    }

    /**
     * 프록시 뒤에 있을 경우를 고려하여 X-Forwarded-For(첫 IP) -> X-Real-IP -> 소켓 원격 주소 순으로 클라이언트 IP를 찾는다.
     */
    private String resolveClientIp(ServerHttpRequest request) {
        var forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // "client, proxy1, proxy2" 형태이므로 첫 번째 IP가 실제 클라이언트이다.
            return forwardedFor.split(",")[0].trim();
        }
        var realIp = request.getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        var remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String resolveUserAgent(ServerHttpRequest request) {
        var userAgent = request.getHeaders().getFirst("User-Agent");
        return (userAgent == null || userAgent.isBlank()) ? "unknown" : userAgent;
    }
}
