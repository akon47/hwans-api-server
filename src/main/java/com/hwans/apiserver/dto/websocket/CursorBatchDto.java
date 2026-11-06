package com.hwans.apiserver.dto.websocket;

import java.util.List;

/**
 * 한 글의 커서 상태 묶음 (서버 -> 클라이언트, 약 100ms 마다 한 번에 보낸다)
 *
 * @param postId   게시글 Id
 * @param cursors  현재 커서들 (최근에 움직인 순, 최대 개수 제한)
 * @param left     글을 떠난 세션 Id 목록
 * @param disabled 같은 글을 읽는 사람이 너무 많아 커서 공유를 끈 상태인지
 */
public record CursorBatchDto(String postId, List<CursorStateDto> cursors, List<String> left, boolean disabled) {
}
