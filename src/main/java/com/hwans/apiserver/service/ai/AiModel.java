package com.hwans.apiserver.service.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;

/**
 * 회원이 고를 수 있는 Claude 모델과 1M 토큰당 가격(USD).
 * 캐시 쓰기는 5분 TTL 가격이다(이 서버는 5분 캐시만 쓴다).
 */
@Getter
@RequiredArgsConstructor
public enum AiModel {
    OPUS("claude-opus-5-5", "Claude Opus 5.5", "가장 잘 써요(기본)", 4.0, 20.0, 0.20, 5.0, true),
    SONNET("claude-sonnet-5-5", "Claude Sonnet 5.5", "빠르고 저렴해요", 2.0, 10.0, 0.10, 2.5, true),
    // Haiku 5.5 는 서버 쪽 대체 모델(fallbacks)이 없다 — 보내면 거절이 그대로 남거나 400 이 난다.
    HAIKU("claude-haiku-5-5", "Claude Haiku 5.5", "아주 저렴해요(짧은 작업용)", 0.10, 0.50, 0.01, 0.125, false);

    public static final AiModel DEFAULT = OPUS;
    /**
     * 웹 검색 1회 비용 (USD)
     */
    public static final double WEB_SEARCH_USD = 0.01;

    private final String id;
    private final String label;
    private final String description;
    private final double inputPerMTok;
    private final double outputPerMTok;
    private final double cacheReadPerMTok;
    private final double cacheWritePerMTok;
    /**
     * 안전 분류기가 거절했을 때 서버 쪽 대체 모델로 다시 시도하는 기능(fallbacks: "default")을 쓸 수 있는지
     */
    private final boolean serverSideFallback;

    public static Optional<AiModel> fromId(String id) {
        return Arrays.stream(values()).filter(x -> x.id.equals(id)).findFirst();
    }

    public static AiModel fromIdOrDefault(String id) {
        return fromId(id).orElse(DEFAULT);
    }
}
