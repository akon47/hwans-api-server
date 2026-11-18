package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 제목·요약·태그 적용 결과 Dto
 */
@Getter
@Builder
@Schema(description = "제목·요약·태그 적용 결과 Dto")
public class AiMetaApplyResultDto implements Serializable {
    @Schema(description = "적용한 글 수", example = "78")
    int applied;
    @Schema(description = "적용하지 못한 글")
    List<Failure> failed;

    @Getter
    @Builder
    @Schema(description = "적용하지 못한 글")
    public static class Failure implements Serializable {
        String postUrl;
        String message;
    }
}
