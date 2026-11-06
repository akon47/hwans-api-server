package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 관리자용 시스템 상태 Dto
 */
@Getter
@Builder
@Schema(description = "관리자용 시스템 상태 Dto")
public class AdminSystemDto implements Serializable {
    @Schema(description = "앱 버전")
    String appVersion;
    @Schema(description = "빌드 시각")
    String buildTime;
    @Schema(description = "실행 중인 프로필", requiredMode = Schema.RequiredMode.REQUIRED)
    String profile;
    @Schema(description = "Java 버전", requiredMode = Schema.RequiredMode.REQUIRED)
    String javaVersion;
    @Schema(description = "서버 시작 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime startedAt;
    @Schema(description = "가동 시간(ms)", requiredMode = Schema.RequiredMode.REQUIRED)
    long uptimeMillis;
    @Schema(description = "CPU 코어 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int processors;
    @Schema(description = "힙 사용량(bytes)", requiredMode = Schema.RequiredMode.REQUIRED)
    long heapUsedBytes;
    @Schema(description = "힙 최대(bytes)", requiredMode = Schema.RequiredMode.REQUIRED)
    long heapMaxBytes;
    @Schema(description = "DB 연결 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean databaseOk;
    @Schema(description = "Redis 연결 상태", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean redisOk;
    @Schema(description = "웹소켓 세션 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int websocketSessions;
    @Schema(description = "첨부파일 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long attachmentCount;
    @Schema(description = "첨부파일 총 용량(bytes)", requiredMode = Schema.RequiredMode.REQUIRED)
    long attachmentBytes;
    @Schema(description = "첨부파일 저장 경로", requiredMode = Schema.RequiredMode.REQUIRED)
    String attachmentPath;
    @Schema(description = "첨부파일 디스크 남은 용량(bytes)", requiredMode = Schema.RequiredMode.REQUIRED)
    long attachmentFreeBytes;
    @Schema(description = "첨부파일 디스크 전체 용량(bytes)", requiredMode = Schema.RequiredMode.REQUIRED)
    long attachmentTotalBytes;
    @Schema(description = "예약 발행 대기 글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long scheduledPostCount;
    @Schema(description = "승인 대기 중인 기기 로그인 요청 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long pendingDeviceCount;
    @Schema(description = "폐기되지 않은 액세스 토큰 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long activeTokenCount;
}
