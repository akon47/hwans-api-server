package com.hwans.apiserver.entity.admin;

import com.hwans.apiserver.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 글·댓글 신고 엔티티
 */
@Entity
@Table(name = "tb_report", indexes = {
        @Index(name = "idx_report_status_created_at", columnList = "status, createdAt"),
        @Index(name = "idx_report_target", columnList = "targetType, targetId")})
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Report extends BaseEntity {
    public enum TargetType { POST, COMMENT }

    public enum Reason { SPAM, ABUSE, ILLEGAL, PRIVACY, COPYRIGHT, OTHER }

    public enum Status { OPEN, RESOLVED, DISMISSED }

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private TargetType targetType;
    // 신고 대상(글 또는 댓글)의 Id
    @Column(length = 36, nullable = false)
    private String targetId;
    // 신고 시점의 대상 요약(글 제목 / 댓글 앞부분)과 화면 경로. 대상이 지워져도 무엇이었는지 알 수 있게 남긴다.
    @Column(length = 300)
    private String targetSummary;
    @Column(length = 400)
    private String targetPath;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Reason reason;
    @Column(length = 500)
    private String detail;
    // 신고한 사람 (손님이면 이름·이메일이 비어 있다)
    @Column(length = 64)
    private String reporterName;
    @Column(length = 320)
    private String reporterEmail;
    @Column(length = 64)
    private String reporterIp;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private Status status;
    // 처리한 관리자와 시각
    @Column(length = 320)
    private String handledBy;
    private LocalDateTime handledAt;

    public void handle(Status status, String handledBy) {
        this.status = status;
        this.handledBy = status == Status.OPEN ? null : handledBy;
        this.handledAt = status == Status.OPEN ? null : LocalDateTime.now();
    }
}
