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

    /**
     * 썸네일이 없는 내 글 (공개 유형 무관, 최신 글부터 최대 500개)
     */
    AiMissingCoversDto missingCovers(UUID accountId);

    /**
     * 여러 글의 커버를 차례로 만들어 썸네일로 지정하거나 본문 앞에 넣는다 (SSE: status·item·done·error)
     */
    ResponseEntity<ResponseBodyEmitter> coverBatch(UUID accountId, AiCoverBatchRequestDto request);

    /**
     * 태그가 없거나 요약이 40자 미만인 내 글 (공개 유형 무관, 최신 글부터 최대 500개)
     */
    AiMetaCandidatesDto metaCandidates(UUID accountId);

    /**
     * 여러 글의 제목·요약·태그 제안을 차례로 만든다. 글은 바꾸지 않는다 (SSE: status·item·done·error)
     */
    ResponseEntity<ResponseBodyEmitter> metaBatch(UUID accountId, AiMetaBatchRequestDto request);

    /**
     * 고른 제목·요약·태그를 적용한다. 온 필드만 바꾼다(AI 호출 없음)
     */
    AiMetaApplyResultDto metaApply(UUID accountId, AiMetaApplyRequestDto request);
}
