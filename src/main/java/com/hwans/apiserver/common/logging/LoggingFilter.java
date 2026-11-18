package com.hwans.apiserver.common.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hwans.apiserver.common.Constants;
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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j(topic = "ioLog")
public class LoggingFilter extends OncePerRequestFilter {
    private static final String RequestType = "REQUEST";
    private static final String ResponseType = "RESPONSE";
    private static final String MASKED = "***";

    /**
     * 값을 로그에 남기지 않을 헤더 (자격증명)
     */
    private static final Set<String> SECRET_HEADERS = Set.of(
            Constants.AUTHORIZATION_HEADER.toLowerCase(),
            Constants.REFRESH_TOKEN_HEADER.toLowerCase(),
            "cookie",
            "set-cookie"
    );

    /**
     * 요청·응답 본문을 로그에 남기지 않을 URI 접두어 (비밀번호, 발급된 토큰, 기기 로그인 코드가 오가는 곳)
     */
    private static final List<String> SECRET_URI_PREFIXES = List.of(
            Constants.API_PREFIX + "/v1/authentication/",
            Constants.API_PREFIX + "/v1/tokens",
            Constants.API_PREFIX + "/v1/accounts/password",
            Constants.API_PREFIX + "/v1/ai/"
    );

    /**
     * 응답을 감싸지 않을 URI 접두어. AI 글쓰기는 SSE 로 조금씩 흘려보내는데, ContentCachingResponseWrapper 로 감싸면
     * 본문이 버퍼에 쌓여 클라이언트에 바로 가지 않는다. (본문은 어차피 남기지 않는다 — API 키·글 내용)
     */
    private static final List<String> UNWRAPPED_URI_PREFIXES = List.of(
            Constants.API_PREFIX + "/v1/ai/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var traceId = UUID.randomUUID().toString();
        var clientIp = getRemoteAddr(request);

        try {
            MDC.put("traceId", traceId);
            MDC.put("clientIp", clientIp);

            if (isAsyncDispatch(request)) {
                filterChain.doFilter(request, response);
            } else if (isUnwrappedUri(request)) {
                doFilterUnwrapped(request, response, filterChain);
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
            logRequest(request, request.getContentAsByteArray());
        } finally {
            logResponse(request, response, response.getContentSize(), response.getContentAsByteArray());
            response.copyBodyToResponse();
        }
    }

    /**
     * 감싸지 않고 그대로 넘기고, 본문 없이 메타데이터만 남긴다.
     */
    protected void doFilterUnwrapped(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
            logRequest(request, null);
        } finally {
            logResponse(request, response, -1, null);
        }
    }

    private static void logRequest(HttpServletRequest request, byte[] body) {
        var headers = Collections.list(request.getHeaderNames()).stream()
                .distinct().collect(Collectors.toMap(name -> name, name -> maskHeader(name, Collections.list(request.getHeaders(name)))));

        var secretBody = isSecretUri(request);
        var parameters = Collections.list(request.getParameterNames()).stream()
                .collect(Collectors.toMap(name -> name, name -> secretBody ? MASKED : request.getParameter(name)));

        var marker = Markers.append("io", HttpData
                .builder()
                .type(RequestType)
                .uri(getUri(request))
                .method(request.getMethod().toUpperCase())
                .contentType(request.getContentType())
                .contentLength(request.getContentLength())
                .payload(secretBody || body == null ? null : getPayloadString(request.getContentType(), body))
                .headers(headers)
                .parameters(parameters)
                .build());

        log.info(marker, null);
    }

    private static void logResponse(HttpServletRequest request, HttpServletResponse response, int contentSize, byte[] body) {
        var headers = response.getHeaderNames().stream()
                .distinct().collect(Collectors.toMap(name -> name, name -> maskHeader(name, response.getHeaders(name))));

        var marker = Markers.append("io", HttpData
                .builder()
                .type(ResponseType)
                .uri(getUri(request))
                .method(request.getMethod().toUpperCase())
                .contentType(response.getContentType())
                .contentLength(contentSize)
                .httpStatus(response.getStatus())
                .payload(isSecretUri(request) || body == null ? null : getPayloadString(response.getContentType(), body))
                .headers(headers)
                .build());

        log.info(marker, null);
    }

    /**
     * 자격증명 헤더는 값 대신 마스킹한 값을 남긴다.
     */
    private static List<String> maskHeader(String name, Collection<String> values) {
        if (SECRET_HEADERS.contains(name.toLowerCase())) {
            return values.stream().map(value -> MASKED).toList();
        }
        return new ArrayList<>(values);
    }

    /**
     * 비밀번호·토큰이 본문에 오가는 요청인지 여부
     */
    private static boolean isSecretUri(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return SECRET_URI_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private static boolean isUnwrappedUri(HttpServletRequest request) {
        var uri = request.getRequestURI();
        return UNWRAPPED_URI_PREFIXES.stream().anyMatch(uri::startsWith);
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
