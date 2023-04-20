package com.hwans.apiserver.common.logging;

import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletResponse;

public class ResponseWrapper extends ContentCachingResponseWrapper {
    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }
}