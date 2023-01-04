package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 사용자 계정 역할 Dto
 */
@Getter
@Builder
@ApiModel(description = "역할 Dto")
public class RoleDto implements Serializable {
    @ApiModelProperty(value = "역할 이름", required = true, example = "ROLE_USER")
    @NotBlank
    String name;
}