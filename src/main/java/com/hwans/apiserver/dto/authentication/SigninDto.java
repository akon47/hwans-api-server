package com.hwans.apiserver.dto.authentication;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@ApiModel(description = "계정 인증 정보 Dto")
public class SigninDto implements Serializable {
    @ApiModelProperty(value = "사용자 계정 아이디", required = true, example = "kimhwan92")
    String id;
    @ApiModelProperty(value = "사용자 계정 비밀번호", required = true, example = "12345")
    String password;
}
