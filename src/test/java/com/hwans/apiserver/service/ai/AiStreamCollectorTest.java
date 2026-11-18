package com.hwans.apiserver.service.ai;

import com.anthropic.core.ObjectMappers;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.beta.messages.BetaOutputConfig;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.anthropic.models.beta.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 API 와 같은 모양의 스트림 이벤트(JSON)를 SDK 로 읽어 넣어 본다. (네트워크 없이 SDK 역직렬화·누적기까지 확인)
 */
class AiStreamCollectorTest {
    private static final String MESSAGE_START = """
            {"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","model":"claude-opus-5-5",
             "content":[],"stop_reason":null,"stop_sequence":null,"container":null,"context_management":null,
             "usage":{"input_tokens":1200,"output_tokens":1,"cache_creation_input_tokens":3000,"cache_read_input_tokens":0}}}""";

    private static final List<String> RESEARCH_EVENTS = List.of(
            MESSAGE_START,
            // 도구 호출 앞 머리말 → 버려야 한다
            """
                    {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
            """
                    {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"CVE 를 검색해 볼게요."}}""",
            """
                    {"type":"content_block_stop","index":0}""",
            """
                    {"type":"content_block_start","index":1,"content_block":{"type":"server_tool_use","id":"srvtoolu_1","name":"web_search","input":{}}}""",
            """
                    {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"{\\"query\\": \\"CVE-2026-"}}""",
            """
                    {"type":"content_block_delta","index":1,"delta":{"type":"input_json_delta","partial_json":"114938\\"}"}}""",
            """
                    {"type":"content_block_stop","index":1}""",
            """
                    {"type":"content_block_start","index":2,"content_block":{"type":"web_search_tool_result","tool_use_id":"srvtoolu_1",
                     "content":[{"type":"web_search_result","title":"NVD - CVE-2026-114938","url":"https://nvd.nist.gov/vuln/detail/CVE-2026-114938","encrypted_content":"e1","page_age":null},
                                {"type":"web_search_result","title":"Some blog","url":"https://example.com/blog","encrypted_content":"e2","page_age":null}]}}""",
            """
                    {"type":"content_block_stop","index":2}""",
            """
                    {"type":"content_block_start","index":3,"content_block":{"type":"server_tool_use","id":"srvtoolu_2","name":"web_fetch","input":{}}}""",
            """
                    {"type":"content_block_delta","index":3,"delta":{"type":"input_json_delta","partial_json":"{\\"url\\":\\"https://github.com/advisories/GHSA-abcd-1234\\"}"}}""",
            """
                    {"type":"content_block_stop","index":3}""",
            """
                    {"type":"content_block_start","index":4,"content_block":{"type":"web_fetch_tool_result","tool_use_id":"srvtoolu_2",
                     "content":{"type":"web_fetch_result","url":"https://github.com/advisories/GHSA-abcd-1234","retrieved_at":"2026-11-18T00:00:00Z",
                                "content":{"type":"document","source":{"type":"text","media_type":"text/plain","data":"advisory"},"title":"GitHub Advisory GHSA-abcd-1234","citations":null}}}}""",
            """
                    {"type":"content_block_stop","index":4}""",
            // 본문: 맨 위 # 제목은 버리고, 인용 때문에 나뉜 글 블록은 이어 붙인다
            """
                    {"type":"content_block_start","index":5,"content_block":{"type":"text","text":""}}""",
            """
                    {"type":"content_block_delta","index":5,"delta":{"type":"text_delta","text":"# 자바 취약점\\n\\n"}}""",
            """
                    {"type":"content_block_delta","index":5,"delta":{"type":"text_delta","text":"요즘 자바에서 "}}""",
            """
                    {"type":"content_block_stop","index":5}""",
            """
                    {"type":"content_block_start","index":6,"content_block":{"type":"text","text":"","citations":[]}}""",
            """
                    {"type":"content_block_delta","index":6,"delta":{"type":"citations_delta","citation":{"type":"web_search_result_location","cited_text":"...","url":"https://nvd.nist.gov/vuln/detail/CVE-2026-114938","title":"NVD - CVE-2026-114938","encrypted_index":"x"}}}""",
            """
                    {"type":"content_block_delta","index":6,"delta":{"type":"text_delta","text":"심각한 취약점이 나왔어요."}}""",
            """
                    {"type":"content_block_stop","index":6}""",
            """
                    {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},
                     "usage":{"input_tokens":1200,"output_tokens":850,"cache_creation_input_tokens":3000,"cache_read_input_tokens":0,
                              "server_tool_use":{"web_search_requests":1,"web_fetch_requests":1}}}""",
            """
                    {"type":"message_stop"}"""
    );

    private static BetaRawMessageStreamEvent parse(String json) {
        try {
            return ObjectMappers.jsonMapper().readValue(json, BetaRawMessageStreamEvent.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class Recorder implements AiStreamCollector.Listener {
        final List<String> statuses = new ArrayList<>();
        final StringBuilder text = new StringBuilder();

        @Override
        public void status(String message) {
            statuses.add(message);
        }

        @Override
        public void text(String text) {
            this.text.append(text);
        }
    }

    @Test
    void webResearchStream() {
        var recorder = new Recorder();
        var collector = new AiStreamCollector(recorder, true, true);
        collector.beginMessage();
        RESEARCH_EVENTS.forEach(json -> collector.accept(parse(json)));
        var message = collector.endMessage();
        collector.finish();

        assertEquals(BetaStopReason.END_TURN, message.stopReason().orElseThrow());
        assertEquals(List.of(
                "웹에서 'CVE-2026-114938' 찾는 중",
                "'github.com/advisories/GHSA-abcd-1234' 읽는 중"), recorder.statuses);
        assertEquals("요즘 자바에서 심각한 취약점이 나왔어요.", recorder.text.toString());
        assertEquals(recorder.text.toString(), collector.body());
        assertEquals(new AiTokenUsage(1200, 850, 0, 3000, 1), collector.usage());

        // 인용한 것과 읽은 페이지가 먼저, 그 밖의 검색 결과는 빠진다
        assertEquals(List.of(
                new AiReferences.Source("NVD - CVE-2026-114938", "https://nvd.nist.gov/vuln/detail/CVE-2026-114938"),
                new AiReferences.Source("GitHub Advisory GHSA-abcd-1234", "https://github.com/advisories/GHSA-abcd-1234")
        ), collector.collectedSources(10));
        assertEquals(2, collector.searchedSources().size());
    }

    @Test
    void longTextBeforeAnyToolIsBody() {
        var recorder = new Recorder();
        var collector = new AiStreamCollector(recorder, true, true);
        collector.beginMessage();
        collector.accept(parse(MESSAGE_START));
        collector.accept(parse("""
                {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}"""));
        var longText = "가".repeat(AiStreamCollector.PREAMBLE_LIMIT + 10);
        collector.accept(parse("""
                {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"%s"}}""".formatted(longText)));
        // 머리말 한도를 넘으면 바로 흘려보낸다(제목 판단은 300자까지 기다림)
        collector.finish();
        assertEquals(longText, recorder.text.toString());
    }

    @Test
    void stripsOnlyALeadingH1() {
        assertEquals("본문", AiStreamCollector.decideTitle("# 제목\n\n\n본문"));
        assertEquals("## 소제목\n본문", AiStreamCollector.decideTitle("## 소제목\n본문"));
        assertEquals("#태그 같은 글\n본문", AiStreamCollector.decideTitle("#태그 같은 글\n본문"));
        assertEquals("", AiStreamCollector.decideTitle("# 제목만"));
    }

    @Test
    void continuesAfterPauseTurn() {
        var first = List.of(
                MESSAGE_START,
                """
                        {"type":"content_block_start","index":0,"content_block":{"type":"server_tool_use","id":"srvtoolu_1","name":"web_search","input":{}}}""",
                """
                        {"type":"content_block_delta","index":0,"delta":{"type":"input_json_delta","partial_json":"{\\"query\\":\\"java cve\\"}"}}""",
                """
                        {"type":"content_block_stop","index":0}""",
                """
                        {"type":"message_delta","delta":{"stop_reason":"pause_turn","stop_sequence":null},"usage":{"output_tokens":40,"server_tool_use":{"web_search_requests":1,"web_fetch_requests":0}}}""",
                """
                        {"type":"message_stop"}""");
        var second = List.of(
                MESSAGE_START.replace("msg_1", "msg_2"),
                """
                        {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}""",
                """
                        {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"본문이에요.\\n"}}""",
                """
                        {"type":"content_block_stop","index":0}""",
                """
                        {"type":"message_delta","delta":{"stop_reason":"end_turn","stop_sequence":null},"usage":{"output_tokens":60}}""",
                """
                        {"type":"message_stop"}""");
        var scripts = List.of(first, second);
        var sentParams = new ArrayList<MessageCreateParams>();

        var initial = AnthropicGateway.baseParams(AiModel.OPUS, 1000, AnthropicGateway.effort(BetaOutputConfig.Effort.MEDIUM))
                .addUserMessage("쓰기")
                .build();
        var recorder = new Recorder();
        var collector = new AiStreamCollector(recorder, true, true);
        var session = new AiSseSession(new ObjectMapper(), new AiSseSession.FrameSink() {
            @Override
            public void write(String frame) {
            }

            @Override
            public void complete() {
            }
        });

        var stopReason = AiServiceImpl.streamWithContinuation(params -> {
            var events = scripts.get(sentParams.size()).stream().map(AiStreamCollectorTest::parse).toList();
            sentParams.add(params);
            return new StreamResponse<>() {
                @Override
                public Stream<BetaRawMessageStreamEvent> stream() {
                    return events.stream();
                }

                @Override
                public void close() {
                }
            };
        }, initial, collector, session);
        collector.finish();

        assertEquals(BetaStopReason.END_TURN, stopReason);
        assertEquals(2, sentParams.size());
        // 두 번째 요청: [user, assistant(첫 응답 내용)] — "계속" 같은 사용자 메시지를 더하지 않는다
        var messages = sentParams.get(1).messages();
        assertEquals(2, messages.size());
        assertTrue(messages.get(0).isUser());
        assertTrue(messages.get(1).isAssistant());
        var assistantJson = ObjectMappers.jsonMapper().valueToTree(messages.get(1)).toString();
        assertTrue(assistantJson.contains("server_tool_use") && assistantJson.contains("java cve"), assistantJson);
        assertEquals("본문이에요.\n", recorder.text.toString());
        assertEquals(new AiTokenUsage(2400, 100, 0, 6000, 1), collector.usage());
    }
}
