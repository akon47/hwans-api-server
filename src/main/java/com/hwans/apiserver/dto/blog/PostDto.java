package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Set;

@Getter
@Builder
@ApiModel(description = "게시글 Dto")
public class PostDto implements Serializable {
    @ApiModelProperty(value = "제목", required = true)
    @NotBlank
    String title;
    @ApiModelProperty(value = "내용", required = true)
    @NotBlank
    String content;
    @ApiModelProperty(value = "태그")
    @NotBlank
    Set<TagDto> tags;
    @ApiModelProperty(value = "댓글")
    @NotBlank
    Set<SimpleCommentDto> comments;
}
