package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Set;

@Getter
@Builder
@ApiModel(description = "사용자 정보 Dto")
public class AccountDto implements Serializable {
    @ApiModelProperty(value = "계정 사용자 이메일", required = true, example = "akon47@naver.com")
    @NotBlank
    @Length(max = 320)
    @Email
    String email;
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @ApiModelProperty(value = "블로그 Id", required = true, example = "kim-hwan")
    @NotBlank
    @Length(max = 64)
    String blogId;
    @ApiModelProperty(value = "프로필 이미지 URL", example = "/file-id")
    String profileImageUrl;
    @ApiModelProperty(value = "간단한 자기소개", example = "안녕하세요, 반갑습니다.")
    @Length(max = 255)
    String biography;
    @ApiModelProperty(value = "회사", example = "google")
    @Length(max = 64)
    String company;
    @ApiModelProperty(value = "위치", example = "seoul")
    @Length(max = 64)
    String location;
    @ApiModelProperty(value = "홈페이지", example = "https://kimhwan.kr")
    @Length(max = 255)
    String homepage;
    @ApiModelProperty(value = "계정에 할당된 역할", required = true, example = "사용자")
    Set<RoleDto> roles;
}