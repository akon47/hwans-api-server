package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.notification.CommentNotificationDto;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.entity.notification.CommentNotification;
import com.hwans.apiserver.entity.notification.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

/**
 * 알림 엔티티와 알림 데이터 모델 사이의 변환을 제공한다.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @SubclassMapping(target = CommentNotificationDto.class, source = CommentNotification.class)
    NotificationDto EntityToNotificationDto(Notification notification);
}
