package com.hwans.apiserver.entity.blog;

// 게시글 공개 유형
public enum OpenType {
    // 전체 공개
    PUBLIC,
    // 비공개
    PRIVATE,
    // 임시저장 (작성자만 볼 수 있음)
    DRAFT,
    // 예약 발행 (scheduledAt 도달 시 PUBLIC 으로 전환됨)
    SCHEDULED
}
