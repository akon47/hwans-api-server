package com.hwans.apiserver.service;

import com.hwans.apiserver.dto.notification.NotificationMessageDto;

public interface NotificationService {
    void SendNotificationMessage(NotificationMessageDto notificationMessageDto);
}
