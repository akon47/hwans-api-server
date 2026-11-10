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
 * 관리자 활동 기록 엔티티 (누가 언제 무엇을 했는지. 지우거나 고치지 않고 쌓기만 한다)
 */
@Entity
@Table(name = "tb_admin_audit_log", indexes = @Index(name = "idx_admin_audit_log_created_at", columnList = "createdAt"))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminAuditLog extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    // 행동한 관리자 (계정이 지워져도 기록은 남도록 문자열로 보관한다)
    @Column(length = 320)
    private String actorEmail;
    @Column(length = 64)
    private String actorName;
    // 행동 종류 (예: ACCOUNT_DISABLE, POST_DELETE)
    @Column(length = 40, nullable = false)
    private String action;
    // 대상 종류와 Id (예: ACCOUNT / 계정 Id)
    @Column(length = 20)
    private String targetType;
    @Column(length = 64)
    private String targetId;
    // 사람이 읽을 요약 (예: 회원 정지: 홍길동 <a@b.c>)
    @Column(length = 500)
    private String summary;
}
