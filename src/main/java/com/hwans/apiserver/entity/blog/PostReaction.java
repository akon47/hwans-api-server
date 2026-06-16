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
 * 게시글 이모지 반응 엔티티.
 * 실제 저장값은 이모지 문자가 아니라 ASCII 반응 키(예: thumbsup)이다.
 * (DB charset 이 utf8mb4 가 아니어도 안전하게 저장하기 위함)
 */
@Entity
@Table(name = "tb_post_reaction", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "account_id", "reaction_key"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostReaction extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
    @Column(name = "reaction_key", length = 32, nullable = false)
    private String reactionKey;
}
