package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 글쓰기 설정 변경 요청 Dto. 빈 값(null)인 항목은 바꾸지 않는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 글쓰기 설정 변경 요청 Dto")
public class AiSettingsRequestDto implements Serializable {
    @Schema(description = "Anthropic API 키 (없거나 null 이면 기존 키 유지). 저장 전에 Anthropic 에서 확인한다.", example = "sk-ant-...")
    String apiKey;
    @Schema(description = "모델 Id (claude-opus-5-5, claude-sonnet-5-5, claude-haiku-5-5)", example = "claude-opus-5-5")
    String model;
    @Schema(description = "월 사용 한도 USD (0.5~500, 기본 10)", example = "10")
    Double monthlyLimitUsd;
}
