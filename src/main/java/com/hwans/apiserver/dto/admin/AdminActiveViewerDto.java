package com.hwans.apiserver.dto.admin;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 관리자 실시간 접속자(시청자 1명) Dto.
 * "어떤 IP/호스트가 어떤 글을 보고 있는지"를 표 한 줄로 표현한다.
 */
@Getter
@Builder
@ApiModel(description = "관리자 실시간 접속자 Dto")
public class AdminActiveViewerDto implements Serializable {
    @ApiModelProperty(value = "클라이언트 IP", required = true, example = "211.45.1.2")
    String ip;
    @ApiModelProperty(value = "클라이언트 User-Agent")
    String userAgent;
    @ApiModelProperty(value = "웹소켓 접속 시각")
    LocalDateTime connectedAt;
    @ApiModelProperty(value = "로그인 사용자 이름(비회원이면 null)")
    String memberName;
    @ApiModelProperty(value = "로그인 사용자 이메일(비회원이면 null)")
    String memberEmail;
    @ApiModelProperty(value = "보고 있는 게시글 Id")
    UUID postId;
    @ApiModelProperty(value = "보고 있는 게시글 제목(비공개 등으로 조회 불가 시 null)")
    String postTitle;
    @ApiModelProperty(value = "게시글 URL(링크 생성용)")
    String postUrl;
    @ApiModelProperty(value = "게시글 작성자 블로그 Id(링크 생성용)")
    String blogId;
}
