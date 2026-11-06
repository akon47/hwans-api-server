package com.hwans.apiserver.dto.blog;

import com.hwans.apiserver.dto.account.AccountDto;
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
 * 댓글 Dto
 */
@Getter
@Builder
@Schema(description = "댓글 Dto")
public class CommentDto implements Serializable {
    @Schema(description = "댓글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "댓글입니다.")
    @NotBlank
    String content;
    @Schema(description = "부모 댓글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID parentId;
    @Schema(description = "대댓글")
    @NotBlank
    Set<SimpleCommentDto> children;
    @Schema(description = "댓글이 달려있는 게시글", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    SimplePostDto post;
    @Schema(description = "댓글을 단 사용자", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    AccountDto author;
    @Schema(description = "작성 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    LocalDateTime createdAt;
    @Schema(description = "좋아요 수")
    int likeCount;
}
