package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 비회원 댓글 작성/수정 Dto
 */
@Getter
@Schema(description = "비회원 댓글 작성/수정 Dto")
public class GuestCommentRequestDto implements Serializable {
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "댓글입니다.")
    @NotBlank
    String content;
    @Schema(description = "계정 사용자 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "김환")
    @NotBlank
    @Length(max = 32)
    String name;
    @Schema(description = "사용자 로그인 비밀번호", requiredMode = Schema.RequiredMode.REQUIRED, example = "12345")
    @NotBlank
    String password;
}
