package com.hwans.apiserver.service.docs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 에이전트용 가이드(resources/docs)와 로그인 도우미 스크립트(resources/cli)를 읽어
 * {{SITE_URL}}, {{API_URL}} 자리를 이 서버의 실제 주소로 채워 준다.
 * 에이전트가 {@code {api}/agents.md} 를 읽으면 바로 복사해 쓸 수 있는 명령이 나오게 하기 위함.
 */
@Service
public class AgentDocsService {
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;
    @Value("${blog.api-base-url:https://api.hwanstory.kr}")
    private String apiBaseUrl;

    /**
     * classpath 의 리소스를 자리표시자를 채워 반환한다.
     *
     * @param path classpath 경로 (예: docs/agents.md)
     * @return 내용
     */
    public String text(String path) {
        var raw = cache.computeIfAbsent(path, AgentDocsService::load);
        return raw
                .replace("{{SITE_URL}}", trimSlash(webBaseUrl))
                .replace("{{API_URL}}", trimSlash(apiBaseUrl));
    }

    private static String trimSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String load(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("missing resource " + path, e);
        }
    }
}
