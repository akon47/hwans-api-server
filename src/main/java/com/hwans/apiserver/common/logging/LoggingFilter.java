package com.hwans.apiserver.common.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static net.logstash.logback.marker.Markers.append;

@Component
public class LoggingFilter extends OncePerRequestFilter {
    protected static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MDC.put("traceId", UUID.randomUUID().toString());
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(new RequestWrapper(request), new ResponseWrapper(response), filterChain);
        }
        MDC.clear();
    }

    protected void doFilterWrapped(RequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain) throws ServletException, IOException {
        try {
            logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            logResponse(response);
            response.copyBodyToResponse();
        }
    }

    private static void logRequest(RequestWrapper request) throws IOException {
        String queryString = request.getQueryString();
        var marker = append("type", "REQUEST")
                .and(append("method", request.getMethod().toUpperCase()))
                .and(append("uri", queryString == null ? request.getRequestURI() : request.getRequestURI() + queryString))
                .and(append("content_type", request.getContentType()));
        var payload = getPayloadString(request.getContentType(), request.getInputStream());
        if (payload != null) {
            marker = marker.and(append("payload", payload));
        }

        log.info(marker, null);
    }

    private static void logResponse(ContentCachingResponseWrapper response) throws IOException {
        var marker = append("content_type", response.getContentType());
        var payload = getPayloadString(response.getContentType(), response.getContentInputStream());
        if (payload != null) {
            marker = marker.and(append("payload", payload));
        }

        log.info(marker, null);
    }

    private static String getPayloadString(String contentType, InputStream inputStream) throws IOException {
        boolean visible = isVisible(MediaType.valueOf(contentType == null ? "application/json" : contentType));
        if (visible) {
            byte[] content = StreamUtils.copyToByteArray(inputStream);
            if (content.length > 0) {
                var contentString = new String(content);
                return contentString;
            }
        } else {
            return null;
        }

        return null;
    }

    private static boolean isVisible(MediaType mediaType) {
        final List<MediaType> VISIBLE_TYPES = Arrays.asList(
                MediaType.valueOf("text/*"),
                MediaType.APPLICATION_FORM_URLENCODED,
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_XML,
                MediaType.valueOf("application/*+json"),
                MediaType.valueOf("application/*+xml"),
                MediaType.MULTIPART_FORM_DATA
        );

        return VISIBLE_TYPES.stream()
                .anyMatch(visibleType -> visibleType.includes(mediaType));
    }
}
