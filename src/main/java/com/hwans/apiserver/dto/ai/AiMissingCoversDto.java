package com.hwans.apiserver.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 썸네일이 없는 내 글 목록 Dto (커버 일괄 만들기용)
 */
@Getter
@Builder
@Schema(description = "썸네일이 없는 내 글 목록 Dto")
public class AiMissingCoversDto implements Serializable {
    @Schema(description = "썸네일이 없는 글 전체 수 (posts 는 최대 500개)", example = "28")
    long total;
    @Schema(description = "썸네일이 없는 글 (최신 글부터)")
    List<Post> posts;

    @Getter
    @Builder
    @Schema(description = "썸네일이 없는 글")
    public static class Post implements Serializable {
        @Schema(example = "wpf-timer")
        String postUrl;
        @Schema(example = "[WPF] 타이머 사용하기")
        String title;
        @Schema(description = "PUBLIC | PRIVATE | DRAFT | SCHEDULED", example = "PUBLIC")
        String openType;
        @Schema(description = "작성 시각 (yyyy-MM-dd'T'HH:mm:ss)", example = "2013-06-20T10:00:00")
        String createdAt;
    }
}
