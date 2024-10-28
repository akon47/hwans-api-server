package com.hwans.apiserver.service.websocket;

import org.springframework.web.socket.WebSocketHandler;

/**
 * 웹소켓 서비스 인터페이스
 */
public interface WebSocketService {
    WebSocketHandler getWebSocketHandler();
}
