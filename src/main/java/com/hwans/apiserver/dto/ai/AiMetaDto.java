package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 제목·요약·태그·주소 추천 결과 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "제목·요약·태그·주소 추천 결과 Dto")
public class AiMetaDto implements Serializable {
    @Schema(description = "제목 (60자 안팎)")
    String title;
    @Schema(description = "요약 (80~160자)")
    String summary;
    @Schema(description = "태그 (회원 블로그의 기존 표기 우선)", example = "[\"Java\", \"보안\"]")
    List<String> tags;
    @Schema(description = "게시글 주소 (영어 소문자·숫자·하이픈)", example = "java-cve-2026-114938-fix")
    String postUrl;
}
