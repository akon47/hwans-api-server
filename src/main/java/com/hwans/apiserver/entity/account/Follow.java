package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

/**
 * 팔로우 관계 엔티티.
 * follower 가 following 을 팔로우한 상태를 의미한다.
 */
@Entity
@Table(name = "tb_follow", uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "following_id"}))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Follow extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    // 팔로우 하는 사용자
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false)
    private Account follower;
    // 팔로우 당하는(대상) 사용자
    @ManyToOne
    @JoinColumn(name = "following_id", nullable = false)
    private Account following;
}
