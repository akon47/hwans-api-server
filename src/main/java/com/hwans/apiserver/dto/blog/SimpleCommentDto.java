package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Getter
@Builder
@ApiModel(description = "댓글 리스트 조회용 Dto")
public class SimpleCommentDto implements Serializable {
    @ApiModelProperty(value = "댓글 Id", required = true)
    @NotBlank
    String id;
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "댓글을 단 사용자", required = true)
    @NotBlank
    SimpleAccountDto account;
}
