package com.hwans.apiserver.service.notification;

import com.hwans.apiserver.dto.notification.NotificationMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final SimpMessageSendingOperations simpMessageSendingOperations;

    @Override
    public void SendNotificationMessage(NotificationMessageDto notificationMessageDto) {
        simpMessageSendingOperations.convertAndSend("/" + notificationMessageDto.getChannel(), notificationMessageDto);
    }
}
