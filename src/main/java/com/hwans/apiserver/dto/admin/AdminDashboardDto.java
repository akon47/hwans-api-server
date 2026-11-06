package com.hwans.apiserver.dto.admin;

import com.hwans.apiserver.dto.blog.SimplePostDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 관리자 대시보드 Dto (요약 수치 + 최근 30일 추이 + 최근 활동)
 */
@Getter
@Builder
@Schema(description = "관리자 대시보드 Dto")
public class AdminDashboardDto implements Serializable {
    @Schema(description = "활성 회원 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long memberCount;
    @Schema(description = "전체 게시글 수 (삭제 제외, 비공개·임시저장 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
    long postCount;
    @Schema(description = "공개 게시글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long publicPostCount;
    @Schema(description = "전체 댓글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long commentCount;
    @Schema(description = "전체 조회수 합", requiredMode = Schema.RequiredMode.REQUIRED)
    long totalHits;
    @Schema(description = "현재 웹소켓 접속 세션 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int sessionCount;
    @Schema(description = "지금 글을 읽고 있는 세션 수", requiredMode = Schema.RequiredMode.REQUIRED)
    int viewingCount;
    @Schema(description = "오늘 가입한 회원 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long todayMembers;
    @Schema(description = "오늘 쓴 글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long todayPosts;
    @Schema(description = "오늘 달린 댓글 수", requiredMode = Schema.RequiredMode.REQUIRED)
    long todayComments;
    @Schema(description = "최근 30일 일별 추이 (오래된 날짜부터)", requiredMode = Schema.RequiredMode.REQUIRED)
    List<DailyCount> daily;
    @Schema(description = "최근 글", requiredMode = Schema.RequiredMode.REQUIRED)
    List<SimplePostDto> recentPosts;
    @Schema(description = "최근 댓글", requiredMode = Schema.RequiredMode.REQUIRED)
    List<AdminCommentDto> recentComments;
    @Schema(description = "최근 가입 회원", requiredMode = Schema.RequiredMode.REQUIRED)
    List<AdminAccountDto> recentMembers;

    @Getter
    @Builder
    @Schema(description = "일별 집계")
    public static class DailyCount implements Serializable {
        @Schema(description = "날짜", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date;
        @Schema(description = "가입 회원 수", requiredMode = Schema.RequiredMode.REQUIRED)
        long members;
        @Schema(description = "작성 글 수", requiredMode = Schema.RequiredMode.REQUIRED)
        long posts;
        @Schema(description = "작성 댓글 수", requiredMode = Schema.RequiredMode.REQUIRED)
        long comments;
    }
}
