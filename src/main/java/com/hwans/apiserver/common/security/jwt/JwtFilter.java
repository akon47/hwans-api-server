package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
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
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String bearerToken = httpServletRequest.getHeader(Constants.AUTHORIZATION_HEADER);
        String jwt = tokenProvider.extractTokenFromHeader(bearerToken);
        String requestURI = httpServletRequest.getRequestURI();

        if (StringUtils.hasText(jwt)) {
            var jwtStatus = tokenProvider.validateAccessToken(jwt);
            if (jwtStatus == JwtStatus.ACCESS) {
                Authentication authentication = tokenProvider.getAuthentication(jwt);
                if(redisTemplate.opsForValue().get(authentication.getName()) == null) {
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.trace("set Authentication to security context for '{}', uri: {}", authentication.getName(), requestURI);
                } else {
                    log.info("JWT token is black listed");
                    log.trace("JWT token is black listed, uri: {}, {}", requestURI, redisTemplate.opsForValue().get(authentication.getName()));
                }
            } else if (jwtStatus == JwtStatus.EXPIRED) {
                request.setAttribute("exception", new RestApiException(ErrorCodes.Unauthorized.TOKEN_EXPIRED));
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
