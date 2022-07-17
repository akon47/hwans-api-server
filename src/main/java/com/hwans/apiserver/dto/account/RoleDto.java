package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@ApiModel(description = "역할 Dto")
public class RoleDto implements Serializable {
    @ApiModelProperty(value = "역할 이름", required = true, example = "사용자")
    String name;
}
