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
@ApiModel(description = "계정 정보 Dto")
public class AccountDto implements Serializable {
    @ApiModelProperty(value = "계정 사용자 이메일", required = true, example = "akon47@naver.com")
    @NotBlank
    String email;
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    String name;
    @ApiModelProperty(value = "블로그 Id", required = true, example = "kim-hwan")
    @NotBlank
    String blogId;
    @ApiModelProperty(value = "프로필 이미지 URL", example = "/file-id")
    @NotBlank
    String profileImageUrl;
    @ApiModelProperty(value = "간단한 자기소개", example = "안녕하세요, 반갑습니다.")
    String biography;
    @ApiModelProperty(value = "회사", example = "google")
    String company;
    @ApiModelProperty(value = "위치", example = "seoul")
    String location;
    @ApiModelProperty(value = "홈페이지", example = "https://kimhwan.kr")
    String homepage;
    @ApiModelProperty(value = "계정에 할당된 역할", required = true, example = "사용자")
    Set<RoleDto> roles;
}