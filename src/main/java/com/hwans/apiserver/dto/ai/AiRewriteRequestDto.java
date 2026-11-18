package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 다듬기 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 다듬기 요청 Dto")
public class AiRewriteRequestDto implements Serializable {
    @Schema(description = "선택한 부분 (20000자 이내)", requiredMode = Schema.RequiredMode.REQUIRED)
    String text;
    @Schema(description = "polish | expand | shorten | fix | custom", requiredMode = Schema.RequiredMode.REQUIRED, example = "polish")
    String instruction;
    @Schema(description = "custom 일 때의 지시 (1000자 이내)")
    String customInstruction;
    @Schema(description = "앞뒤 문맥 (선택). 선택 앞뒤 글 사이에 \"[다듬을 부분]\" 표시로 text 의 자리를 나타낸다. 4500자까지만 쓴다.")
    String context;
}
