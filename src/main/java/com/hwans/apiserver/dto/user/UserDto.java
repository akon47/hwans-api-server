package com.hwans.apiserver.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@ApiModel(description = "사용자 정보 Dto")
public class UserDto implements Serializable {
    @ApiModelProperty(value = "사용자 로그인 Id", required = true, example = "kimhwan92")
    String id;
    @ApiModelProperty(value = "사용자 이름", required = true, example = "김환")
    String name;
}
