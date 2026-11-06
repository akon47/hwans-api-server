package com.hwans.apiserver.controller;

import com.hwans.apiserver.service.docs.AgentDocsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Hidden;

import java.time.Duration;

/**
 * AI 에이전트용 문서와 로그인 도우미 스크립트 (인증 없음).
 * <ul>
 *   <li>{@code /agents.md} — 에이전트 가이드 + API 레퍼런스 (마크다운)</li>
 *   <li>{@code /llms.txt} — 크롤러·에이전트용 요약 (llms.txt 관례)</li>
 *   <li>{@code /cli/login.sh}, {@code /cli/login.ps1} — 기기 로그인 도우미</li>
 * </ul>
 */
@RestController
@Hidden
@RequiredArgsConstructor
public class AgentDocsController {
    private static final MediaType MARKDOWN = MediaType.valueOf("text/markdown;charset=UTF-8");
    private static final MediaType PLAIN = MediaType.valueOf("text/plain;charset=UTF-8");

    private final AgentDocsService agentDocsService;

    @GetMapping(value = "/agents.md")
    public ResponseEntity<String> agentsGuide() {
        return text(MARKDOWN, "docs/agents.md");
    }

    @GetMapping(value = "/llms.txt")
    public ResponseEntity<String> llmsTxt() {
        return text(PLAIN, "docs/llms.txt");
    }

    @GetMapping(value = "/cli/login.sh")
    public ResponseEntity<String> loginSh() {
        return text(PLAIN, "cli/login.sh");
    }

    @GetMapping(value = "/cli/login.ps1")
    public ResponseEntity<String> loginPs1() {
        return text(PLAIN, "cli/login.ps1");
    }

    private ResponseEntity<String> text(MediaType mediaType, String path) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(agentDocsService.text(path));
    }
}
