package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "사용자 정보 수정 Dto")
public class ModifyAccountDto implements Serializable {
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    @Max(32)
    String name;
    @ApiModelProperty(value = "간단한 자기소개", example = "안녕하세요, 반갑습니다.")
    @Max(255)
    String biography;
    @ApiModelProperty(value = "회사", example = "google")
    @Max(64)
    String company;
    @ApiModelProperty(value = "위치", example = "seoul")
    @Max(64)
    String location;
    @ApiModelProperty(value = "홈페이지", example = "https://kimhwan.kr")
    @Max(255)
    String homepage;
}
