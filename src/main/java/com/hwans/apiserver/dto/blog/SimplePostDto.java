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
@ApiModel(description = "게시글 리스트 조회용 Dto")
public class SimplePostDto implements Serializable {
    @ApiModelProperty(value = "게시글이 작성되어진 블로그 Id", required = true, example = "kim-hwan")
    @NotBlank
    String blogId;
    @ApiModelProperty(value = "게시글 URL", required = true, example = "my-first-post")
    @NotBlank
    String postUrl;
    @ApiModelProperty(value = "제목", required = true, example = "제목입니다.")
    @NotBlank
    String title;
    @ApiModelProperty(value = "내용", required = true, example = "게시글 내용입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "태그")
    @NotBlank
    Set<TagDto> tags;
    @ApiModelProperty(value = "댓글 수")
    Long commentCount;
    @ApiModelProperty(value = "좋아요 수")
    Long likeCount;
}
