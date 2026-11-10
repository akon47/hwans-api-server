package com.hwans.apiserver.dto.site;

import com.hwans.apiserver.entity.admin.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * 글·댓글 신고 요청 Dto
 */
@Getter
@Schema(description = "글·댓글 신고 요청 Dto")
public class ReportRequestDto implements Serializable {
    @Schema(description = "신고 대상 종류", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    Report.TargetType targetType;
    @Schema(description = "신고 대상(글 또는 댓글) Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID targetId;
    @Schema(description = "신고 사유", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    Report.Reason reason;
    @Schema(description = "자세한 설명")
    @Size(max = 500)
    String detail;
}
