package com.hwans.apiserver.dto.authentication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "계정 인증 정보 Dto")
public class AuthenticationInfoDto implements Serializable {
    @ApiModelProperty(value = "사용자 계정 아이디", required = true, example = "kimhwan92")
    @NotBlank
    String id;
    @ApiModelProperty(value = "사용자 계정 비밀번호", required = true, example = "12345")
    @NotBlank
    String password;
}