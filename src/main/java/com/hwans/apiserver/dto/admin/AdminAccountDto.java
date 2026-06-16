package com.hwans.apiserver.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 관리자용 계정 정보 Dto
 */
@Getter
@Builder
@ApiModel(description = "관리자용 계정 정보 Dto")
public class AdminAccountDto implements Serializable {
    @ApiModelProperty(value = "계정 Id", required = true)
    UUID id;
    @ApiModelProperty(value = "이메일", required = true)
    String email;
    @ApiModelProperty(value = "이름", required = true)
    String name;
    @ApiModelProperty(value = "블로그 Id", required = true)
    String blogId;
    @ApiModelProperty(value = "프로필 이미지 URL")
    String profileImageUrl;
    @ApiModelProperty(value = "정지(비활성화) 여부", required = true)
    boolean deleted;
    @ApiModelProperty(value = "보유 역할 목록", required = true)
    Set<String> roles;
    @ApiModelProperty(value = "가입 시각", required = true)
    LocalDateTime createdAt;
}
