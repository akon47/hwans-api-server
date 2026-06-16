package com.hwans.apiserver.dto.blog;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 블로그 작성자용 통계 Dto
 */
@Getter
@Builder
@ApiModel(description = "블로그 작성자용 통계 Dto")
public class BlogStatisticsDto implements Serializable {
    @ApiModelProperty(value = "전체 게시글 수(삭제 제외)")
    long totalPosts;
    @ApiModelProperty(value = "공개 게시글 수")
    long publicPosts;
    @ApiModelProperty(value = "총 조회수")
    long totalHits;
    @ApiModelProperty(value = "총 좋아요 수")
    long totalLikes;
    @ApiModelProperty(value = "총 댓글 수")
    long totalComments;
    @ApiModelProperty(value = "조회수 상위 게시글")
    List<SimplePostDto> popularPosts;
}
