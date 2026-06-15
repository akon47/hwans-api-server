package com.hwans.apiserver.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 특정 게시글의 현재 시청자 수를 전달하기 위한 Dto
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PostViewerCountDto {
    // 게시글 Id
    private String postId;
    // 현재 해당 게시글을 보고 있는 시청자 수
    private int count;
}
