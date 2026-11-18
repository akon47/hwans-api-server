package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * AI 글쓰기 설정 Dto. API 키 원문은 절대 담지 않는다(끝 4자리만).
 */
@Getter
@Builder
@Schema(description = "AI 글쓰기 설정 Dto")
public class AiSettingsDto implements Serializable {
    @Schema(description = "서버에서 AI 기능을 쓸 수 있는지 (false 면 서버에 암호화 키가 없어 기능이 꺼짐)", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean available;
    @Schema(description = "이 회원이 API 키를 등록했는지", requiredMode = Schema.RequiredMode.REQUIRED)
    boolean configured;
    @Schema(description = "등록한 키의 끝 4자리 (없으면 null)", example = "AbCd")
    String keyLast4;
    @Schema(description = "쓰는 모델 Id", requiredMode = Schema.RequiredMode.REQUIRED, example = "claude-opus-5-5")
    String model;
    @Schema(description = "월 사용 한도 (USD)", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.0")
    double monthlyLimitUsd;
    @Schema(description = "이번 달 사용량", requiredMode = Schema.RequiredMode.REQUIRED)
    Usage usage;
    @Schema(description = "문체 프로필", requiredMode = Schema.RequiredMode.REQUIRED)
    AiStyleDto style;
    @Schema(description = "고를 수 있는 모델 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    List<ModelInfo> models;

    @Getter
    @Builder
    @Schema(description = "이번 달 AI 사용량")
    public static class Usage implements Serializable {
        @Schema(description = "달 (yyyy-MM)", example = "2026-11")
        String month;
        @Schema(description = "이번 달 비용 합계 (USD)", example = "1.23")
        double costUsd;
        @Schema(description = "이번 달 요청 수", example = "14")
        long requests;
    }

    @Getter
    @Builder
    @Schema(description = "모델 정보 (1M 토큰당 USD)")
    public static class ModelInfo implements Serializable {
        @Schema(example = "claude-opus-5-5")
        String id;
        @Schema(example = "Claude Opus 5.5")
        String label;
        @Schema(example = "가장 잘 써요(기본)")
        String description;
        @Schema(example = "4.0")
        double inputPerMTok;
        @Schema(example = "20.0")
        double outputPerMTok;
    }
}
