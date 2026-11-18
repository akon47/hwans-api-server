package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 커버 일괄 만들기 요청 Dto
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "커버 일괄 만들기 요청 Dto")
public class AiCoverBatchRequestDto implements Serializable {
    @Schema(description = "커버를 만들 내 글 주소 (1~100개, 중복은 한 번만)", requiredMode = Schema.RequiredMode.REQUIRED)
    List<String> postUrls;
    @Schema(description = "만든 커버를 그 글의 썸네일로 지정 (이미 썸네일이 있는 글은 건너뜀, 기본 true)", example = "true")
    Boolean useAsThumbnail;
    @Schema(description = "본문 맨 앞에 커버 이미지를 넣기 (기본 false)", example = "false")
    Boolean insertIntoContent;
}
