package com.hwans.apiserver.service.notification;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.entity.notification.CommentNotification;
import com.hwans.apiserver.entity.notification.FollowNotification;
import com.hwans.apiserver.entity.notification.Notification;
import com.hwans.apiserver.mapper.NotificationMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.notification.NotificationRepository;
import com.hwans.apiserver.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 알림 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final CommentRepository commentRepository;
    private final NotificationMapper notificationMapper;
    private final WebSocketService webSocketService;

    /**
     * 알림 목록을 조회한다.
     *
     * @param accountId                  조회할 대상의 계정 Id
     * @param cursorId                   페이징 조회를 위한 기준 cursorId
     * @param size                       조회를 원하는 최대 size
     * @param findUnreadNotificationOnly 읽은 알림도 포함하여 조회할지 여부
     * @return 조회된 알림 목록(페이징)
     */
    @Override
    public SliceDto<NotificationDto> getNotifications(UUID accountId, Optional<UUID> cursorId, int size, boolean findUnreadNotificationOnly) {
        List<Notification> foundPosts;
        if (cursorId.isPresent()) {
            var foundCursorPost = notificationRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundPosts = notificationRepository
                    .findByIdLessThanOrderByIdDesc(accountId, foundCursorPost.getId(), foundCursorPost.getCreatedAt(), findUnreadNotificationOnly, PageRequest.of(0, size + 1));
        } else {
            foundPosts = notificationRepository
                    .findAllByOrderByIdDesc(accountId, findUnreadNotificationOnly, PageRequest.of(0, size + 1));
        }
        return SliceDto.of(foundPosts, size, cursorId.isEmpty(), notificationMapper::EntityToNotificationDto, Notification::getId);
    }

    /**
     * 알림을 조회한다.
     *
     * @param accountId      조회할 대상의 계정 Id
     * @param notificationId 조회를 원하는 알림 Id
     * @return 조회된 알림
     */
    @Override
    public NotificationDto getNotification(UUID accountId, UUID notificationId) {
        var foundNotification = notificationRepository
                .findByAccountIdAndId(accountId, notificationId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_NOTIFICATION));
        return notificationMapper.EntityToNotificationDto(foundNotification);
    }

    @Override
    @Transactional
    public NotificationDto markNotificationAsRead(UUID accountId, UUID notificationId) {
        var foundNotification = notificationRepository
                .findByAccountIdAndId(accountId, notificationId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_NOTIFICATION));
        foundNotification.setReadAtNow();
        var savedNotification = notificationRepository.save(foundNotification);
        return notificationMapper.EntityToNotificationDto(savedNotification);
    }

    /**
     * 모든 알림을 삭제합니다.
     *
     * @param accountId 삭제할 대상의 계정 Id
     */
    @Override
    @Transactional
    public void deleteNotifications(UUID accountId) {
        notificationRepository.setDeletedAllByAccountId(accountId);
    }

    /**
     * 알림을 삭제합니다.
     *
     * @param accountId      조회할 대상의 계정 Id
     * @param notificationId 알림 Id
     */
    @Override
    @Transactional
    public void deleteNotification(UUID accountId, UUID notificationId) {
        var foundNotification = notificationRepository
                .findByAccountIdAndId(accountId, notificationId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_NOTIFICATION));
        foundNotification.setDeleted();
        notificationRepository.save(foundNotification);
    }

    /**
     * 새 댓글 알림을 생성합니다.
     *
     * @param comment 관련 댓글
     * @return 생성된 알림 데이터 모델
     */
    @Override
    @Transactional
    public NotificationDto createCommentNotification(CommentDto comment) {
        var foundComment = commentRepository
                .findById(comment.getId())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        var receiverAccountId = foundComment.getPost().getAuthor().getId();
        var foundAccount = accountRepository
                .findByIdAndDeletedIsFalse(receiverAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        if (foundAccount.isGuest()) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND);
        }

        var senderAccountId = foundComment.getAuthor().getId();
        if (receiverAccountId.equals(senderAccountId)) {
            return null;
        }

        var notification = CommentNotification.builder()
                .account(foundAccount)
                .comment(foundComment)
                .build();

        var savedNotification = notificationRepository.save(notification);
        var notificationDto = notificationMapper.EntityToNotificationDto(savedNotification);

        // 트랜잭션 커밋 이후에 실시간 알림을 전송하여, 롤백 시 유령 알림이 전달되는 것을 방지한다.
        pushNotificationAfterCommit(foundAccount.getEmail(), notificationDto);
        return notificationDto;
    }

    /**
     * 새 팔로워 알림을 생성합니다.
     *
     * @param followerAccountId  팔로우를 한 사용자 Id
     * @param followingAccountId 팔로우를 당한(알림을 받을) 사용자 Id
     * @return 생성된 알림 데이터 모델
     */
    @Override
    @Transactional
    public NotificationDto createFollowNotification(UUID followerAccountId, UUID followingAccountId) {
        if (followerAccountId.equals(followingAccountId)) {
            return null;
        }

        var receiverAccount = accountRepository
                .findByIdAndDeletedIsFalse(followingAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        if (receiverAccount.isGuest()) {
            return null;
        }

        var followerAccount = accountRepository
                .findByIdAndDeletedIsFalse(followerAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        var notification = FollowNotification.builder()
                .account(receiverAccount)
                .follower(followerAccount)
                .build();

        var savedNotification = notificationRepository.save(notification);
        var notificationDto = notificationMapper.EntityToNotificationDto(savedNotification);

        pushNotificationAfterCommit(receiverAccount.getEmail(), notificationDto);
        return notificationDto;
    }

    /**
     * 현재 트랜잭션이 성공적으로 커밋된 뒤 수신자의 웹소켓 세션으로 알림을 전송한다.
     * 활성 트랜잭션이 없으면 즉시 전송한다.
     */
    private void pushNotificationAfterCommit(String accountEmail, NotificationDto notification) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    webSocketService.sendNotification(accountEmail, notification);
                }
            });
        } else {
            webSocketService.sendNotification(accountEmail, notification);
        }
    }
}
