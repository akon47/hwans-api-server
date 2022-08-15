package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@ApiModel(description = "게시글 Dto")
public class PostDto implements Serializable {
    @ApiModelProperty(value = "게시글 Id", required = true)
    @NotNull
    UUID id;
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
    @ApiModelProperty(value = "글쓴이", required = true)
    @NotBlank
    SimpleAccountDto author;
    @ApiModelProperty(value = "작성 시간", required = true)
    @NotBlank
    LocalDateTime createdAt;
    @ApiModelProperty(value = "태그")
    Set<TagDto> tags;
    @ApiModelProperty(value = "댓글")
    Set<SimpleCommentDto> comments;
    @ApiModelProperty(value = "좋아요 수")
    int likeCount;
}
