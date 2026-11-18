package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.dto.ai.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.UUID;

/**
 * AI 글쓰기 서비스. 회원이 등록한 자기 Anthropic API 키로 서버에서 Claude 를 부른다.
 * 스트리밍 메서드는 시작 전 오류(키 없음·한도 등)를 예외(일반 JSON 오류)로 던지고, 시작한 뒤의 오류는 SSE {@code error} 이벤트로 보낸다.
 */
public interface AiService {
    AiSettingsDto getSettings(UUID accountId);

    AiSettingsDto updateSettings(UUID accountId, AiSettingsRequestDto request);

    void deleteApiKey(UUID accountId);

    AiStyleDto analyzeStyle(UUID accountId);

    ResponseEntity<ResponseBodyEmitter> draft(UUID accountId, AiDraftRequestDto request);

    ResponseEntity<ResponseBodyEmitter> rewrite(UUID accountId, AiRewriteRequestDto request);

    AiMetaDto meta(UUID accountId, AiMetaRequestDto request);

    ResponseEntity<ResponseBodyEmitter> translate(UUID accountId, AiTranslateRequestDto request);

    AiCoverDto cover(UUID accountId, AiCoverRequestDto request);
}
