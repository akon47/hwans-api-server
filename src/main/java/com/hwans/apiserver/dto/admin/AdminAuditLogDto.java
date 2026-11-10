package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 활동 기록 Dto
 */
@Getter
@Builder
@Schema(description = "관리자 활동 기록 Dto")
public class AdminAuditLogDto implements Serializable {
    @Schema(description = "기록 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "행동한 관리자 이메일")
    String actorEmail;
    @Schema(description = "행동한 관리자 이름")
    String actorName;
    @Schema(description = "행동 종류", requiredMode = Schema.RequiredMode.REQUIRED)
    String action;
    @Schema(description = "대상 종류")
    String targetType;
    @Schema(description = "대상 Id")
    String targetId;
    @Schema(description = "요약")
    String summary;
    @Schema(description = "시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
}
