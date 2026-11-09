package com.hwans.apiserver.dto.websocket;

import java.util.List;

/**
 * 한 글을 지금 읽고 있는 사람들 (서버 -> 그 글을 읽는 세션들, 들어오거나 나갈 때마다)
 *
 * @param postId  게시글 Id
 * @param viewers 읽는 사람들 (먼저 들어온 순, 최대 개수 제한)
 * @param total   전체 읽는 사람 수 (viewers 에 다 담기지 않았을 수 있다)
 */
public record PostViewersDto(String postId, List<PostViewerDto> viewers, int total) {
}
