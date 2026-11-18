package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 제목·요약·태그를 채울 만한 내 글 목록 Dto
 */
@Getter
@Builder
@Schema(description = "태그가 없거나 요약이 40자 미만인 내 글 목록 Dto")
public class AiMetaCandidatesDto implements Serializable {
    @Schema(description = "해당하는 글 전체 수 (posts 는 최대 500개)", example = "80")
    long total;
    @Schema(description = "해당하는 글 (최신 글부터)")
    List<Post> posts;

    @Getter
    @Builder
    @Schema(description = "태그가 없거나 요약이 짧은 글")
    public static class Post implements Serializable {
        String postUrl;
        String title;
        @Schema(description = "PUBLIC | PRIVATE | DRAFT | SCHEDULED", example = "PUBLIC")
        String openType;
        @Schema(description = "작성 시각 (yyyy-MM-dd'T'HH:mm:ss)", example = "2013-06-20T10:00:00")
        String createdAt;
        @Schema(description = "지금 요약 (없으면 빈 문자열)")
        String summary;
        @Schema(description = "지금 태그")
        List<String> tags;
        @Schema(description = "태그가 없는지")
        boolean noTags;
        @Schema(description = "요약이 40자 미만(또는 없음)인지")
        boolean shortSummary;
    }
}
