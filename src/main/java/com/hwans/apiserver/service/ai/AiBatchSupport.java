package com.hwans.apiserver.service.ai;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 일괄 작업(커버 일괄 만들기, 제목·요약·태그 제안)이 함께 쓰는 것
 */
final class AiBatchSupport {
    static final int MAX_POSTS = 100;

    private AiBatchSupport() {
    }

    /**
     * 글 주소 목록 검증: 1~100개, 빈 값 제외, 앞뒤 공백 제거, 중복은 한 번만(순서 유지)
     */
    static List<String> validatePostUrls(List<String> postUrls) {
        if (postUrls == null || postUrls.size() > MAX_POSTS) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 는 1~" + MAX_POSTS + "개여야 해요.");
        }
        var cleaned = postUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::strip)
                .distinct()
                .toList();
        if (cleaned.isEmpty()) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 는 1~" + MAX_POSTS + "개여야 해요.");
        }
        if (cleaned.stream().anyMatch(url -> url.length() > 320)) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "postUrls 에 너무 긴 주소가 있어요.");
        }
        return cleaned;
    }

    /**
     * {@code done.usage} ({@code inputTokens} 는 캐시 포함 합계)
     */
    static Map<String, Object> usageMap(AiTokenUsage usage, AiModel model) {
        var map = new LinkedHashMap<String, Object>();
        map.put("inputTokens", usage.totalInputTokens());
        map.put("outputTokens", usage.outputTokens());
        map.put("costUsd", usage.costUsd(model).doubleValue());
        return map;
    }
}
