package com.hwans.apiserver.service.websocket;

import org.springframework.web.socket.WebSocketHandler;

import java.util.Map;

/**
 * 웹소켓 서비스 인터페이스
 */
public interface WebSocketService {
    WebSocketHandler getWebSocketHandler();

    /**
     * 현재 한 명 이상이 보고 있는 게시글들의 시청자 수를 반환한다.
     *
     * @return 게시글 Id(String) -> 시청자 수
     */
    Map<String, Integer> getActiveViewerCounts();
}
