package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 댓글 리스트 조회용 Dto
 */
@Getter
@Builder
@Schema(description = "댓글 리스트 조회용 Dto")
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCommentDto implements Serializable {
    @Schema(description = "댓글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "댓글입니다.")
    @NotBlank
    String content;
    @Schema(description = "부모 댓글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID parentId;
    @Schema(description = "대댓글 개수", requiredMode = Schema.RequiredMode.REQUIRED)
    int childrenCount;
    @Schema(description = "댓글을 단 사용자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    SimpleAccountDto author;
    @Schema(description = "작성 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    LocalDateTime createdAt;
    @Schema(description = "좋아요 수")
    int likeCount;
}
