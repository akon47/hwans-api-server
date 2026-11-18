package com.hwans.apiserver.entity.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AI 호출 종류. DB({@code tb_ai_usage.kind})에는 소문자 키로 저장한다.
 */
@Getter
@RequiredArgsConstructor
public enum AiUsageKind {
    DRAFT("draft"),
    REWRITE("rewrite"),
    META("meta"),
    TRANSLATE("translate"),
    STYLE("style"),
    COVER("cover");

    private final String key;
}
