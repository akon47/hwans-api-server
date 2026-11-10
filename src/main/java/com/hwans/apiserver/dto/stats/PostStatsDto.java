package com.hwans.apiserver.dto.stats;

import com.hwans.apiserver.dto.blog.SimplePostDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 통계 Dto 모음: 내 글 통계(작성자용)와 사이트 인사이트(관리자용)가 함께 쓴다.
 */
public class PostStatsDto {
    @Getter
    @Builder
    @Schema(description = "날짜별 조회수")
    public static class DailyViews implements Serializable {
        @Schema(description = "날짜", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate date;
        @Schema(description = "조회수", requiredMode = Schema.RequiredMode.REQUIRED)
        long views;
    }

    @Getter
    @Builder
    @Schema(description = "글 하나의 통계")
    public static class PostViews implements Serializable {
        @Schema(description = "글 (전체 조회수·좋아요·댓글 수 포함)", requiredMode = Schema.RequiredMode.REQUIRED)
        SimplePostDto post;
        @Schema(description = "조회 기간 동안의 조회수", requiredMode = Schema.RequiredMode.REQUIRED)
        long recentViews;
    }

    @Getter
    @Builder
    @Schema(description = "내 글 통계")
    public static class Mine implements Serializable {
        @Schema(description = "조회 기간(일)", requiredMode = Schema.RequiredMode.REQUIRED)
        int days;
        @Schema(description = "기간 동안의 날짜별 조회수 (오래된 날부터, 빈 날은 0)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DailyViews> daily;
        @Schema(description = "글별 통계 (기간 조회수 많은 순)", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PostViews> posts;
    }

    @Getter
    @Builder
    @Schema(description = "검색어와 검색 횟수")
    public static class Keyword implements Serializable {
        @Schema(description = "검색어", requiredMode = Schema.RequiredMode.REQUIRED)
        String keyword;
        @Schema(description = "검색 횟수", requiredMode = Schema.RequiredMode.REQUIRED)
        long count;
        @Schema(description = "마지막으로 검색된 시각")
        LocalDateTime lastSearchedAt;
    }

    @Getter
    @Builder
    @Schema(description = "사이트 인사이트 (관리자용)")
    public static class Insights implements Serializable {
        @Schema(description = "조회 기간(일)", requiredMode = Schema.RequiredMode.REQUIRED)
        int days;
        @Schema(description = "기간 동안의 날짜별 전체 조회수", requiredMode = Schema.RequiredMode.REQUIRED)
        List<DailyViews> daily;
        @Schema(description = "기간 동안 많이 읽힌 글", requiredMode = Schema.RequiredMode.REQUIRED)
        List<PostViews> topPosts;
        @Schema(description = "많이 찾는 검색어", requiredMode = Schema.RequiredMode.REQUIRED)
        List<Keyword> topKeywords;
    }
}
