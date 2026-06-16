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
 * 게시글 북마크(나중에 읽기) 엔티티
 */
@Entity
@Table(name = "tb_bookmark", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "account_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Bookmark extends BaseEntity {
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
}
