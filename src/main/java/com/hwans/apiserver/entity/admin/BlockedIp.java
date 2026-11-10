package com.hwans.apiserver.entity.admin;

import com.hwans.apiserver.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;

/**
 * 차단한 IP. 차단된 IP 는 손님 댓글과 신고를 할 수 없다. (읽기는 막지 않는다)
 */
@Entity
@Table(name = "tb_blocked_ip", uniqueConstraints = @UniqueConstraint(name = "uk_blocked_ip", columnNames = "ip"))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BlockedIp extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 64, nullable = false)
    private String ip;
    @Column(length = 200)
    private String reason;
    // 차단한 관리자 이메일
    @Column(length = 320)
    private String createdBy;
}
