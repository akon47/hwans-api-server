package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 차단 관리(IP·금칙어) Dto 모음
 */
public class AdminModerationDto {
    @Getter
    @Builder
    @Schema(description = "차단한 IP")
    public static class BlockedIp implements Serializable {
        @Schema(description = "차단 Id", requiredMode = Schema.RequiredMode.REQUIRED)
        UUID id;
        @Schema(description = "IP", requiredMode = Schema.RequiredMode.REQUIRED)
        String ip;
        @Schema(description = "사유")
        String reason;
        @Schema(description = "차단한 관리자")
        String createdBy;
        @Schema(description = "차단 시각", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime createdAt;
    }

    @Getter
    @Schema(description = "IP 차단 요청")
    public static class BlockIpRequest implements Serializable {
        @Schema(description = "차단할 IP (IPv4 또는 IPv6)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 64)
        @Pattern(regexp = "^[0-9a-fA-F:.]+$")
        String ip;
        @Schema(description = "사유")
        @Size(max = 200)
        String reason;
    }

    @Getter
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @Schema(description = "금칙어 목록")
    public static class BannedWords implements Serializable {
        @Schema(description = "금칙어 (댓글과 손님 이름에 들어 있으면 등록을 막는다)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Size(max = 300)
        List<@Size(min = 1, max = 30) String> words;
    }
}
