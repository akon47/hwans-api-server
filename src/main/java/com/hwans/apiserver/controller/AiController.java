package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCode;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.ai.*;
import com.hwans.apiserver.dto.common.ErrorResponseDto;
import com.hwans.apiserver.service.ai.AiService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * AI 글쓰기 Controller. 회원이 등록한 자기 Anthropic API 키로 초안·다듬기·제목/요약/태그·영어판·커버 이미지를 만든다.
 * 로그인(JWT)이 필요하고, 개인 액세스 토큰으로는 쓸 수 없다(AccessTokenAccessRules). AI 가 만든 글은 프론트가 임시저장으로만 저장한다.
 * <p>
 * 스트리밍(SSE) 응답: {@code event: status|delta|done|error}, data 는 JSON, 15초마다 {@code : ping}.
 * 스트림 시작 전의 오류는 일반 JSON 오류로 돌려준다.
 */
@Slf4j
@RestController
@Tag(name = "AI 글쓰기")
@RequestMapping(value = Constants.API_PREFIX + "/v1/ai")
@RequiredArgsConstructor
public class AiController {
    private final AiService aiService;

    @Operation(summary = "AI 설정 조회", description = "기능 사용 가능 여부, 키 등록 여부(끝 4자리), 모델, 월 한도, 이번 달 사용량, 문체 프로필, 모델 목록을 조회한다.")
    @GetMapping(value = "/settings")
    public AiSettingsDto getSettings(@CurrentAuthenticationDetails UserAuthenticationDetails user) {
        return aiService.getSettings(user.getId());
    }

    @Operation(summary = "AI 설정 변경", description = "API 키(오면 Anthropic 에서 확인 후 암호화 저장, 없거나 null 이면 유지)·모델·월 한도를 바꾼다.")
    @PutMapping(value = "/settings")
    public AiSettingsDto updateSettings(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                        @Parameter(description = "설정", required = true) @RequestBody AiSettingsRequestDto request) {
        return aiService.updateSettings(user.getId(), request);
    }

    @Operation(summary = "AI API 키 삭제", description = "등록한 API 키만 지운다. 문체 프로필과 사용 기록은 남긴다.")
    @DeleteMapping(value = "/settings/key")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiKey(@CurrentAuthenticationDetails UserAuthenticationDetails user) {
        aiService.deleteApiKey(user.getId());
    }

    @Operation(summary = "문체 분석", description = "내 글(임시저장 제외)에서 최근 것부터 본문 300자 이상인 글을 최대 10개 읽어 문체 프로필을 만든다.")
    @PostMapping(value = "/style/analyze")
    public AiStyleDto analyzeStyle(@CurrentAuthenticationDetails UserAuthenticationDetails user) {
        return aiService.analyzeStyle(user.getId());
    }

    @Operation(summary = "AI 초안 쓰기 (SSE)", description = "내 문체로 블로그 글 초안 본문(마크다운)을 스트리밍한다. webResearch 면 웹에서 자료를 찾아 끝에 참고 자료를 붙인다.")
    @PostMapping(value = "/draft")
    public ResponseEntity<ResponseBodyEmitter> draft(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                                     @Parameter(description = "초안 요청", required = true) @RequestBody AiDraftRequestDto request) {
        return aiService.draft(user.getId(), request);
    }

    @Operation(summary = "AI 다듬기 (SSE)", description = "선택한 부분을 지시대로 바꾼 텍스트만 스트리밍한다.")
    @PostMapping(value = "/rewrite")
    public ResponseEntity<ResponseBodyEmitter> rewrite(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                                       @Parameter(description = "다듬기 요청", required = true) @RequestBody AiRewriteRequestDto request) {
        return aiService.rewrite(user.getId(), request);
    }

    @Operation(summary = "제목·요약·태그·주소 추천", description = "본문을 읽고 제목·요약·태그(내 블로그의 기존 표기 우선)·게시글 주소를 추천한다.")
    @PostMapping(value = "/meta")
    public AiMetaDto meta(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                          @Parameter(description = "글", required = true) @RequestBody AiMetaRequestDto request) {
        return aiService.meta(user.getId(), request);
    }

    @Operation(summary = "영어 번역 (SSE)", description = "영어 본문 마크다운을 스트리밍하고, done 에 영어 title·summary 를 담는다.")
    @PostMapping(value = "/translate")
    public ResponseEntity<ResponseBodyEmitter> translate(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                                         @Parameter(description = "번역 요청", required = true) @RequestBody AiTranslateRequestDto request) {
        return aiService.translate(user.getId(), request);
    }

    @Operation(summary = "커버 이미지 만들기", description = "AI 가 문구·테마·그림을 고르고 서버가 1200×630 PNG 를 그려 내 첨부 파일로 저장한다.")
    @PostMapping(value = "/cover")
    public AiCoverDto cover(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                            @Parameter(description = "커버 요청", required = true) @RequestBody AiCoverRequestDto request) {
        return aiService.cover(user.getId(), request);
    }

    @Operation(summary = "썸네일 없는 내 글 목록", description = "내 글(공개·비공개·임시저장·예약 모두) 중 썸네일이 없는 글을 최신 글부터 최대 500개 조회한다. total 은 전체 수.")
    @GetMapping(value = "/covers/missing")
    public AiMissingCoversDto missingCovers(@CurrentAuthenticationDetails UserAuthenticationDetails user) {
        return aiService.missingCovers(user.getId());
    }

    @Operation(summary = "커버 일괄 만들기 (SSE)", description = "글(1~100개)마다 커버를 만들어 썸네일로 지정하거나 본문 맨 앞에 넣는다. "
            + "다른 필드는 그대로 둔다. 이벤트: status·item·done·error. 한도는 글마다 확인하고, 분당 요청 제한은 한 번으로 센다.")
    @PostMapping(value = "/covers/batch")
    public ResponseEntity<ResponseBodyEmitter> coverBatch(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                                          @Parameter(description = "일괄 요청", required = true) @RequestBody AiCoverBatchRequestDto request) {
        return aiService.coverBatch(user.getId(), request);
    }

    @Operation(summary = "제목·요약·태그를 채울 내 글 목록", description = "내 글(모든 공개 유형) 중 태그가 없거나 요약이 40자 미만인 글을 최신 글부터 최대 500개 조회한다. total 은 전체 수.")
    @GetMapping(value = "/meta/candidates")
    public AiMetaCandidatesDto metaCandidates(@CurrentAuthenticationDetails UserAuthenticationDetails user) {
        return aiService.metaCandidates(user.getId());
    }

    @Operation(summary = "제목·요약·태그 일괄 제안 (SSE)", description = "글(1~100개)마다 제안을 만든다. 글은 바꾸지 않는다(조회수도 올리지 않음). "
            + "이벤트: status·item·done·error. 한도는 글마다 확인하고, 분당 요청 제한은 한 번으로 센다.")
    @PostMapping(value = "/meta/batch")
    public ResponseEntity<ResponseBodyEmitter> metaBatch(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                                         @Parameter(description = "일괄 제안 요청", required = true) @RequestBody AiMetaBatchRequestDto request) {
        return aiService.metaBatch(user.getId(), request);
    }

    @Operation(summary = "고른 제목·요약·태그 적용", description = "글(1~100개)마다 온 필드만 바꾼다. 본문·공개 상태·시리즈·예약·썸네일·주소는 그대로. AI 호출 없음.")
    @PostMapping(value = "/meta/apply")
    public AiMetaApplyResultDto metaApply(@CurrentAuthenticationDetails UserAuthenticationDetails user,
                                          @Parameter(description = "적용 요청", required = true) @RequestBody AiMetaApplyRequestDto request) {
        return aiService.metaApply(user.getId(), request);
    }

    // 스트리밍 요청은 Accept: text/event-stream 으로 올 수 있다. 오류 응답의 Content-Type 을 JSON 으로 못박아
    // 내용 협상(406) 없이 일반 JSON 오류 형식으로 돌려준다.

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ErrorResponseDto> handleRestApiException(RestApiException e) {
        return error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("unreadable AI request body: {}", e.getMessage());
        return error(ErrorCodes.BadRequest.INVALID_PARAMETER, "요청 본문(JSON)을 읽을 수 없어요.");
    }

    private static ResponseEntity<ErrorResponseDto> error(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseDto.builder()
                        .name(errorCode.getName())
                        .message(message)
                        .build());
    }
}
