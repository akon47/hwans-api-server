package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends GenericFilterBean {
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var httpServletRequest = (HttpServletRequest) request;
        var bearerToken = httpServletRequest.getHeader(Constants.AUTHORIZATION_HEADER);
        var jwt = tokenProvider.extractTokenFromHeader(bearerToken);
        var requestURI = httpServletRequest.getRequestURI();

        if (StringUtils.hasText(jwt)) {
            var jwtStatus = tokenProvider.validateAccessToken(jwt);
            if (jwtStatus == JwtStatus.ACCESS) {
                var authentication = tokenProvider.getAuthentication(jwt);
                if ("issue".equals(redisTemplate.opsForValue().get(jwt))) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.trace("set Authentication to security context for '{}', uri: {}", authentication.getName(), requestURI);
                } else {
                    log.info("JWT token is black listed");
                    log.trace("JWT token is black listed, uri: {}, {}", requestURI);
                }
            } else if (jwtStatus == JwtStatus.EXPIRED) {
                var exception = new RestApiException(ErrorCodes.Unauthorized.TOKEN_EXPIRED);
                request.setAttribute("exception", exception);
                log.trace("JWT token is expired, uri: {}", requestURI);
            } else {
                log.trace("no valid JWT token found, uri: {}", requestURI);
            }
        } else {
            log.trace("JWT token is blank, uri: {}", requestURI);
        }

        chain.doFilter(request, response);
    }
}
