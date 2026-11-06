package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import com.hwans.apiserver.entity.blog.OpenType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 게시글 리스트 조회용 Dto
 */
@Getter
@Builder
@Schema(description = "게시글 리스트 조회용 Dto")
public class SimplePostDto implements Serializable {
    @Schema(description = "게시글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "게시글 URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "my-first-post")
    @NotBlank
    String postUrl;
    @Schema(description = "제목", requiredMode = Schema.RequiredMode.REQUIRED, example = "제목입니다.")
    @NotBlank
    String title;
    @Schema(description = "요약 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "요약 내용입니다.")
    @NotBlank
    String summary;
    @Schema(description = "게시글 공개 유형", requiredMode = Schema.RequiredMode.REQUIRED, example = "PUBLIC")
    @NotNull
    OpenType openType;
    @Schema(description = "썸네일 이미지 URL", example = "/file-id")
    @NotBlank
    String thumbnailImageUrl;
    @Schema(description = "글쓴이", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    SimpleAccountDto author;
    @Schema(description = "작성 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    LocalDateTime createdAt;
    @Schema(description = "마지막 수정 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    LocalDateTime lastModifiedAt;
    @Schema(description = "태그")
    Set<TagDto> tags;
    @Schema(description = "댓글 수")
    int commentCount;
    @Schema(description = "좋아요 수")
    int likeCount;
    @Schema(description = "조회수")
    int hits;
    @Schema(description = "시리즈 Url")
    String seriesUrl;
    @Schema(description = "예약 발행 시각")
    LocalDateTime scheduledAt;
}
