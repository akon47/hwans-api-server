package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

@Getter
@Builder
@ApiModel(description = "사용자 계정 비밀번호 재설정 Dto")
public class ResetPasswordDto {
    @ApiModelProperty(value = "재설정 할 사용자 로그인 비밀번호", required = true, example = "12345")
    @NotBlank
    String newPassword;
    @ApiModelProperty(value = "비밀번호 재설정 토큰", required = true)
    @NotBlank
    String resetPasswordToken;
}
