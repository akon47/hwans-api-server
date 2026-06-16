package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 게시글 이모지 반응 요청 Dto
 */
@Getter
@ApiModel(description = "게시글 이모지 반응 요청 Dto")
public class ReactionRequestDto implements Serializable {
    @ApiModelProperty(value = "이모지", required = true, example = "👍")
    @NotBlank
    String emoji;
}
