package com.hwans.apiserver.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * 관리자 실시간 접속자 목록 응답 Dto.
 */
@Getter
@Builder
@ApiModel(description = "관리자 실시간 접속자 목록 Dto")
public class AdminActiveViewersDto implements Serializable {
    @ApiModelProperty(value = "현재 연결된 전체 웹소켓 세션 수", required = true)
    int totalSessions;
    @ApiModelProperty(value = "현재 게시글을 보고 있는 시청자 목록", required = true)
    List<AdminActiveViewerDto> viewers;
}
