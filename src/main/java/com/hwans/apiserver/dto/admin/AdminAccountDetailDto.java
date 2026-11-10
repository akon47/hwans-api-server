package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.dto.blog.SimplePostDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 관리자용 회원 상세 Dto (한 회원의 활동을 한 화면에 모아 본다)
 */
@Getter
@Builder
@Schema(description = "관리자용 회원 상세 Dto")
public class AdminAccountDetailDto implements Serializable {
    @Schema(description = "회원", requiredMode = Schema.RequiredMode.REQUIRED)
    AdminAccountDto account;
    @Schema(description = "쓴 글 수 (비공개·임시저장 포함, 삭제 제외)", requiredMode = Schema.RequiredMode.REQUIRED)
    long postCount;
    @Schema(description = "쓴 댓글 수 (삭제 제외)", requiredMode = Schema.RequiredMode.REQUIRED)
    long commentCount;
    @Schema(description = "글 조회수 합", requiredMode = Schema.RequiredMode.REQUIRED)
    long totalHits;
    @Schema(description = "이 회원의 글·댓글이 신고된 횟수", requiredMode = Schema.RequiredMode.REQUIRED)
    long reportedCount;
    @Schema(description = "이 회원이 신고한 횟수", requiredMode = Schema.RequiredMode.REQUIRED)
    long reportCount;
    @Schema(description = "최근 글", requiredMode = Schema.RequiredMode.REQUIRED)
    List<SimplePostDto> recentPosts;
    @Schema(description = "최근 댓글", requiredMode = Schema.RequiredMode.REQUIRED)
    List<AdminCommentDto> recentComments;
}
