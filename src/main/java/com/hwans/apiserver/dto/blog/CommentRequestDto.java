package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 댓글 작성/수정 Dto
 */
@Getter
@ApiModel(description = "댓글 작성/수정 Dto")
public class CommentRequestDto implements Serializable {
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
}
