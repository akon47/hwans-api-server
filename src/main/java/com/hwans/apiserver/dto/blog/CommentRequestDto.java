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
@ApiModel(description = "댓글 작성/수정 Dto")
public class CommentRequestDto implements Serializable {
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
}
