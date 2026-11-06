package com.hwans.apiserver.dto.token;

import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 개인 액세스 토큰 정보 Dto (토큰 원문은 포함하지 않는다)
 */
@Getter
@Builder
@Schema(description = "개인 액세스 토큰 정보 Dto")
public class AccessTokenDto implements Serializable {
    @Schema(description = "토큰 레코드 Id (폐기할 때 사용)", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "토큰 공개 Id (토큰 문자열 hsp_<tokenId>_… 의 가운데 부분)", requiredMode = Schema.RequiredMode.REQUIRED)
    String tokenId;
    @Schema(description = "이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "Claude Code - 블로그 글쓰기")
    String name;
    @Schema(description = "권한 목록", requiredMode = Schema.RequiredMode.REQUIRED, example = "[\"posts\", \"social\"]")
    List<String> scopes;
    @Schema(description = "발급 경로", requiredMode = Schema.RequiredMode.REQUIRED)
    AccessTokenOrigin origin;
    @Schema(description = "발급 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
    @Schema(description = "만료 시각 (null 이면 무기한)")
    LocalDateTime expiresAt;
    @Schema(description = "마지막 사용 시각")
    LocalDateTime lastUsedAt;
    @Schema(description = "마지막 사용 IP")
    String lastUsedIp;
    @Schema(description = "폐기 시각 (null 이면 폐기되지 않음)")
    LocalDateTime revokedAt;
    @Schema(description = "만료 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean expired;
    @Schema(description = "사용 가능 여부 (폐기되지 않았고 만료되지 않음)", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean usable;
}
