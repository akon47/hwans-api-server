package com.hwans.apiserver.entity.notification;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "notification_type", discriminatorType = DiscriminatorType.STRING)
@Table(name = "tb_notification")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public abstract class Notification extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(nullable = false)
    private boolean deleted;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Column(nullable = true)
    private LocalDateTime readAt;

    public abstract NotificationType getNotificationType();

    public Account getReceiver() {
        return this.account;
    }
}
