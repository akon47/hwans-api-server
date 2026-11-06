package com.hwans.apiserver.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * 요청 본문을 로깅용으로 캐시한다. 큰 업로드(첨부파일 최대 100 MB)를 메모리에 통째로 담지 않도록 1 MB 까지만 캐시한다.
 */
public class RequestWrapper extends ContentCachingRequestWrapper {
    private static final int CACHE_LIMIT_BYTES = 1024 * 1024;

    public RequestWrapper(HttpServletRequest request) {
        super(request, CACHE_LIMIT_BYTES);
    }
}
