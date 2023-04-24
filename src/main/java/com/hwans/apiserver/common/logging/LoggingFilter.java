package com.hwans.apiserver.common.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.marker.Markers;
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
import java.util.*;

@Component
@Slf4j(topic = "ioLog")
public class LoggingFilter extends OncePerRequestFilter {
    private static final String RequestType = "REQUEST";
    private static final String ResponseType = "RESPONSE";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            MDC.put("traceId", UUID.randomUUID().toString());
            MDC.put("clientIp", getRemoteAddr(request));
            if (isAsyncDispatch(request)) {
                filterChain.doFilter(request, response);
            } else {
                doFilterWrapped(new RequestWrapper(request), new ResponseWrapper(response), filterChain);
            }
        } finally {
            MDC.clear();
        }
    }

    protected void doFilterWrapped(RequestWrapper request, ContentCachingResponseWrapper response, FilterChain filterChain) throws ServletException, IOException {
        try {
            logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            logResponse(request, response);
            response.copyBodyToResponse();
        }
    }

    private static void logRequest(RequestWrapper request) throws IOException {
        var headers = new HashMap<String, Object>();
        headers.put("content-type", request.getContentType());
        headers.put("content-length", request.getContentLength());

        var marker = Markers.append("io", HttpData
                .builder()
                .Type(RequestType)
                .Uri(getUri(request))
                .Method(request.getMethod().toUpperCase())
                .Payload(getPayloadString(request.getContentType(), request.getInputStream()))
                .Headers(headers)
                .build());

        log.info(marker, null);
    }

    private static void logResponse(RequestWrapper request, ContentCachingResponseWrapper response) throws IOException {
        var headers = new HashMap<String, Object>();
        headers.put("content-type", response.getContentType());
        headers.put("content-length", response.getContentSize());

        var marker = Markers.append("io", HttpData
                .builder()
                .Type(ResponseType)
                .Uri(getUri(request))
                .Method(request.getMethod().toUpperCase())
                .HttpStatus(response.getStatus())
                .Payload(getPayloadString(response.getContentType(), response.getContentInputStream()))
                .Headers(headers)
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

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class HttpData {
        String Type;
        Integer HttpStatus;
        String Uri;
        String Method;
        String Payload;
        Map<String, Object> Headers;
    }
}
