package com.hwans.apiserver.service.notification;

import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.notification.NotificationDto;

import java.util.Optional;
import java.util.UUID;

/**
 * 알림 서비스 인터페이스
 */
public interface NotificationService {
    /**
     * 알림 목록을 조회한다.
     *
     * @param accountId 조회할 대상의 계정 Id
     * @param cursorId 페이징 조회를 위한 기준 cursorId
     * @param size 조회를 원하는 최대 size
     * @param findUnreadNotificationOnly 읽지 않은 알림만 조회할지 여부
     * @return 조회된 알림 목록 (페이징)
     */
    SliceDto<NotificationDto> getNotifications(UUID accountId, Optional<UUID> cursorId, int size, boolean findUnreadNotificationOnly);

    /**
     * 알림을 조회한다.
     *
     * @param accountId 조회할 대상의 계정 Id
     * @param notificationId 조회를 원하는 알림 Id
     * @return 조회된 알림
     */
    NotificationDto getNotification(UUID accountId, UUID notificationId);

    /**
     * 알림을 삭제합니다.
     *
     * @param notificationId 삭제를 원하는 알림 Id
     */
    void deleteNotification(UUID accountId, UUID notificationId);

    /**
     * 새 댓글 알림을 생성합니다.
     *
     * @param comment 관련 댓글
     * @return 생성된 알림 데이터 모델
     */
    NotificationDto createCommentNotification(CommentDto comment);
}
