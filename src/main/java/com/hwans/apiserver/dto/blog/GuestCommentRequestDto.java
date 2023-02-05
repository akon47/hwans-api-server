package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 비회원 댓글 작성/수정 Dto
 */
@Getter
@ApiModel(description = "비회원 댓글 작성/수정 Dto")
public class GuestCommentRequestDto implements Serializable {
    @ApiModelProperty(value = "내용", required = true, example = "댓글입니다.")
    @NotBlank
    String content;
    @ApiModelProperty(value = "계정 사용자 이름", required = true, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @ApiModelProperty(value = "사용자 로그인 비밀번호", required = true, example = "12345")
    @NotBlank
    String password;
}
