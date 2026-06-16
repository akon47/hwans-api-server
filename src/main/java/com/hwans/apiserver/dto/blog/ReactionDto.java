package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 게시글 이모지 반응 집계 Dto
 */
@Getter
@Builder
@ApiModel(description = "게시글 이모지 반응 집계 Dto")
public class ReactionDto implements Serializable {
    @ApiModelProperty(value = "이모지", required = true, example = "👍")
    String emoji;
    @ApiModelProperty(value = "반응 수", required = true)
    long count;
    @ApiModelProperty(value = "현재 사용자가 반응했는지 여부", required = true)
    boolean reacted;
}
