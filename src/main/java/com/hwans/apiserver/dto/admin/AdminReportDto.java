package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.entity.admin.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 신고 Dto
 */
@Getter
@Builder
@Schema(description = "관리자용 신고 Dto")
public class AdminReportDto implements Serializable {
    @Schema(description = "신고 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "대상 종류", requiredMode = Schema.RequiredMode.REQUIRED)
    Report.TargetType targetType;
    @Schema(description = "대상 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    String targetId;
    @Schema(description = "신고 시점의 대상 요약 (글 제목 / 댓글 앞부분)")
    String targetSummary;
    @Schema(description = "대상을 볼 수 있는 화면 경로")
    String targetPath;
    @Schema(description = "대상이 아직 남아 있는지 (삭제되지 않았는지)", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean targetExists;
    @Schema(description = "사유", requiredMode = Schema.RequiredMode.REQUIRED)
    Report.Reason reason;
    @Schema(description = "자세한 설명")
    String detail;
    @Schema(description = "신고한 사람 이름 (손님이면 없음)")
    String reporterName;
    @Schema(description = "신고한 사람 이메일 (손님이면 없음)")
    String reporterEmail;
    @Schema(description = "신고한 사람 IP")
    String reporterIp;
    @Schema(description = "처리 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    Report.Status status;
    @Schema(description = "처리한 관리자")
    String handledBy;
    @Schema(description = "처리 시각")
    LocalDateTime handledAt;
    @Schema(description = "신고 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
}
