package com.hwans.apiserver.support.schedule;

import com.hwans.apiserver.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 같은 글을 읽는 사람들의 커서 위치를 약 100ms 마다 묶어 보낸다.
 * 세션마다 움직일 때마다 바로 중계하면 (읽는 사람 수)² 로 메시지가 늘어나므로, 글 단위로 모아 한 번에 보낸다.
 */
@Component
@RequiredArgsConstructor
public class CursorFlushScheduler {
    private final WebSocketService webSocketService;

    @Scheduled(fixedDelay = 100)
    public void flushCursors() {
        webSocketService.flushCursors();
        // 읽는 위치는 커서보다 느리게 바뀌므로 서비스 쪽에서 더 긴 간격으로 걸러 보낸다.
        webSocketService.flushReadPositions();
    }
}
