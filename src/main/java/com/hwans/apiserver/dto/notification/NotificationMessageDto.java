package com.hwans.apiserver.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
public class NotificationMessageDto implements Serializable {
    private String type;
    private String channel;
    private Object payload;
}
