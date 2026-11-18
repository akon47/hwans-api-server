package com.hwans.apiserver.service.ai;

import com.anthropic.models.beta.messages.BetaStopReason;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSE 프레임이 계약서 형식({@code event: x\ndata: {...}\n\n}, {@code : ping})과 같은지
 */
class AiSseSessionTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final class ListSink implements AiSseSession.FrameSink {
        final List<String> frames = new ArrayList<>();
        boolean completed;
        boolean failWrites;

        @Override
        public void write(String frame) throws IOException {
            if (failWrites) {
                throw new IOException("Broken pipe");
            }
            frames.add(frame);
        }

        @Override
        public void complete() {
            completed = true;
        }
    }

    @Test
    void formatsEventsLikeTheContract() {
        var sink = new ListSink();
        var session = new AiSseSession(objectMapper, sink);

        session.status("웹에서 'CVE-2026-114938' 찾는 중");
        session.delta("첫 줄\n둘째 줄 \"따옴표\"");
        session.done(AiServiceImpl.donePayload(BetaStopReason.END_TURN, new AiTokenUsage(1000, 500, 2000, 3000, 2), AiModel.OPUS,
                List.of(new AiReferences.Source("NVD", "https://nvd.nist.gov/vuln/detail/CVE-2026-114938"))));

        assertEquals(List.of(
                "event: status\ndata: {\"message\":\"웹에서 'CVE-2026-114938' 찾는 중\"}\n\n",
                "event: delta\ndata: {\"text\":\"첫 줄\\n둘째 줄 \\\"따옴표\\\"\"}\n\n",
                "event: done\ndata: {\"stopReason\":\"end_turn\",\"usage\":{\"inputTokens\":6000,\"outputTokens\":500,\"costUsd\":0.0494},"
                        + "\"sources\":[{\"title\":\"NVD\",\"url\":\"https://nvd.nist.gov/vuln/detail/CVE-2026-114938\"}]}\n\n"
        ), sink.frames);
        assertTrue(sink.completed);

        // 끝난 뒤에는 아무것도 보내지 않는다
        session.delta("늦게 온 조각");
        assertEquals(3, sink.frames.size());
    }

    @Test
    void errorEventEndsTheStream() {
        var sink = new ListSink();
        var session = new AiSseSession(objectMapper, sink);
        session.error("AI_UPSTREAM", "AI 호출에 실패했어요.");
        assertEquals(List.of("event: error\ndata: {\"code\":\"AI_UPSTREAM\",\"message\":\"AI 호출에 실패했어요.\"}\n\n"), sink.frames);
        assertTrue(sink.completed);
    }

    @Test
    void pingAndMultilineDataFrames() {
        assertEquals(": ping\n\n", SseFormat.comment("ping"));
        assertEquals("event: x\ndata: a\ndata: b\n\n", SseFormat.event("x", "a\nb"));
    }

    @Test
    void writeFailureMeansClientGoneAndClosesUpstream() {
        var sink = new ListSink();
        var session = new AiSseSession(objectMapper, sink);
        var closed = new boolean[1];
        session.attachUpstream(() -> closed[0] = true);
        sink.failWrites = true;

        session.delta("조각");

        assertTrue(session.isCancelled());
        assertTrue(closed[0], "Anthropic stream must be closed when the client goes away");
        // 이미 취소된 뒤 붙이는 스트림은 바로 닫는다
        var lateClosed = new boolean[1];
        session.attachUpstream(() -> lateClosed[0] = true);
        assertTrue(lateClosed[0]);
    }
}
