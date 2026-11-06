package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.support.annotation.PostUrl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 작성/수정 Dto
 */
@Getter
@Builder
@Schema(description = "게시글 작성/수정 Dto")
public class PostRequestDto implements Serializable {
    @Schema(description = "게시글 URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "my-first-post")
    @Length(max = 320)
    @PostUrl
    String postUrl;
    @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "제목입니다.")
    @NotBlank
    @Length(max = 2000)
    String title;
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "게시글 내용입니다.")
    @NotBlank
    String content;
    @Schema(description = "게시글 공개 유형", requiredMode = Schema.RequiredMode.REQUIRED, example = "PUBLIC")
    @NotNull
    OpenType openType;
    @Schema(description = "요약 내용", example = "요약 내용입니다.")
    @NotBlank
    @Length(max = 255)
    String summary;
    @Schema(description = "썸네일 파일 Id")
    UUID thumbnailFileId;
    @Schema(description = "태그", requiredMode = Schema.RequiredMode.REQUIRED)
    Set<TagDto> tags;
    @Schema(description = "시리즈 Url", example = "series_url")
    @Length(max = 320)
    String seriesUrl;
    @Schema(description = "예약 발행 시각 (openType 이 SCHEDULED 인 경우 사용)")
    java.time.LocalDateTime scheduledAt;
}
