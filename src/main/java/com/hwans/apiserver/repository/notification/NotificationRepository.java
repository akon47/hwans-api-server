package com.hwans.apiserver.repository.notification;

import com.hwans.apiserver.entity.notification.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    /**
     * 해당 계정의 모든 알림을 삭제된 상태로 변경합니다.
     *
     * @param accountId 관련 계정 Id
     * @return 변경된 알림의 수
     */
    @Modifying
    @Query("update Notification x set x.deleted = True where x.account.id = :accountId and x.deleted != True")
    int setDeletedAllByAccountId(@Param("uuid") UUID accountId);

    Optional<Notification> findByAccountIdAndId(UUID accountId, UUID id);

    @Query("select x from Notification as x where x.account.id = :accountId and x.deleted = false and (:findUnreadNotificationOnly is false or x.readAt is not null) order by x.createdAt desc, x.id desc")
    List<Notification> findAllByOrderByIdDesc(@Param("uuid") UUID accountId, boolean findUnreadNotificationOnly, Pageable page);

    @Query("select x from Notification as x where x.account.id = :accountId and x.deleted = false and (:findUnreadNotificationOnly is false or x.readAt is not null) and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<Notification> findByIdLessThanOrderByIdDesc(@Param("uuid") UUID accountId, @Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, boolean findUnreadNotificationOnly, Pageable page);
}
