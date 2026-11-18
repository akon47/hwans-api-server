package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 커버 이미지 만들기 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커버 이미지 만들기 요청 Dto")
public class AiCoverRequestDto implements Serializable {
    @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED)
    String title;
    @Schema(description = "요약")
    String summary;
    @Schema(description = "태그")
    List<String> tags;
}
