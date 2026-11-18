package com.hwans.apiserver.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.AiMetaApplyRequestDto;
import com.hwans.apiserver.dto.ai.AiMetaDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 제목·요약·태그 일괄 제안의 글별 흐름 (DB·Anthropic 없이 가짜 협력자로). 쓰기 경로가 아예 없다는 것도 함께 본다.
 */
class AiMetaBatchTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AiTokenUsage PER_POST = new AiTokenUsage(1000, 200, 0, 0, 0);

    private static final class Sink implements AiSseSession.FrameSink {
        final List<String> frames = new ArrayList<>();
        int failAfterItems = Integer.MAX_VALUE;
        int items;

        @Override
        public void write(String frame) throws IOException {
            if (items >= failAfterItems) {
                throw new IOException("client gone");
            }
            frames.add(frame);
            if (frame.startsWith("event: item")) {
                items++;
            }
        }

        @Override
        public void complete() {
        }

        List<String> names() {
            return frames.stream().map(f -> f.substring("event: ".length(), f.indexOf('\n'))).toList();
        }

        List<JsonNode> data(String name) {
            return frames.stream().filter(f -> f.startsWith("event: " + name + "\n")).map(f -> {
                try {
                    return JSON.readTree(f.substring(f.indexOf("data: ") + 6).strip());
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).toList();
        }
    }

    private static Map<String, AiPostMeta.Target> posts(String... postUrls) {
        var map = new LinkedHashMap<String, AiPostMeta.Target>();
        for (var postUrl : postUrls) {
            map.put(postUrl, new AiPostMeta.Target(postUrl, "제목 " + postUrl, "짧은 요약", List.of("Java"), "본문 " + postUrl));
        }
        return map;
    }

    private static final class FakeSuggester implements AiMetaBatch.Suggester {
        final List<String> called = new ArrayList<>();
        final Set<String> failing = new HashSet<>();

        @Override
        public AiMetaDto suggest(AiPostMeta.Target post, Consumer<AiTokenUsage> usage) {
            called.add(post.postUrl());
            usage.accept(PER_POST);
            if (failing.contains(post.postUrl())) {
                throw new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 호출에 실패했어요: Anthropic 사용량 제한에 걸렸어요(429).");
            }
            return AiMetaDto.builder().title("새 제목 " + post.postUrl()).summary("새 요약 " + post.postUrl())
                    .tags(List.of("Java", "보안")).postUrl("ignored").build();
        }
    }

    private static AiMetaBatch batch(Map<String, AiPostMeta.Target> posts, FakeSuggester suggester, AiCoverBatch.Quota quota) {
        // Posts 는 읽기(find)만 있다 — 일괄 제안에는 글을 바꾸는 경로가 없다
        return new AiMetaBatch(postUrl -> Optional.ofNullable(posts.get(postUrl)), suggester, quota, AiModel.OPUS);
    }

    @Test
    void fillsOnlyTheRequestedFields() {
        var sink = new Sink();
        var suggester = new FakeSuggester();

        var result = batch(posts("a"), suggester, () -> {
        }).run(List.of("a"), Set.of("summary", "tags"), new AiSseSession(JSON, sink));

        assertEquals(List.of("status", "item", "done"), sink.names());
        assertEquals("1/1 '제목 a' 제안 만드는 중", sink.data("status").get(0).path("message").asText());
        var item = sink.data("item").get(0);
        assertEquals("a", item.path("postUrl").asText());
        assertTrue(item.path("ok").asBoolean());
        assertEquals("제목 a", item.path("current").path("title").asText());
        assertEquals("짧은 요약", item.path("current").path("summary").asText());
        assertEquals("[\"Java\"]", item.path("current").path("tags").toString());
        // title 은 요청하지 않았으므로 current 와 같다
        assertEquals("제목 a", item.path("suggested").path("title").asText());
        assertEquals("새 요약 a", item.path("suggested").path("summary").asText());
        assertEquals("[\"Java\",\"보안\"]", item.path("suggested").path("tags").toString());
        assertFalse(item.path("suggested").has("postUrl"));

        var done = sink.data("done").get(0);
        assertEquals(1, done.path("total").asInt());
        assertEquals(1, done.path("suggested").asInt());
        assertEquals(0, done.path("failed").asInt());
        assertEquals(0.008, done.path("usage").path("costUsd").asDouble(), 1e-9); // 1000×4 + 200×20 (/1M)
        assertEquals(new AiMetaBatch.Result(1, 1, 0, PER_POST), result);
    }

    @Test
    void oneFailureDoesNotStopTheOthers() {
        var posts = posts("a", "b", "d");
        posts.put("empty", new AiPostMeta.Target("empty", "빈 글", null, List.of(), "  "));
        var suggester = new FakeSuggester();
        suggester.failing.add("b");
        var sink = new Sink();

        var result = batch(posts, suggester, () -> {
        }).run(List.of("a", "b", "not-mine", "empty", "d", "a"), Set.copyOf(AiMetaBatch.ALL_FIELDS), new AiSseSession(JSON, sink));

        assertEquals(List.of("a", "b", "d"), suggester.called, "missing/empty posts are not sent to the AI; duplicates are done once");
        var items = sink.data("item");
        assertEquals(5, items.size());
        assertTrue(items.get(0).path("ok").asBoolean());
        assertEquals("새 제목 a", items.get(0).path("suggested").path("title").asText());
        assertFalse(items.get(1).path("ok").asBoolean());
        assertEquals("AI 호출에 실패했어요: Anthropic 사용량 제한에 걸렸어요(429).", items.get(1).path("message").asText());
        assertEquals("글을 찾을 수 없어요.", items.get(2).path("message").asText());
        assertEquals("본문이 비어 있어 제안할 수 없어요.", items.get(3).path("message").asText());
        assertTrue(items.get(4).path("ok").asBoolean());
        var done = sink.data("done").get(0);
        assertEquals(5, done.path("total").asInt());
        assertEquals(2, done.path("suggested").asInt());
        assertEquals(3, done.path("failed").asInt());
        assertEquals(PER_POST.plus(PER_POST).plus(PER_POST), result.usage());
    }

    @Test
    void limitExceededMidwayEndsWithAnError() {
        var suggester = new FakeSuggester();
        var checks = new int[1];
        AiCoverBatch.Quota quota = () -> {
            if (++checks[0] == 2) {
                throw new RestApiException(ErrorCodes.TooManyRequests.AI_LIMIT_EXCEEDED, "이번 달 AI 사용 한도($10)에 닿았어요. 설정에서 한도를 늘릴 수 있어요.");
            }
        };
        var sink = new Sink();
        var session = new AiSseSession(JSON, sink);
        try {
            batch(posts("a", "b", "c"), suggester, quota).run(List.of("a", "b", "c"), Set.of("tags"), session);
            fail("limit exceeded must end the batch");
        } catch (RestApiException e) {
            AiServiceImpl.reportStreamFailure(session, e);
        }
        assertEquals(List.of("a"), suggester.called);
        assertEquals(List.of("status", "item", "error"), sink.names());
        assertEquals("AI_LIMIT_EXCEEDED", sink.data("error").get(0).path("code").asText());
    }

    @Test
    void stopsAfterTheCurrentPostWhenTheClientDisconnects() {
        var suggester = new FakeSuggester();
        var sink = new Sink();
        sink.failAfterItems = 1;
        var session = new AiSseSession(JSON, sink);
        assertNull(batch(posts("a", "b", "c"), suggester, () -> {
        }).run(List.of("a", "b", "c"), Set.of("summary"), session));
        assertEquals(List.of("a"), suggester.called);
    }

    @Test
    void emptySuggestionsKeepTheCurrentValue() {
        var post = new AiPostMeta.Target("a", "제목", null, List.of("Java"), "본문");
        var item = AiMetaBatch.success(post, AiMetaDto.builder().title(" ").summary("").tags(List.of()).build(),
                Set.copyOf(AiMetaBatch.ALL_FIELDS));
        @SuppressWarnings("unchecked")
        var suggested = (Map<String, Object>) item.get("suggested");
        assertEquals("제목", suggested.get("title"));
        assertEquals("", suggested.get("summary"));
        assertEquals(List.of("Java"), suggested.get("tags"));
    }

    @Test
    void validatesFields() {
        assertEquals(Set.of("title", "summary", "tags"), AiMetaBatch.validateFields(null));
        assertEquals(Set.of("summary"), AiMetaBatch.validateFields(List.of(" summary ", "summary")));
        assertThrows(RestApiException.class, () -> AiMetaBatch.validateFields(List.of()));
        assertThrows(RestApiException.class, () -> AiMetaBatch.validateFields(List.of("postUrl")));
    }

    @Test
    void validatesApplyItems() {
        var change = AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a")
                .tags(List.of(" Java ", "#보안", "java", "", "Spring  Boot", "spring boot")).build());
        assertNull(change.title());
        assertNull(change.summary());
        assertEquals(List.of("Java", "보안", "Spring  Boot"), change.tags());

        assertEquals("새 요약", AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").summary("  새 요약 ").build()).summary());
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").summary("가".repeat(256)).build()));
        assertEquals(255, AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").summary("가".repeat(255)).build()).summary().length());
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").title("가".repeat(2001)).build()));
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").title("  ").build()));
        var eleven = new ArrayList<String>();
        for (var i = 0; i < 11; i++) {
            eleven.add("t" + i);
        }
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").tags(eleven).build()));
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().postUrl("a").build()), "nothing to change");
        assertThrows(RestApiException.class, () -> AiPostMeta.validate(AiMetaApplyRequestDto.Item.builder().summary("x").build()), "no postUrl");
    }

    @Test
    void requestBodiesDeserialize() throws Exception {
        var apply = JSON.readValue("""
                {"items":[{"postUrl":"a","summary":"요약","tags":["Java"]},{"postUrl":"b","title":"제목"}]}""",
                AiMetaApplyRequestDto.class);
        assertEquals(2, apply.getItems().size());
        assertEquals("요약", apply.getItems().get(0).getSummary());
        assertEquals(List.of("Java"), apply.getItems().get(0).getTags());
        assertNull(apply.getItems().get(1).getTags(), "a missing field stays null (= keep)");
        var batch = JSON.readValue("""
                {"postUrls":["a","b"],"fields":["summary","tags"]}""", com.hwans.apiserver.dto.ai.AiMetaBatchRequestDto.class);
        assertEquals(List.of("summary", "tags"), batch.getFields());
    }

    @Test
    void shortSummaryCountsCharactersNotBytes() {
        assertTrue(AiPostMeta.isShortSummary(null));
        assertTrue(AiPostMeta.isShortSummary("  "));
        assertTrue(AiPostMeta.isShortSummary("가".repeat(39)));
        assertFalse(AiPostMeta.isShortSummary("가".repeat(40)));
    }
}
