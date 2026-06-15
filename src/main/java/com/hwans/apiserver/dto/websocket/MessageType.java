package com.hwans.apiserver.dto.websocket;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 웹소켓 이벤트 유형
 */
@Getter
@RequiredArgsConstructor
public enum MessageType {
    // 서버 -> 클라이언트: 전체 접속 세션 수가 변경됨
    SESSION_COUNT_CHANGED,
    // 클라이언트 -> 서버: 현재 보고 있는 게시글을 지정(payload: 게시글 Id, 비우면 보지 않음)
    VIEW_POST,
    // 클라이언트 -> 서버: 시청자 수를 알고 싶은 게시글 목록 요청(payload: 게시글 Id 배열)
    WATCH_POSTS,
    // 서버 -> 클라이언트: 특정 게시글의 현재 시청자 수가 변경됨(payload: PostViewerCountDto)
    POST_VIEWER_COUNT_CHANGED,
    // 서버 -> 클라이언트: 요청한 게시글들의 현재 시청자 수 스냅샷(payload: PostViewerCountDto 배열)
    POST_VIEWER_COUNTS
}
