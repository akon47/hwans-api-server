package com.hwans.apiserver.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.attachment.FileDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 커버 일괄 만들기의 글별 흐름 (DB·Anthropic 없이 가짜 협력자로)
 */
class AiCoverBatchTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREFIX = "https://api.hwanstory.kr/api/v1";
    private static final AiTokenUsage PER_COVER = new AiTokenUsage(800, 120, 0, 0, 0);

    /**
     * SSE 프레임을 모으고, 원하면 n 번째 item 뒤로 쓰기를 실패시켜 "클라이언트가 끊음"을 흉내 낸다.
     */
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

    private static final class FakePosts implements AiCoverBatch.Posts {
        final Map<String, AiPostCovers.Target> posts = new LinkedHashMap<>();
        final List<String> applied = new ArrayList<>();

        FakePosts add(String postUrl, boolean hasThumbnail) {
            posts.put(postUrl, new AiPostCovers.Target(postUrl, "제목 " + postUrl, "요약", List.of("Java"), hasThumbnail));
            return this;
        }

        @Override
        public Optional<AiPostCovers.Target> find(String postUrl) {
            return Optional.ofNullable(posts.get(postUrl));
        }

        @Override
        public void apply(String postUrl, UUID fileId, String imageUrl, boolean useAsThumbnail, boolean insertIntoContent) {
            assertEquals(PREFIX + "/attachments/" + fileId + "/image.png", imageUrl);
            applied.add(postUrl);
        }
    }

    private static final class FakeCovers implements AiCoverBatch.Covers {
        final List<String> created = new ArrayList<>();
        final Set<String> failing = new HashSet<>();

        @Override
        public FileDto create(AiPostCovers.Target post, java.util.function.Consumer<AiTokenUsage> usage) {
            created.add(post.postUrl());
            usage.accept(PER_COVER); // 실패해도 이미 Anthropic 을 불렀으면 사용량이 있다
            if (failing.contains(post.postUrl())) {
                throw new RestApiException(ErrorCodes.BadGateway.AI_UPSTREAM, "AI 호출에 실패했어요: Anthropic 서버가 지금 바빠요(529).");
            }
            var id = UUID.randomUUID();
            return FileDto.builder().id(id).url("/attachments/" + id + "/image.png").build();
        }
    }

    private static AiCoverBatch batch(FakePosts posts, FakeCovers covers, AiCoverBatch.Quota quota) {
        return new AiCoverBatch(posts, covers, quota, AiModel.OPUS, PREFIX);
    }

    @Test
    void skipsPostsThatAlreadyHaveAThumbnail() {
        var posts = new FakePosts().add("a", false).add("b", true);
        var covers = new FakeCovers();
        var sink = new Sink();

        var result = batch(posts, covers, () -> {
        }).run(List.of("a", "b"), true, false, new AiSseSession(JSON, sink));

        assertEquals(List.of("a"), covers.created);
        assertEquals(List.of("a"), posts.applied);
        assertEquals(List.of("status", "item", "item", "done"), sink.names());
        assertEquals("1/2 '제목 a' 커버 만드는 중", sink.data("status").get(0).path("message").asText());
        var skipped = sink.data("item").get(1);
        assertEquals("b", skipped.path("postUrl").asText());
        assertTrue(skipped.path("ok").asBoolean());
        assertTrue(skipped.path("skipped").asBoolean());
        assertEquals("이미 썸네일이 있어요", skipped.path("message").asText());
        var created = sink.data("item").get(0);
        assertFalse(created.path("skipped").asBoolean());
        assertTrue(created.path("url").asText().matches("/attachments/" + created.path("fileId").asText() + "/image.png"));

        var done = sink.data("done").get(0);
        assertEquals(2, done.path("total").asInt());
        assertEquals(1, done.path("created").asInt());
        assertEquals(1, done.path("skipped").asInt());
        assertEquals(0, done.path("failed").asInt());
        assertEquals(800, done.path("usage").path("inputTokens").asLong());
        assertEquals(120, done.path("usage").path("outputTokens").asLong());
        assertEquals(0.0056, done.path("usage").path("costUsd").asDouble(), 1e-9); // 800×4 + 120×20 (/1M)
        assertEquals(new AiCoverBatch.Result(2, 1, 1, 0, PER_COVER), result);

        // 썸네일로 쓰지 않을 때는 이미 썸네일이 있어도 만든다
        var covers2 = new FakeCovers();
        batch(new FakePosts().add("a", false).add("b", true), covers2, () -> {
        }).run(List.of("a", "b"), false, true, new AiSseSession(JSON, new Sink()));
        assertEquals(List.of("a", "b"), covers2.created);
    }

    @Test
    void oneFailureDoesNotStopTheOthers() {
        var posts = new FakePosts().add("a", false).add("b", false).add("d", false);
        var covers = new FakeCovers();
        covers.failing.add("b");
        var sink = new Sink();

        var result = batch(posts, covers, () -> {
        }).run(List.of("a", "b", "not-mine", "d"), true, false, new AiSseSession(JSON, sink));

        assertEquals(List.of("a", "b", "d"), covers.created);
        assertEquals(List.of("a", "d"), posts.applied);
        var items = sink.data("item");
        assertEquals(4, items.size());
        assertTrue(items.get(0).path("ok").asBoolean());
        assertFalse(items.get(1).path("ok").asBoolean());
        assertEquals("AI 호출에 실패했어요: Anthropic 서버가 지금 바빠요(529).", items.get(1).path("message").asText());
        assertFalse(items.get(1).has("fileId"));
        assertEquals("not-mine", items.get(2).path("postUrl").asText());
        assertEquals("글을 찾을 수 없어요.", items.get(2).path("message").asText());
        assertTrue(items.get(3).path("ok").asBoolean());

        var done = sink.data("done").get(0);
        assertEquals(4, done.path("total").asInt());
        assertEquals(2, done.path("created").asInt());
        assertEquals(0, done.path("skipped").asInt());
        assertEquals(2, done.path("failed").asInt());
        // 실패한 b 도 Anthropic 을 불렀으므로 사용량에 들어간다
        assertEquals(PER_COVER.plus(PER_COVER).plus(PER_COVER), result.usage());
    }

    @Test
    void limitExceededMidwayEndsWithAnError() {
        var posts = new FakePosts().add("a", false).add("b", false).add("c", false).add("d", false);
        var covers = new FakeCovers();
        var checks = new int[1];
        AiCoverBatch.Quota quota = () -> {
            if (++checks[0] == 3) {
                throw new RestApiException(ErrorCodes.TooManyRequests.AI_LIMIT_EXCEEDED,
                        "이번 달 AI 사용 한도($10)에 닿았어요. 설정에서 한도를 늘릴 수 있어요.");
            }
        };
        var sink = new Sink();
        var session = new AiSseSession(JSON, sink);

        // 실제 스트림 작업과 같게: 예외는 runStream 이 error 이벤트로 바꾼다
        try {
            batch(posts, covers, quota).run(List.of("a", "b", "c", "d"), true, true, session);
            fail("limit exceeded must end the batch");
        } catch (RestApiException e) {
            AiServiceImpl.reportStreamFailure(session, e);
        }

        assertEquals(List.of("a", "b"), covers.created);
        assertEquals(List.of("a", "b"), posts.applied, "posts done before the limit stay applied");
        assertEquals(List.of("status", "item", "status", "item", "error"), sink.names());
        var error = sink.data("error").get(0);
        assertEquals("AI_LIMIT_EXCEEDED", error.path("code").asText());
        assertTrue(error.path("message").asText().contains("$10"));
        assertTrue(sink.data("done").isEmpty());
    }

    @Test
    void stopsAfterTheCurrentPostWhenTheClientDisconnects() {
        var posts = new FakePosts().add("a", false).add("b", false).add("c", false);
        var covers = new FakeCovers();
        var sink = new Sink();
        sink.failAfterItems = 1; // 첫 글의 item 을 보낸 뒤 연결이 끊긴다
        var session = new AiSseSession(JSON, sink);

        var result = batch(posts, covers, () -> {
        }).run(List.of("a", "b", "c"), true, false, session);

        assertNull(result);
        assertTrue(session.isCancelled());
        assertEquals(List.of("a"), covers.created);
        assertEquals(List.of("a"), posts.applied);
    }

    @Test
    void duplicatePostUrlsAreDoneOnce() {
        var posts = new FakePosts().add("a", false);
        var covers = new FakeCovers();
        var sink = new Sink();
        batch(posts, covers, () -> {
        }).run(List.of("a", "a"), false, true, new AiSseSession(JSON, sink));
        assertEquals(List.of("a"), covers.created);
        assertEquals(List.of("a"), posts.applied);
        assertEquals(1, sink.data("done").get(0).path("total").asInt());
    }

    @Test
    void validatesTheRequest() {
        assertThrows(RestApiException.class, () -> AiBatchSupport.validatePostUrls(null));
        assertThrows(RestApiException.class, () -> AiBatchSupport.validatePostUrls(List.of()));
        assertThrows(RestApiException.class, () -> AiBatchSupport.validatePostUrls(Arrays.asList(" ", null)));
        var tooMany = new ArrayList<String>();
        for (var i = 0; i <= AiBatchSupport.MAX_POSTS; i++) {
            tooMany.add("p" + i);
        }
        assertThrows(RestApiException.class, () -> AiBatchSupport.validatePostUrls(tooMany));
        assertEquals(List.of("a", "b"), AiBatchSupport.validatePostUrls(List.of(" a ", "b", "a")));
    }

    @Test
    void prependsTheImageOnce() {
        assertEquals("![\\[WPF\\] 타이머 사용하기](https://api.hwanstory.kr/api/v1/attachments/x/image.png)\n\n본문",
                AiPostCovers.prependImage("본문", "[WPF] 타이머 사용하기", "https://api.hwanstory.kr/api/v1/attachments/x/image.png"));
        assertEquals("![제목](u)\n\n", AiPostCovers.prependImage(null, "제목", "u"));
    }
}
