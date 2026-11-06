package com.hwans.apiserver.common.security.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.ClientIp;
import com.hwans.apiserver.dto.common.ErrorResponseDto;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.token.AccessTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code Authorization: Bearer hsp_...} (개인 액세스 토큰) 인증 필터.
 * 토큰이 유효하면 토큰 주인으로 인증 컨텍스트를 만들고(관리자 역할은 제외), 토큰 권한으로 할 수 없는 요청은 여기서 403 으로 끝낸다.
 * JWT(웹 로그인) 요청에는 관여하지 않는다 — 그쪽은 {@code JwtFilter}.
 */
@RequiredArgsConstructor
@Slf4j
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {
    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        var bearer = extractBearer(request.getHeader(Constants.AUTHORIZATION_HEADER));
        if (bearer == null || !bearer.startsWith(AccessTokenService.PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        var requestUri = request.getRequestURI();
        var clientIp = ClientIp.of(request);
        var authenticated = accessTokenService.authenticate(bearer, clientIp);
        if (authenticated.isEmpty()) {
            log.info("invalid access token presented, uri: {}, ip: {}", requestUri, clientIp);
            writeError(response, new RestApiException(ErrorCodes.Unauthorized.INVALID_ACCESS_TOKEN));
            return;
        }

        var details = authenticated.get();
        var decision = AccessTokenAccessRules.decide(request.getMethod(), requestUri, details.getScopes());
        if (decision.kind() == AccessTokenAccessRules.Kind.DENY) {
            log.info("access token {} may not call {} {}", details.getTokenId(), request.getMethod(), requestUri);
            writeError(response, new RestApiException(ErrorCodes.Forbidden.ACCESS_TOKEN_NOT_ALLOWED));
            return;
        }
        if (decision.kind() == AccessTokenAccessRules.Kind.SCOPE) {
            var scope = decision.scope().getKey();
            log.info("access token {} lacks scope '{}' for {} {}", details.getTokenId(), scope, request.getMethod(), requestUri);
            writeError(response, new RestApiException(ErrorCodes.Forbidden.ACCESS_TOKEN_SCOPE_REQUIRED,
                    "이 작업에는 토큰 권한 '" + scope + "' 이(가) 필요합니다. (이 토큰의 권한: " + String.join(", ", details.getScopes()) + ")"));
            return;
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        details.getRoles().stream()
                .filter(role -> !RoleType.ADMIN.getName().equals(role)) // 토큰으로는 관리자 기능을 쓸 수 없다
                .forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        details.getScopes().forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));

        var principal = new UserAuthenticationDetails(details.getAccountEmail(), authorities);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(details);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.trace("access token {} authenticated as '{}', uri: {}", details.getTokenId(), details.getAccountEmail(), requestUri);

        chain.doFilter(request, response);
    }

    private static String extractBearer(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        var trimmed = header.trim();
        if (!trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        var token = trimmed.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private void writeError(HttpServletResponse response, RestApiException exception) throws IOException {
        response.setStatus(exception.getErrorCode().getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().print(objectMapper.writeValueAsString(new ErrorResponseDto(exception)));
    }
}
