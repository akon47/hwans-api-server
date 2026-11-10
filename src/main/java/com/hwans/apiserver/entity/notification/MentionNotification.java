package com.hwans.apiserver.entity.notification;

import com.hwans.apiserver.entity.blog.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.persistence.*;

/**
 * 댓글에서 언급(@블로그Id)된 사람에게 가는 알림.
 * 한 댓글이 여러 사람을 언급할 수 있으므로 댓글과는 다대일이다.
 */
@Entity
@DiscriminatorValue(value = NotificationType.Values.MENTION)
@Table(name = "tb_mention_notification")
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class MentionNotification extends Notification {
    @ManyToOne
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.MENTION;
    }
}
