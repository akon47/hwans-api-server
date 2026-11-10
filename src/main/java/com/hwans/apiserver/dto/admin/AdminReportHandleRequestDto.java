package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.entity.admin.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.io.Serializable;

/**
 * 신고 처리 요청 Dto
 */
@Getter
@Schema(description = "신고 처리 요청 Dto")
public class AdminReportHandleRequestDto implements Serializable {
    @Schema(description = "바꿀 처리 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    Report.Status status;
    @Schema(description = "true 면 신고된 글·댓글도 함께 삭제한다 (status 가 RESOLVED 일 때만)")
    boolean deleteTarget;
}
