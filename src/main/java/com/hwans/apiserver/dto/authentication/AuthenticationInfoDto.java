package com.hwans.apiserver.dto.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 계정 인증 정보 Dto
 */
@Getter
@Builder
@Schema(description = "계정 인증 정보 Dto")
public class AuthenticationInfoDto implements Serializable {
    @Schema(description = "사용자 계정 이메일", requiredMode = Schema.RequiredMode.REQUIRED, example = "akon47@naver.com")
    @NotBlank
    String email;
    @Schema(description = "사용자 계정 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    @NotBlank
    String password;
}
