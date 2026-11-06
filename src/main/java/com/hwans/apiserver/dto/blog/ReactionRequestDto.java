package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 게시글 이모지 반응 요청 Dto
 */
@Getter
@Schema(description = "게시글 이모지 반응 요청 Dto")
public class ReactionRequestDto implements Serializable {
    @Schema(description = "이모지", requiredMode = Schema.RequiredMode.REQUIRED, example = "👍")
    @NotBlank
    String emoji;
}
