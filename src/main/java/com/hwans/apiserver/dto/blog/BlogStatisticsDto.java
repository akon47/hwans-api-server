package com.hwans.apiserver.dto.blog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 블로그 작성자용 통계 Dto
 */
@Getter
@Builder
@Schema(description = "블로그 작성자용 통계 Dto")
public class BlogStatisticsDto implements Serializable {
    @Schema(description = "전체 게시글 수(삭제 제외)")
    long totalPosts;
    @Schema(description = "공개 게시글 수")
    long publicPosts;
    @Schema(description = "총 조회수")
    long totalHits;
    @Schema(description = "총 좋아요 수")
    long totalLikes;
    @Schema(description = "총 댓글 수")
    long totalComments;
    @Schema(description = "조회수 상위 게시글")
    List<SimplePostDto> popularPosts;
}
