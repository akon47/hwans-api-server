package com.hwans.apiserver.dto.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 간단한 계정 정보 Dto
 */
@Getter
@Builder
@ApiModel(description = "간단한 계정 정보 Dto")
public class SimpleAccountDto implements Serializable {
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    String name;
    @ApiModelProperty(value = "블로그 Id", example = "kim-hwan")
    String blogId;
    @ApiModelProperty(value = "프로필 이미지 URL", example = "/attachments/file-id")
    String profileImageUrl;
    @ApiModelProperty(value = "비회원인지 여부", required = true)
    boolean guest;
}