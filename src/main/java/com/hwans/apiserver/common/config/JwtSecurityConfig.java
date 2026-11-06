package com.hwans.apiserver.common.config;

import com.hwans.apiserver.common.security.jwt.JwtFilter;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.common.security.token.AccessTokenAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bearer 토큰 인증 필터 등록.
 * 개인 액세스 토큰(hsp_…) 필터가 먼저 돌고, 그 뒤에 JWT(웹 로그인) 필터가 돈다. 둘은 토큰 접두어로 서로 관여하지 않는다.
 */
@RequiredArgsConstructor
public class JwtSecurityConfig extends AbstractHttpConfigurer<JwtSecurityConfig, HttpSecurity> {
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final AccessTokenAuthenticationFilter accessTokenAuthenticationFilter;

    @Override
    public void configure(HttpSecurity http) {
        JwtFilter customFilter = new JwtFilter(tokenProvider, redisTemplate);
        http.addFilterBefore(customFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(accessTokenAuthenticationFilter, JwtFilter.class);
    }
}
