package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 관리자 대시보드 통계 Dto
 */
@Getter
@Builder
@Schema(description = "관리자 대시보드 통계 Dto")
public class AdminStatisticsDto implements Serializable {
    @Schema(description = "활성 회원 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long memberCount;
    @Schema(description = "전체 게시글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long postCount;
    @Schema(description = "전체 댓글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long commentCount;
}
