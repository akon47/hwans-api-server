package com.hwans.apiserver.dto.websocket;

/**
 * 한 글을 지금 읽고 있는 사람 한 명 (서버 -> 클라이언트)
 *
 * @param id    세션 Id (자기 자신을 가려내는 데 쓴다)
 * @param name  표시 이름 (로그인 사용자는 이름, 손님은 클라이언트가 정한 별명)
 * @param color 표시 색 (#rrggbb)
 */
public record PostViewerDto(String id, String name, String color) {
}
