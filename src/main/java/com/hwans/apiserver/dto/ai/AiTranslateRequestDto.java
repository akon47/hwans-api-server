package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 영어 번역 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "영어 번역 요청 Dto")
public class AiTranslateRequestDto implements Serializable {
    @Schema(description = "제목")
    String title;
    @Schema(description = "요약")
    String summary;
    @Schema(description = "본문 마크다운", requiredMode = Schema.RequiredMode.REQUIRED)
    String content;
}
