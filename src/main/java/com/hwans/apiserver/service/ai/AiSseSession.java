package com.hwans.apiserver.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AI 응답 하나의 SSE 연결.
 * <ul>
 *   <li>이벤트: {@code status}·{@code delta}·{@code done}·{@code error} (data 는 JSON), 15초마다 {@code : ping}</li>
 *   <li>여러 스레드(작업 스레드, ping)가 쓰므로 쓰기는 잠금으로 직렬화한다.</li>
 *   <li>클라이언트가 끊으면(쓰기 실패·컨테이너 오류·타임아웃) {@link #isCancelled()} 가 참이 되고, 붙여 둔 Anthropic 스트림을 닫는다.</li>
 * </ul>
 * {@code ResponseBodyEmitter} 에 미리 만든 UTF-8 바이트를 그대로 보낸다(SseEmitter 는 "event:" 뒤에 공백을 넣지 않아 계약서 형식과 다르다).
 */
@Slf4j
public class AiSseSession {
    public static final MediaType EVENT_STREAM = new MediaType("text", "event-stream", StandardCharsets.UTF_8);
    public static final long PING_INTERVAL_SECONDS = 15;

    /**
     * 프레임을 실제로 내보내는 곳 (운영은 ResponseBodyEmitter, 테스트는 목록)
     */
    public interface FrameSink {
        void write(String frame) throws IOException;

        void complete();
    }

    private final ObjectMapper objectMapper;
    private final FrameSink sink;
    private final ResponseBodyEmitter emitter;
    private final ReentrantLock writeLock = new ReentrantLock();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicReference<AutoCloseable> upstream = new AtomicReference<>();
    private volatile ScheduledFuture<?> pingTask;

    public AiSseSession(ObjectMapper objectMapper, FrameSink sink) {
        this(objectMapper, sink, null);
    }

    private AiSseSession(ObjectMapper objectMapper, FrameSink sink, ResponseBodyEmitter emitter) {
        this.objectMapper = objectMapper;
        this.sink = sink;
        this.emitter = emitter;
    }

    /**
     * 서블릿 응답용 세션을 만든다.
     *
     * @param timeoutMillis 연결 최대 유지 시간 (이 시간이 지나면 끊고 Anthropic 스트림도 닫는다)
     */
    public static AiSseSession forEmitter(ObjectMapper objectMapper, long timeoutMillis) {
        var emitter = new ResponseBodyEmitter(timeoutMillis);
        var sink = new FrameSink() {
            @Override
            public void write(String frame) throws IOException {
                emitter.send(frame.getBytes(StandardCharsets.UTF_8), EVENT_STREAM);
            }

            @Override
            public void complete() {
                emitter.complete();
            }
        };
        var session = new AiSseSession(objectMapper, sink, emitter);
        emitter.onTimeout(session::cancel);
        emitter.onError(e -> session.cancel());
        emitter.onCompletion(session::cancel);
        return session;
    }

    /**
     * 요청 스레드에서 부른다. ResponseEntity 의 헤더는 첫 이벤트를 쓸 때에야 응답에 붙는데, 그 전에 요청 스레드가 필터를
     * 빠져나가며 Spring Security 가 자기 Cache-Control(no-store…)을 먼저 써 헤더가 둘이 된다. 그래서 Cache-Control 은
     * 서블릿 응답에 바로 써 두고(Security 는 이미 있으면 건너뛴다) 나머지만 ResponseEntity 로 넘긴다.
     */
    public ResponseEntity<ResponseBodyEmitter> toResponseEntity() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes && attributes.getResponse() != null) {
            attributes.getResponse().setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        }
        return ResponseEntity.ok()
                .contentType(EVENT_STREAM)
                .header("X-Accel-Buffering", "no")
                .body(emitter);
    }

    public void startPing(ScheduledExecutorService scheduler) {
        pingTask = scheduler.scheduleAtFixedRate(() -> {
                    if (finished.get() || cancelled.get()) {
                        stopPing();
                        return;
                    }
                    write(SseFormat.comment("ping"));
                },
                PING_INTERVAL_SECONDS, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
        // 작업이 이미 끝났으면(아주 짧은 응답) 바로 멈춘다
        if (finished.get() || cancelled.get()) {
            stopPing();
        }
    }

    public void status(String message) {
        event("status", Map.of("message", message));
    }

    public void delta(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        event("delta", Map.of("text", text));
    }

    /**
     * 커버 일괄 만들기의 글 하나 결과
     */
    public void item(Map<String, Object> data) {
        event("item", data);
    }

    /**
     * {@code done} 을 보내고 연결을 끝낸다.
     */
    public void done(Map<String, Object> payload) {
        event("done", payload);
        complete();
    }

    /**
     * {@code error} 를 보내고 연결을 끝낸다.
     */
    public void error(String code, String message) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("code", code);
        payload.put("message", message);
        event("error", payload);
        complete();
    }

    /**
     * 클라이언트가 연결을 끊었거나(취소) 타임아웃으로 닫혔는지
     */
    public boolean isCancelled() {
        return cancelled.get();
    }

    /**
     * 지금 읽고 있는 Anthropic 스트림. 클라이언트가 끊으면 닫는다.
     */
    public void attachUpstream(AutoCloseable stream) {
        upstream.set(stream);
        if (cancelled.get()) {
            closeQuietly(upstream.getAndSet(null));
        }
    }

    public void detachUpstream() {
        upstream.set(null);
    }

    public void complete() {
        if (finished.compareAndSet(false, true)) {
            stopPing();
            writeLock.lock();
            try {
                sink.complete();
            } catch (Exception e) {
                log.debug("SSE complete failed: {}", e.getMessage());
            } finally {
                writeLock.unlock();
            }
        }
    }

    private void cancel() {
        if (!finished.get()) {
            cancelled.set(true);
        }
        stopPing();
        closeQuietly(upstream.getAndSet(null));
    }

    private void event(String name, Object data) {
        try {
            write(SseFormat.event(name, objectMapper.writeValueAsString(data)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize SSE data", e);
        }
    }

    private void write(String frame) {
        if (finished.get() || cancelled.get()) {
            return;
        }
        writeLock.lock();
        try {
            if (finished.get() || cancelled.get()) {
                return;
            }
            sink.write(frame);
        } catch (IOException | IllegalStateException e) {
            // 클라이언트가 끊음 (또는 이미 닫힌 응답)
            log.debug("SSE write failed, treating as client disconnect: {}", e.getMessage());
            cancelled.set(true);
            stopPing();
            closeQuietly(upstream.getAndSet(null));
        } finally {
            writeLock.unlock();
        }
    }

    private void stopPing() {
        var task = pingTask;
        if (task != null) {
            task.cancel(false);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log.debug("closing upstream stream failed: {}", e.getMessage());
        }
    }
}
