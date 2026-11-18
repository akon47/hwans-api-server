package com.hwans.apiserver.service.ai;

import com.anthropic.core.JsonValue;
import com.anthropic.core.ObjectMappers;
import com.anthropic.helpers.BetaMessageAccumulator;
import com.anthropic.models.beta.messages.BetaInputJsonDelta;
import com.anthropic.models.beta.messages.BetaMessage;
import com.anthropic.models.beta.messages.BetaRawContentBlockDeltaEvent;
import com.anthropic.models.beta.messages.BetaRawContentBlockStartEvent;
import com.anthropic.models.beta.messages.BetaRawMessageDeltaEvent;
import com.anthropic.models.beta.messages.BetaRawMessageStreamEvent;
import com.anthropic.models.beta.messages.BetaServerToolUsage;
import com.anthropic.models.beta.messages.BetaStopReason;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Anthropic 스트림 이벤트를 받아 본문 조각·진행 상황·출처·사용량을 모은다(네트워크와 무관해 단위 테스트할 수 있다).
 * <ul>
 *   <li>본문: text 블록의 text_delta. 웹 조사 모드에서는 첫 본문이 확정되기 전의 짧은 글(“검색해 볼게요” 같은 머리말)을
 *       잠시 붙잡아 두었다가, 바로 뒤에 서버 도구 호출이 오면 버린다(본문에 섞이지 않게).</li>
 *   <li>맨 위 {@code # 제목} 줄은 버린다(계약: 본문만).</li>
 *   <li>진행 상황: web_search 검색어, web_fetch 주소(입력은 input_json_delta 로 조금씩 오므로 블록이 끝날 때 읽는다).</li>
 *   <li>출처: 본문 인용(citations)의 검색 결과, 읽은 페이지, 검색 결과.</li>
 *   <li>사용량: message_start / message_delta 의 usage(누적값)로 계산 — 중간에 끊겨도 그때까지의 사용량을 안다.</li>
 * </ul>
 * 한 번의 요청이 pause_turn 으로 여러 번 이어지면 {@link #beginMessage()} ~ {@link #endMessage()} 를 반복한다.
 */
@Slf4j
public class AiStreamCollector {
    /**
     * 이 길이를 넘는 글은 머리말이 아니라 본문으로 본다.
     */
    static final int PREAMBLE_LIMIT = 200;
    private static final int TITLE_DECISION_LIMIT = 300;
    private static final Pattern LEADING_H1 = Pattern.compile("^#\\s+.*");

    public interface Listener {
        void status(String message);

        void text(String text);
    }

    private record ToolCall(String name, JsonValue initialInput, StringBuilder json) {
    }

    private final Listener listener;
    private final boolean holdPreamble;
    private final boolean stripLeadingTitle;

    private BetaMessageAccumulator accumulator;
    private final Map<Long, ToolCall> toolCalls = new HashMap<>();
    private final StringBuilder pending = new StringBuilder();
    private boolean bodyStarted;
    private final StringBuilder titleHead = new StringBuilder();
    private boolean titleDecided;
    private final StringBuilder body = new StringBuilder();

    private final LinkedHashMap<String, AiReferences.Source> cited = new LinkedHashMap<>();
    private final LinkedHashMap<String, AiReferences.Source> fetched = new LinkedHashMap<>();
    private final LinkedHashMap<String, AiReferences.Source> searched = new LinkedHashMap<>();

    private AiTokenUsage finishedUsage = AiTokenUsage.ZERO;
    private AiTokenUsage messageUsage = AiTokenUsage.ZERO;
    private BetaStopReason stopReason;

    /**
     * @param listener          본문 조각·진행 상황을 받을 곳
     * @param holdPreamble      서버 도구(웹 조사)를 쓰는 요청이면 true — 도구 호출 앞의 짧은 머리말을 버린다
     * @param stripLeadingTitle 맨 위 {@code # 제목} 줄을 버릴지
     */
    public AiStreamCollector(Listener listener, boolean holdPreamble, boolean stripLeadingTitle) {
        this.listener = listener;
        this.holdPreamble = holdPreamble;
        this.stripLeadingTitle = stripLeadingTitle;
        this.bodyStarted = !holdPreamble;
        this.titleDecided = !stripLeadingTitle;
    }

    public void beginMessage() {
        accumulator = BetaMessageAccumulator.create();
        toolCalls.clear();
        messageUsage = AiTokenUsage.ZERO;
        stopReason = null;
    }

    public void accept(BetaRawMessageStreamEvent event) {
        event.contentBlockStop().ifPresent(e -> feedInitialInputIfNoDeltas(e.index()));
        accumulator.accumulate(event);
        event.messageStart().ifPresent(e -> messageUsage = AiTokenUsage.of(e.message().usage()));
        event.messageDelta().ifPresent(this::onMessageDelta);
        event.contentBlockStart().ifPresent(this::onBlockStart);
        event.contentBlockDelta().ifPresent(this::onBlockDelta);
        event.contentBlockStop().ifPresent(e -> onBlockStop(e.index()));
    }

    /**
     * 한 번의 스트림이 끝났을 때 부른다. 완성된 응답(이어 받기에 쓸 assistant 내용)을 돌려준다.
     */
    public BetaMessage endMessage() {
        finishedUsage = finishedUsage.plus(messageUsage);
        messageUsage = AiTokenUsage.ZERO;
        return accumulator.message();
    }

    /**
     * 모든 스트림이 끝난 뒤 붙잡아 둔 글을 내보낸다.
     */
    public void finish() {
        flushPending();
        if (!titleDecided) {
            titleDecided = true;
            emitRaw(decideTitle(titleHead.toString().stripLeading()));
        }
    }

    /**
     * 서버가 덧붙이는 글(참고 자료 등)을 본문과 같은 경로로 내보낸다.
     */
    public void appendText(String text) {
        emitRaw(text);
    }

    public String body() {
        return body.toString();
    }

    /**
     * 지금까지의 사용량 (진행 중인 스트림 포함)
     */
    public AiTokenUsage usage() {
        return finishedUsage.plus(messageUsage);
    }

    public Optional<BetaStopReason> stopReason() {
        return Optional.ofNullable(stopReason);
    }

    /**
     * 참고 자료로 쓸 출처: 본문이 인용한 것과 읽은 페이지가 있으면 그것들, 없으면 검색 결과 앞쪽.
     */
    public List<AiReferences.Source> collectedSources(int max) {
        var primary = new LinkedHashMap<>(cited);
        fetched.forEach(primary::putIfAbsent);
        var pool = primary.isEmpty() ? searched : primary;
        return pool.values().stream().limit(max).toList();
    }

    private void onMessageDelta(BetaRawMessageDeltaEvent e) {
        var u = e.usage();
        messageUsage = new AiTokenUsage(
                u.inputTokens().orElse(messageUsage.inputTokens()),
                u.outputTokens(),
                u.cacheReadInputTokens().orElse(messageUsage.cacheReadTokens()),
                u.cacheCreationInputTokens().orElse(messageUsage.cacheWriteTokens()),
                (int) u.serverToolUse().map(BetaServerToolUsage::webSearchRequests).orElse((long) messageUsage.webSearches()).longValue());
        e.delta().stopReason().ifPresent(reason -> stopReason = reason);
    }

    private void onBlockStart(BetaRawContentBlockStartEvent e) {
        var block = e.contentBlock();
        if (block.isServerToolUse()) {
            if (!bodyStarted && !pending.isEmpty()) {
                log.debug("dropping preamble before a server tool call ({} chars)", pending.length());
                pending.setLength(0);
            }
            var toolUse = block.asServerToolUse();
            toolCalls.put(e.index(), new ToolCall(toolUse.name().asString(), toolUse._input(), new StringBuilder()));
        } else if (block.isText()) {
            // 글 블록이 이어지면(인용 때문에 블록이 나뉜다) 붙잡아 둔 글은 본문이다.
            if (!bodyStarted && !pending.isEmpty()) {
                flushPending();
            }
            onText(block.asText().text());
        } else if (block.isWebSearchToolResult()) {
            block.asWebSearchToolResult().content().resultBlocks().ifPresent(results -> results.forEach(r ->
                    searched.putIfAbsent(r.url(), new AiReferences.Source(r.title(), r.url()))));
        } else if (block.isWebFetchToolResult()) {
            block.asWebFetchToolResult().content().betaWebFetchBlock().ifPresent(page ->
                    fetched.putIfAbsent(page.url(), new AiReferences.Source(page.content().title().orElse(page.url()), page.url())));
        } else if (block.isFallback()) {
            listener.status("안전 검사 때문에 다른 모델이 이어서 써요");
        }
    }

    private void onBlockDelta(BetaRawContentBlockDeltaEvent e) {
        var delta = e.delta();
        delta.text().ifPresent(t -> onText(t.text()));
        delta.inputJson().ifPresent(j -> {
            var call = toolCalls.get(e.index());
            if (call != null) {
                call.json().append(j.partialJson());
            }
        });
        delta.citations().ifPresent(c -> c.citation().webSearchResultLocation().ifPresent(loc ->
                cited.putIfAbsent(loc.url(), new AiReferences.Source(loc.title().orElse(loc.url()), loc.url()))));
    }

    private void onBlockStop(long index) {
        var call = toolCalls.remove(index);
        if (call == null) {
            return;
        }
        var input = parseInput(call);
        if ("web_search".equals(call.name())) {
            var query = text(input, "query");
            if (query != null) {
                listener.status("웹에서 '" + shorten(query, 80) + "' 찾는 중");
            }
        } else if ("web_fetch".equals(call.name())) {
            var url = text(input, "url");
            if (url != null) {
                listener.status("'" + displayUrl(url) + "' 읽는 중");
            }
        }
    }

    private void onText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (bodyStarted) {
            emit(text);
            return;
        }
        pending.append(text);
        if (pending.length() >= PREAMBLE_LIMIT) {
            flushPending();
        }
    }

    private void flushPending() {
        bodyStarted = true;
        if (!pending.isEmpty()) {
            var text = pending.toString();
            pending.setLength(0);
            emit(text);
        }
    }

    private void emit(String text) {
        if (titleDecided) {
            emitRaw(text);
            return;
        }
        titleHead.append(text);
        var head = titleHead.toString().stripLeading();
        if (head.isEmpty() || (head.indexOf('\n') < 0 && head.length() < TITLE_DECISION_LIMIT)) {
            return;
        }
        titleDecided = true;
        emitRaw(decideTitle(head));
    }

    /**
     * 첫 줄이 {@code # 제목} 이면 그 줄과 뒤따르는 빈 줄을 뺀다.
     */
    static String decideTitle(String head) {
        var newline = head.indexOf('\n');
        var firstLine = newline < 0 ? head : head.substring(0, newline);
        if (!LEADING_H1.matcher(firstLine.strip()).matches()) {
            return head;
        }
        return newline < 0 ? "" : head.substring(newline + 1).replaceFirst("^(\\s*\\n)+", "");
    }

    private void emitRaw(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        body.append(text);
        listener.text(text);
    }

    /**
     * SDK 3.x 의 {@link BetaMessageAccumulator} 는 server_tool_use 블록이 끝날 때 input_json_delta 를 하나도 받지 못했으면
     * "Missing input JSON for index N" 을 던진다. 그런데 web_search_20260209(동적 필터링)는 입력을 content_block_start 에
     * 다 담아 보내고 delta 를 보내지 않는 경우가 있다(2026-11-18 실제 호출에서 확인, SDK main 브랜치도 같은 코드).
     * 그때는 시작 때 받은 입력을 delta 하나로 만들어 먼저 넣어 준다 — 이어 받기(pause_turn)에 쓸 assistant 내용도 그대로 맞는다.
     */
    private void feedInitialInputIfNoDeltas(long index) {
        var call = toolCalls.get(index);
        if (call == null || !call.json().isEmpty()) {
            return;
        }
        String json = "{}";
        try {
            if (call.initialInput() != null && !call.initialInput().isMissing()) {
                json = ObjectMappers.jsonMapper().writeValueAsString(call.initialInput());
            }
        } catch (Exception ex) {
            log.debug("could not serialize server tool input: {}", ex.getMessage());
        }
        if (json.isBlank() || "null".equals(json)) {
            json = "{}";
        }
        call.json().append(json);
        accumulator.accumulate(BetaRawMessageStreamEvent.ofContentBlockDelta(BetaRawContentBlockDeltaEvent.builder()
                .index(index)
                .delta(BetaInputJsonDelta.builder().partialJson(json).build())
                .build()));
    }

    private static JsonNode parseInput(ToolCall call) {
        try {
            var mapper = ObjectMappers.jsonMapper();
            if (!call.json().isEmpty()) {
                return mapper.readTree(call.json().toString());
            }
            return mapper.valueToTree(call.initialInput());
        } catch (Exception e) {
            log.debug("could not parse server tool input: {}", e.getMessage());
            return null;
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        var value = node.get(field).asText().strip();
        return value.isEmpty() ? null : value;
    }

    static String displayUrl(String url) {
        try {
            var uri = URI.create(url);
            var shown = (uri.getHost() == null ? "" : uri.getHost().replaceFirst("^www\\.", ""))
                    + (uri.getRawPath() == null ? "" : uri.getRawPath());
            return shorten(shown.isEmpty() ? url : shown, 80);
        } catch (IllegalArgumentException e) {
            return shorten(url, 80);
        }
    }

    private static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    /**
     * 검색 결과 출처들 (테스트용)
     */
    List<AiReferences.Source> searchedSources() {
        return new ArrayList<>(searched.values());
    }
}
