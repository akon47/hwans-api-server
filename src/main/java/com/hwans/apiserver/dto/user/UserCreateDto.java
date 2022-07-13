package com.hwans.apiserver.dto.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ApiModel(description = "사용자 생성 Dto")
public class UserCreateDto {

    @ApiModelProperty(value = "사용자 이름", required = true, example = "김환")
    String userName;

}
