package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * AI 초안 쓰기 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI 초안 쓰기 요청 Dto")
public class AiDraftRequestDto implements Serializable {
    @Schema(description = "무엇에 대해 쓸지 (2000자 이내)", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "CVE-2026-114938 자바 보안 취약점이 어떤 문제이고 어떻게 해결하는지")
    String request;
    @Schema(description = "메모·개요 (선택, 10000자 이내)")
    String notes;
    @Schema(description = "분량: short | medium | long (기본 medium)", example = "medium")
    String length;
    @Schema(description = "웹에서 최신 자료를 찾아 출처와 함께 쓸지", example = "true")
    Boolean webResearch;
}
