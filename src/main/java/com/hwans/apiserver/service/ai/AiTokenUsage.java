package com.hwans.apiserver.service.ai;

import com.anthropic.models.beta.messages.BetaServerToolUsage;
import com.anthropic.models.beta.messages.BetaUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Anthropic 호출의 토큰 사용량. 이어 받기(pause_turn)·보조 호출은 {@link #plus} 로 더한다.
 *
 * @param inputTokens      캐시에 걸리지 않은 입력 토큰
 * @param outputTokens     출력 토큰 (생각 토큰 포함)
 * @param cacheReadTokens  캐시에서 읽은 입력 토큰
 * @param cacheWriteTokens 캐시에 쓴 입력 토큰
 * @param webSearches      웹 검색 횟수
 */
public record AiTokenUsage(long inputTokens, long outputTokens, long cacheReadTokens, long cacheWriteTokens, int webSearches) {
    public static final AiTokenUsage ZERO = new AiTokenUsage(0, 0, 0, 0, 0);
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

    public static AiTokenUsage of(BetaUsage usage) {
        if (usage == null) {
            return ZERO;
        }
        return new AiTokenUsage(
                usage.inputTokens(),
                usage.outputTokens(),
                usage.cacheReadInputTokens().orElse(0L),
                usage.cacheCreationInputTokens().orElse(0L),
                (int) usage.serverToolUse().map(BetaServerToolUsage::webSearchRequests).orElse(0L).longValue());
    }

    public AiTokenUsage plus(AiTokenUsage other) {
        return new AiTokenUsage(inputTokens + other.inputTokens, outputTokens + other.outputTokens,
                cacheReadTokens + other.cacheReadTokens, cacheWriteTokens + other.cacheWriteTokens, webSearches + other.webSearches);
    }

    /**
     * 화면에 보여 줄 입력 토큰 합계 (캐시 읽기·쓰기 포함)
     */
    public long totalInputTokens() {
        return inputTokens + cacheReadTokens + cacheWriteTokens;
    }

    /**
     * 이 모델 가격으로 계산한 비용 (USD, 소수 6자리)
     */
    public BigDecimal costUsd(AiModel model) {
        var cost = perMillion(inputTokens, model.getInputPerMTok())
                .add(perMillion(outputTokens, model.getOutputPerMTok()))
                .add(perMillion(cacheReadTokens, model.getCacheReadPerMTok()))
                .add(perMillion(cacheWriteTokens, model.getCacheWritePerMTok()))
                .add(BigDecimal.valueOf(AiModel.WEB_SEARCH_USD).multiply(BigDecimal.valueOf(webSearches)));
        return cost.setScale(6, RoundingMode.HALF_UP);
    }

    private static BigDecimal perMillion(long tokens, double pricePerMillion) {
        return BigDecimal.valueOf(tokens).multiply(BigDecimal.valueOf(pricePerMillion)).divide(MILLION, 10, RoundingMode.HALF_UP);
    }
}
