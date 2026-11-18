package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 제목·요약·태그 일괄 제안 요청 Dto (글은 바꾸지 않는다)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제목·요약·태그 일괄 제안 요청 Dto")
public class AiMetaBatchRequestDto implements Serializable {
    @Schema(description = "제안을 만들 내 글 주소 (1~100개, 중복은 한 번만)", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> postUrls;
    @Schema(description = "제안할 항목 ⊆ {title, summary, tags} (없으면 셋 다)", example = "[\"summary\", \"tags\"]")
    List<String> fields;
}
