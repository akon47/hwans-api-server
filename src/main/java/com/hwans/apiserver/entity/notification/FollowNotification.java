package com.hwans.apiserver.entity.notification;

import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@DiscriminatorValue(value = NotificationType.Values.FOLLOW)
@Table(name = "tb_follow_notification")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class FollowNotification extends Notification {
    // 새로 팔로우한 사용자(알림을 발생시킨 주체)
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private Account follower;

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.FOLLOW;
    }
}
