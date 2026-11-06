package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 댓글 작성/수정 Dto
 */
@Getter
@Schema(description = "댓글 작성/수정 Dto")
public class CommentRequestDto implements Serializable {
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "댓글입니다.")
    @NotBlank
    String content;
}
