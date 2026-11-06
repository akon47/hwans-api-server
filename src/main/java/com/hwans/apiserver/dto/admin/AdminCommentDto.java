package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자용 댓글 Dto (어느 글의 댓글인지 함께 담는다)
 */
@Getter
@Builder
@Schema(description = "관리자용 댓글 Dto")
public class AdminCommentDto implements Serializable {
    @Schema(description = "댓글 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    UUID id;
    @Schema(description = "내용", requiredMode = Schema.RequiredMode.REQUIRED)
    String content;
    @Schema(description = "작성자", requiredMode = Schema.RequiredMode.REQUIRED)
    SimpleAccountDto author;
    @Schema(description = "부모 댓글 Id (답글이면)")
    UUID parentId;
    @Schema(description = "좋아요 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int likeCount;
    @Schema(description = "댓글이 달린 글의 블로그 Id")
    String postBlogId;
    @Schema(description = "댓글이 달린 글의 URL")
    String postUrl;
    @Schema(description = "댓글이 달린 글의 제목")
    String postTitle;
    @Schema(description = "작성 시각", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDateTime createdAt;
}
