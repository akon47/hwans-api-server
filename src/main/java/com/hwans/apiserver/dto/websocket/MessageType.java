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
    // 클라이언트 -> 서버: 현재 보고 있는 게시글을 지정
    // (payload: 게시글 Id 문자열 또는 {postId, name, color}; 비우면 보지 않음. name·color 는 읽는 사람 목록에 쓴다)
    VIEW_POST,
    // 클라이언트 -> 서버: 시청자 수를 알고 싶은 게시글 목록 요청(payload: 게시글 Id 배열)
    WATCH_POSTS,
    // 클라이언트 -> 서버: 현재 세션을 로그인 사용자와 연결(payload: 액세스 토큰 문자열)
    AUTHENTICATE,
    // 클라이언트 -> 서버: 세션 생존 신호(payload 없음). 네트워크 전환 등으로 끊긴 유령 세션을
    // 리퍼가 수거할 수 있도록, 받은 시각으로 세션의 lastSeen을 갱신하는 용도.
    PING,
    // 서버 -> 클라이언트: 특정 게시글의 현재 시청자 수가 변경됨(payload: PostViewerCountDto)
    POST_VIEWER_COUNT_CHANGED,
    // 서버 -> 클라이언트: 요청한 게시글들의 현재 시청자 수 스냅샷(payload: PostViewerCountDto 배열)
    POST_VIEWER_COUNTS,
    // 서버 -> 클라이언트: 인증된 사용자에게 새 알림이 도착함(payload: NotificationDto)
    NOTIFICATION,
    // 클라이언트 -> 서버: 지금 보는 글 위의 내 마우스 위치(payload: {x, y, name, color}; x,y 는 본문 영역 기준 0~1)
    CURSOR_MOVE,
    // 클라이언트 -> 서버: 내 커서 옆에 띄울 짧은 말(payload: {message}; 빈 문자열이면 지움)
    CURSOR_CHAT,
    // 클라이언트 -> 서버: 마우스가 창을 벗어나거나 탭이 숨겨져 내 커서를 당장 지워 달라는 요청(payload 없음)
    CURSOR_LEAVE,
    // 서버 -> 클라이언트: 이 세션의 Id(payload: 문자열). 커서 묶음에서 자기 커서를 걸러내는 데 쓴다
    CURSOR_SELF,
    // 서버 -> 클라이언트: 같은 글을 읽는 사람들의 커서 묶음(payload: CursorBatchDto). 약 100ms 마다 한 번
    CURSOR_BATCH,
    // 서버 -> 클라이언트: 지금 보는 글을 함께 읽고 있는 사람들(payload: PostViewersDto). 누가 들어오거나 나갈 때마다
    POST_VIEWERS,
    // 클라이언트 -> 서버: 지금 보는 글에서 내가 읽고 있는 위치(payload: 본문 시작 0 ~ 끝 1 의 숫자)
    READ_POSITION,
    // 서버 -> 클라이언트: 같은 글을 읽는 사람들의 읽는 위치(payload: ReadPositionsDto). 바뀐 것이 있을 때 묶어서
    READ_POSITIONS
}
