package com.hwans.apiserver.common.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "ioLog")
public class LoggingFilter extends OncePerRequestFilter {
    private static final String RequestType = "REQUEST";
    private static final String ResponseType = "RESPONSE";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var traceId = UUID.randomUUID().toString();
        var clientIp = getRemoteAddr(request);

        try {
            MDC.put("traceId", traceId);
            MDC.put("clientIp", clientIp);

            if (isAsyncDispatch(request)) {
                filterChain.doFilter(request, response);
            } else {
                doFilterWrapped(new RequestWrapper(request), new ResponseWrapper(response), filterChain);
            }
        } finally {
            MDC.clear();
        }
    }

    protected void doFilterWrapped(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
            logRequest(request);
        } finally {
            logResponse(request, response);
            response.copyBodyToResponse();
        }
    }

    private static void logRequest(ContentCachingRequestWrapper request) {
        var headers = Collections.list(request.getHeaderNames()).stream()
                .distinct().collect(Collectors.toMap(name -> name, name -> Collections.list(request.getHeaders(name)).stream().toList()));

        var parameters = Collections.list(request.getParameterNames()).stream()
                .collect(Collectors.toMap(name -> name, request::getParameter));

        var marker = Markers.append("io", HttpData
                .builder()
                .type(RequestType)
                .uri(getUri(request))
                .method(request.getMethod().toUpperCase())
                .contentType(request.getContentType())
                .contentLength(request.getContentLength())
                .payload(getPayloadString(request.getContentType(), request.getContentAsByteArray()))
                .headers(headers)
                .parameters(parameters)
                .build());

        log.info(marker, null);
    }

    private static void logResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response) {
        var headers = response.getHeaderNames().stream()
                .distinct().collect(Collectors.toMap(name -> name, name -> response.getHeaders(name).stream().toList()));

        var marker = Markers.append("io", HttpData
                .builder()
                .type(ResponseType)
                .uri(getUri(request))
                .method(request.getMethod().toUpperCase())
                .contentType(response.getContentType())
                .contentLength(response.getContentSize())
                .httpStatus(response.getStatus())
                .payload(getPayloadString(response.getContentType(), response.getContentAsByteArray()))
                .headers(headers)
                .build());

        log.info(marker, null);
    }

    private static String getRemoteAddr(HttpServletRequest request) {
        var clientIp = request.getHeader("X-Forwarded-For");

        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("WL-Proxy-Client-IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_CLIENT_IP");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (clientIp == null || clientIp.length() == 0 || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = request.getRemoteAddr();
        }

        return clientIp;
    }

    private static String getUri(HttpServletRequest request) {
        var queryString = request.getQueryString();
        return queryString == null ? request.getRequestURI() : request.getRequestURI() + "?" + queryString;
    }

    private static String getPayloadString(String contentType, byte[] content) {
        boolean visible = isVisible(MediaType.valueOf(contentType == null ? "application/json" : contentType));
        if (visible) {
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

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private static class HttpData {
        String type;
        Integer httpStatus;
        String contentType;
        Integer contentLength;
        String uri;
        String method;
        String payload;
        Map<String, List<String>> headers;
        Map<String, String> parameters;

        public Integer getContentLength() {
            if (this.contentLength < 0) {
                return null;
            }

            return this.contentLength;
        }
    }
}
