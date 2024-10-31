package com.hwans.apiserver.dto.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 웹소켓 이벤트 유형
 */
@Getter
@RequiredArgsConstructor
public enum MessageType {
    SESSION_COUNT_CHANGED
}
