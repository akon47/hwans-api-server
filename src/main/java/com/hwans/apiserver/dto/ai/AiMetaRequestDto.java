package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 제목·요약·태그·주소 추천 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제목·요약·태그·주소 추천 요청 Dto")
public class AiMetaRequestDto implements Serializable {
    @Schema(description = "지금 제목 (있으면)")
    String title;
    @Schema(description = "본문 마크다운", requiredMode = Schema.RequiredMode.REQUIRED)
    String content;
}
