package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "사용자 생성 Dto")
public class AccountCreateDto implements Serializable {
    @ApiModelProperty(value = "사용자 로그인 Id", required = true, example = "kimhwan92")
    String id;
    @ApiModelProperty(value = "사용자 로그인 비밀번호", required = true, example = "12345")
    String password;
    @ApiModelProperty(value = "사용자 이름", required = true, example = "김환")
    String name;
}
