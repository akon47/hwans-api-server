package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.entity.token.DeviceAuthorization;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 기기 로그인 요청 Dto (사람이 승인 화면에서 확인하는 내용)
 */
@Getter
@Builder
@Schema(description = "기기 로그인 요청 Dto")
public class DeviceAuthorizationDto implements Serializable {
    @Schema(description = "사용자 코드", requiredMode = Schema.RequiredMode.REQUIRED, example = "BCDF-GHJK")
    String userCode;
    @Schema(description = "요청한 클라이언트 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "Claude Code - 블로그 글쓰기")
    String clientName;
    @Schema(description = "요청한 권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> scopes;
    @Schema(description = "요청한 IP")
    String requestedIp;
    @Schema(description = "요청한 User-Agent")
    String requestedUa;
    @Schema(description = "상태", requiredMode = Schema.RequiredMode.REQUIRED)
    DeviceAuthorization.Status status;
    @Schema(description = "요청 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
    @Schema(description = "코드 만료 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime expiresAt;
}
