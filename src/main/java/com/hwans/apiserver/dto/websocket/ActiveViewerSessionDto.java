package com.hwans.apiserver.dto.websocket;

import java.time.LocalDateTime;

/**
 * 현재 게시글을 보고 있는 웹소켓 세션 1개의 실시간 시청 정보(서버 내부 표현).
 * 게시글 제목/회원 이름 등은 여기에 없고, 어드민 서비스에서 postId/email로 조인하여 채운다.
 *
 * @param ip          클라이언트 IP
 * @param userAgent   클라이언트 User-Agent
 * @param connectedAt 웹소켓 연결(접속) 시각
 * @param postId      현재 보고 있는 게시글 Id(문자열)
 * @param email       로그인된 세션이면 사용자 이메일, 비회원이면 null
 */
public record ActiveViewerSessionDto(
        String ip,
        String userAgent,
        LocalDateTime connectedAt,
        String postId,
        String email
) {
}
