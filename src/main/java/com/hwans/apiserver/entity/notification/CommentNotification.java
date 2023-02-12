package com.hwans.apiserver.entity.notification;

import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.blog.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

@Entity
@DiscriminatorValue(value = NotificationType.Values.COMMENT)
@Table(name = "tb_comment_notification")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class CommentNotification extends Notification {
    @OneToOne
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.COMMENT;
    }
}
