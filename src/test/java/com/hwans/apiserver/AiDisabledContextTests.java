package com.hwans.apiserver;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiDraftRequestDto;
import com.hwans.apiserver.service.ai.AiKeyCipher;
import com.hwans.apiserver.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 마스터 키가 없어도 서버는 뜨고, AI 기능만 꺼진다(available=false, 503 AI_DISABLED).
 * (임베디드 Redis 포트를 바꿔 다른 테스트 컨텍스트와 겹치지 않게 한다)
 */
@SpringBootTest(properties = {"spring.mail.host=localhost", "ai.encryption-key=", "spring.data.redis.port=6391"})
class AiDisabledContextTests {
    @Autowired
    private AiKeyCipher aiKeyCipher;
    @Autowired
    private AiService aiService;

    @Test
    void startsWithAiDisabled() {
        assertFalse(aiKeyCipher.isEnabled());

        var settings = aiService.getSettings(UUID.randomUUID());
        assertFalse(settings.isAvailable());
        assertFalse(settings.isConfigured());
        assertEquals("claude-opus-5-5", settings.getModel());
        assertEquals(10.0, settings.getMonthlyLimitUsd());
        assertEquals(3, settings.getModels().size());
        assertFalse(settings.getStyle().isAnalyzed());

        var error = assertThrows(RestApiException.class, () ->
                aiService.draft(UUID.randomUUID(), AiDraftRequestDto.builder().request("글").build()));
        assertEquals(ErrorCodes.ServiceUnavailable.AI_DISABLED, error.getErrorCode());
        assertEquals(503, error.getErrorCode().getStatus().value());
    }
}
