package com.hwans.apiserver.dto.websocket;

/**
 * 같은 글을 읽는 사람의 마우스 커서 상태 (서버 -> 클라이언트, CURSOR_BATCH 안에 담긴다)
 *
 * @param id        세션 Id (받는 쪽은 자기 커서를 이 값으로 걸러낸다)
 * @param x         글 본문 영역 기준 가로 위치 (0~1)
 * @param y         글 본문 영역 기준 세로 위치 (0~1)
 * @param name      표시 이름
 * @param color     커서 색 (#rrggbb)
 * @param message   커서 옆에 띄우는 짧은 말 (없으면 null)
 * @param messageAt 말을 보낸 시각 (epoch ms)
 * @param updatedAt 마지막으로 움직인 시각 (epoch ms)
 */
public record CursorStateDto(String id, double x, double y, String name, String color, String message, long messageAt, long updatedAt) {
}
