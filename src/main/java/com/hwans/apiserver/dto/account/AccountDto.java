package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;

@Getter
@Builder
@ApiModel(description = "계정 정보 Dto")
public class AccountDto implements Serializable {
    @ApiModelProperty(value = "계정 로그인 Id", required = true, example = "kimhwan92")
    String id;
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    String name;
    @ApiModelProperty(value = "계정에 할당된 역할", required = true, example = "사용자")
    Set<RoleDto> roles;
}
