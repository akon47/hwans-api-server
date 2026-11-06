package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 관리자용 개인 액세스 토큰 Dto (소유자 정보를 함께 담는다. 토큰 원문은 없다)
 */
@Getter
@Builder
@Schema(description = "관리자용 개인 액세스 토큰 Dto")
public class AdminTokenDto implements Serializable {
    @Schema(description = "토큰 레코드 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "토큰 공개 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    String tokenId;
    @Schema(description = "이름", requiredMode = Schema.RequiredMode.REQUIRED)
    String name;
    @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> scopes;
    @Schema(description = "발급 경로", requiredMode = Schema.RequiredMode.REQUIRED)
    AccessTokenOrigin origin;
    @Schema(description = "소유자", requiredMode = Schema.RequiredMode.REQUIRED)
    SimpleAccountDto owner;
    @Schema(description = "소유자 이메일", requiredMode = Schema.RequiredMode.REQUIRED)
    String ownerEmail;
    @Schema(description = "발급 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
    @Schema(description = "만료 시각 (null 이면 무기한)")
    LocalDateTime expiresAt;
    @Schema(description = "마지막 사용 시각")
    LocalDateTime lastUsedAt;
    @Schema(description = "마지막 사용 IP")
    String lastUsedIp;
    @Schema(description = "폐기 시각")
    LocalDateTime revokedAt;
    @Schema(description = "만료 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean expired;
    @Schema(description = "사용 가능 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean usable;
}
