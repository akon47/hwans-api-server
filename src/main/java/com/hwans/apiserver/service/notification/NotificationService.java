package com.hwans.apiserver.service.notification;

import com.hwans.apiserver.dto.notification.NotificationMessageDto;

public interface NotificationService {
    void SendNotificationMessage(NotificationMessageDto notificationMessageDto);
}
