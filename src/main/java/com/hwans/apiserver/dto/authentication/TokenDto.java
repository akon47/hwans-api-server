package com.hwans.apiserver.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 계정 인증 토큰 Dto
 */
@Getter
@Builder
@Schema(description = "계정 인증 토큰 Dto")
public class TokenDto implements Serializable {
    @Schema(description = "인증 토큰 값", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String accessToken;
    @Schema(description = "인증 토큰 만료 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    Long accessTokenExpiresIn;
    @Schema(description = "갱신 토큰 값", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String refreshToken;
    @Schema(description = "갱신 토큰 만료 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    Long refreshTokenExpiresIn;
}
