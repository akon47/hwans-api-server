package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Set;

@Getter
@Builder
@ApiModel(description = "간단한 계정 정보 Dto")
public class SimpleAccountDto implements Serializable {
    @ApiModelProperty(value = "계정 사용자 이메일", required = true, example = "akon47@naver.com")
    @NotBlank
    String email;
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    String name;
}