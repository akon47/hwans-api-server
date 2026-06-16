package com.hwans.apiserver.entity.blog;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

/**
 * 댓글 좋아요 엔티티
 */
@Entity
@Table(name = "tb_comment_like", uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "account_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentLike extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
