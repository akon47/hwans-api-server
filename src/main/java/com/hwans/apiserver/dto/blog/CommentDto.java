package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "댓글 Dto")
public class CommentDto implements Serializable {
    @ApiModelProperty(value = "댓글 Id", required = true)
    @NotBlank
    String id;
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "댓글이 달려있는 게시글", required = true)
    @NotBlank
    PostDto post;
    @ApiModelProperty(value = "댓글을 단 사용자", required = true)
    @NotBlank
    AccountDto account;
}
