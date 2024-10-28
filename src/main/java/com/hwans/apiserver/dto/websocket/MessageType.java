package com.hwans.apiserver.dto.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    SESSION_COUNT_CHANGED
}
