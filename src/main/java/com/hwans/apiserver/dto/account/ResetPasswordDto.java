package com.hwans.apiserver.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 계정 비밀번호 재설정 Dto
 */
@Getter
@Builder
@Schema(description = "사용자 계정 비밀번호 재설정 Dto")
public class ResetPasswordDto {
    @Schema(description = "재설정 할 사용자 로그인 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    @NotBlank
    String newPassword;
    @Schema(description = "비밀번호 재설정 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    String resetPasswordToken;
}
