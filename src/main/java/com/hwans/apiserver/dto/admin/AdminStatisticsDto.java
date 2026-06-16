package com.hwans.apiserver.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 관리자 대시보드 통계 Dto
 */
@Getter
@Builder
@ApiModel(description = "관리자 대시보드 통계 Dto")
public class AdminStatisticsDto implements Serializable {
    @ApiModelProperty(value = "활성 회원 수", required = true)
    long memberCount;
    @ApiModelProperty(value = "전체 게시글 수", required = true)
    long postCount;
    @ApiModelProperty(value = "전체 댓글 수", required = true)
    long commentCount;
}
