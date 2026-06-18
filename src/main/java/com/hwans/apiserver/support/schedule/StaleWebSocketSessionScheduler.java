package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 네트워크 전환(예: Wi-Fi -> LTE)이나 비정상 종료로 끊겼지만 close 프레임이 도달하지 않아
 * 서버에 남아있는 유령 웹소켓 세션을 주기적으로 수거하는 스케줄러
 */
@RequiredArgsConstructor
@Component
public class StaleWebSocketSessionScheduler {
    private final WebSocketService webSocketService;

    // 30초마다 마지막 생존 신호가 끊긴 죽은 세션을 수거한다.
    @Scheduled(cron = "*/30 * * * * ?")
    private void reapStaleSessions() {
        webSocketService.reapStaleSessions();
    }
}
