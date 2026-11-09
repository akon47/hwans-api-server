package com.hwans.apiserver.dto.websocket;

import java.util.List;

/**
 * 한 글을 읽는 사람들이 지금 읽고 있는 위치 (서버 -> 그 글을 읽는 세션들, 바뀐 것이 있을 때 묶어서)
 *
 * @param postId    게시글 Id
 * @param positions 읽는 사람별 위치 (읽는 사람 목록에 보이는 사람들만)
 */
public record ReadPositionsDto(String postId, List<Position> positions) {
    /**
     * @param id       세션 Id
     * @param progress 본문 시작(0)부터 끝(1)까지의 비율
     */
    public record Position(String id, double progress) {
    }
}
