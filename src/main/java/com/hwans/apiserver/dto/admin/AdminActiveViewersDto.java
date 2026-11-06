package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 관리자 실시간 접속자 목록 응답 Dto.
 */
@Getter
@Builder
@Schema(description = "관리자 실시간 접속자 목록 Dto")
public class AdminActiveViewersDto implements Serializable {
    @Schema(description = "현재 연결된 전체 웹소켓 세션 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int totalSessions;
    @Schema(description = "현재 게시글을 보고 있는 시청자 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<AdminActiveViewerDto> viewers;
}
