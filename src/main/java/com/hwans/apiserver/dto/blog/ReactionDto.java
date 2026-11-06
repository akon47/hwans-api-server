package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 게시글 이모지 반응 집계 Dto
 */
@Getter
@Builder
@Schema(description = "게시글 이모지 반응 집계 Dto")
public class ReactionDto implements Serializable {
    @Schema(description = "이모지", requiredMode = Schema.RequiredMode.REQUIRED, example = "👍")
    String emoji;
    @Schema(description = "반응 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long count;
    @Schema(description = "현재 사용자가 반응했는지 여부", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean reacted;
}
