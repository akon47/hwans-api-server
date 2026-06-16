package com.hwans.apiserver.service.websocket;

import com.hwans.apiserver.dto.notification.NotificationDto;
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

    /**
     * 특정 사용자가 연결한 모든 세션에 새 알림을 실시간으로 전송한다.
     * 해당 사용자의 연결된 세션이 없으면 아무 일도 하지 않는다.
     *
     * @param accountEmail 알림을 받을 사용자의 이메일(웹소켓 인증 시 사용한 식별자)
     * @param notification 전송할 알림
     */
    void sendNotification(String accountEmail, NotificationDto notification);
}
