package com.hwans.apiserver.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "관리자 실시간 접속자 Dto")
public class AdminActiveViewerDto implements Serializable {
    @Schema(description = "클라이언트 IP", requiredMode = Schema.RequiredMode.REQUIRED, example = "211.45.1.2")
    String ip;
    @Schema(description = "클라이언트 User-Agent")
    String userAgent;
    @Schema(description = "웹소켓 접속 시각")
    LocalDateTime connectedAt;
    @Schema(description = "로그인 사용자 이름(비회원이면 null)")
    String memberName;
    @Schema(description = "로그인 사용자 이메일(비회원이면 null)")
    String memberEmail;
    @Schema(description = "보고 있는 게시글 Id")
    UUID postId;
    @Schema(description = "보고 있는 게시글 제목(비공개 등으로 조회 불가 시 null)")
    String postTitle;
    @Schema(description = "게시글 URL(링크 생성용)")
    String postUrl;
    @Schema(description = "게시글 작성자 블로그 Id(링크 생성용)")
    String blogId;
}
