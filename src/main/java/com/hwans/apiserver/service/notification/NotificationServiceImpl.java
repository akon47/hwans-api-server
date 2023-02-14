package com.hwans.apiserver.service.notification;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.blog.CommentDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.entity.notification.CommentNotification;
import com.hwans.apiserver.entity.notification.Notification;
import com.hwans.apiserver.mapper.NotificationMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        var last = foundPosts.size() <= size;
        return SliceDto.<NotificationDto>builder()
                .data(foundPosts.stream().limit(size).map(notificationMapper::EntityToNotificationDto).toList())
                .size((int) foundPosts.stream().limit(size).count())
                .empty(foundPosts.isEmpty())
                .first(cursorId.isEmpty())
                .last(last)
                .cursorId(last ? null : foundPosts.stream().limit(size).skip(size - 1).findFirst().map(Notification::getId).orElse(null))
                .build();
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
        return notificationMapper.EntityToNotificationDto(savedNotification);
    }
}
