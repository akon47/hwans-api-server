package com.hwans.apiserver.service.ai;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.beta.messages.BetaUsage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiTokenUsageTest {
    @Test
    void opusCost() {
        // 입력 1000×4 + 출력 500×20 + 캐시 읽기 2000×0.2 + 캐시 쓰기 3000×5 (/1M) + 검색 2×0.01
        var usage = new AiTokenUsage(1000, 500, 2000, 3000, 2);
        assertEquals(new BigDecimal("0.049400"), usage.costUsd(AiModel.OPUS));
        assertEquals(6000, usage.totalInputTokens());
    }

    @Test
    void sonnetAndHaikuCost() {
        var usage = new AiTokenUsage(1_000_000, 1_000_000, 1_000_000, 1_000_000, 0);
        assertEquals(new BigDecimal("14.600000"), usage.costUsd(AiModel.SONNET)); // 2 + 10 + 0.1 + 2.5
        assertEquals(new BigDecimal("0.735000"), usage.costUsd(AiModel.HAIKU)); // 0.1 + 0.5 + 0.01 + 0.125
    }

    @Test
    void webSearchOnly() {
        assertEquals(new BigDecimal("0.050000"), new AiTokenUsage(0, 0, 0, 0, 5).costUsd(AiModel.HAIKU));
    }

    @Test
    void plusAddsEveryField() {
        var total = new AiTokenUsage(1, 2, 3, 4, 5).plus(new AiTokenUsage(10, 20, 30, 40, 50));
        assertEquals(new AiTokenUsage(11, 22, 33, 44, 55), total);
    }

    @Test
    void readsSdkUsage() throws Exception {
        var usage = ObjectMappers.jsonMapper().readValue("""
                {"input_tokens": 120, "output_tokens": 340, "cache_creation_input_tokens": 50, "cache_read_input_tokens": 7,
                 "server_tool_use": {"web_search_requests": 3, "web_fetch_requests": 1}}""", BetaUsage.class);
        assertEquals(new AiTokenUsage(120, 340, 7, 50, 3), AiTokenUsage.of(usage));
    }
}
