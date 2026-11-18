package com.hwans.apiserver.service.ai;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaWebFetchTool20260209;
import com.anthropic.models.beta.messages.BetaWebSearchTool20260209;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 API 를 부르지 않고, SDK 가 만드는 요청 본문·헤더가 의도대로인지 확인한다.
 */
class AnthropicGatewayTest {
    private static JsonNode body(com.anthropic.models.beta.messages.MessageCreateParams params) {
        return ObjectMappers.jsonMapper().valueToTree(params._body());
    }

    @Test
    void opusDraftRequestShape() {
        var params = AnthropicGateway.baseParams(AiModel.OPUS, 20000, AnthropicGateway.effort(BetaOutputConfig.Effort.MEDIUM))
                .systemOfBetaTextBlockParams(AnthropicGateway.cachedSystem("지침"))
                .addTool(BetaWebSearchTool20260209.builder().maxUses(5L).build())
                .addTool(BetaWebFetchTool20260209.builder().maxUses(5L).build())
                .addUserMessage("쓰기")
                .build();
        var json = body(params);

        assertEquals("claude-opus-5-5", json.path("model").asText());
        assertEquals(20000, json.path("max_tokens").asInt());
        assertEquals("medium", json.path("output_config").path("effort").asText());
        assertFalse(json.has("thinking"), "Opus 5.5 always thinks; do not send thinking");
        assertFalse(json.has("tool_choice"));
        assertEquals("default", json.path("fallbacks").asText());
        assertEquals("ephemeral", json.path("system").get(0).path("cache_control").path("type").asText());
        assertEquals("web_search_20260209", json.path("tools").get(0).path("type").asText());
        assertEquals(5, json.path("tools").get(0).path("max_uses").asInt());
        assertEquals("web_fetch_20260209", json.path("tools").get(1).path("type").asText());
        assertTrue(params._headers().values("anthropic-beta").stream().anyMatch(v -> v.contains(AnthropicGateway.FALLBACK_BETA)),
                "fallback beta header: " + params._headers().values("anthropic-beta"));
    }

    @Test
    void haikuHasNoFallback() {
        var params = AnthropicGateway.baseParams(AiModel.HAIKU, 1000, AnthropicGateway.effort(BetaOutputConfig.Effort.LOW))
                .addUserMessage("x")
                .build();
        var json = body(params);
        assertFalse(json.has("fallbacks"));
        assertEquals("low", json.path("output_config").path("effort").asText());
        assertTrue(params._headers().values("anthropic-beta").stream().noneMatch(v -> v.contains(AnthropicGateway.FALLBACK_BETA)));
    }

    @Test
    void structuredOutputSchema() {
        var schema = Map.<String, Object>of(
                "type", "object",
                "properties", Map.of("theme", Map.of("type", "string", "enum", List.of("slate", "violet"))),
                "required", List.of("theme"),
                "additionalProperties", false);
        var params = AnthropicGateway.baseParams(AiModel.SONNET, 2000, AnthropicGateway.jsonOutput(BetaOutputConfig.Effort.LOW, schema))
                .addUserMessage("x")
                .build();
        var format = body(params).path("output_config").path("format");
        assertEquals("json_schema", format.path("type").asText());
        assertFalse(format.path("schema").path("additionalProperties").asBoolean(true));
        assertEquals("violet", format.path("schema").path("properties").path("theme").path("enum").get(1).asText());
        assertEquals("default", body(params).path("fallbacks").asText());
    }

    @Test
    void clientCanBeBuiltOnThisClasspath() {
        // Spring Boot 가 관리하는 Jackson/Kotlin 버전과 SDK 가 맞는지 (호출은 하지 않음)
        var client = new AnthropicGateway().createClient("sk-ant-test-not-a-real-key");
        assertNotNull(client.beta().messages());
        client.close();
    }
}
