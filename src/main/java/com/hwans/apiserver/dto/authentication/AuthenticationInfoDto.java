package com.hwans.apiserver.dto.authentication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 계정 인증 정보 Dto
 */
@Getter
@Builder
@ApiModel(description = "계정 인증 정보 Dto")
public class AuthenticationInfoDto implements Serializable {
    @ApiModelProperty(value = "사용자 계정 이메일", required = true, example = "akon47@naver.com")
    @NotBlank
    String email;
    @ApiModelProperty(value = "사용자 계정 비밀번호", required = true, example = "12345")
    @NotBlank
    String password;
}