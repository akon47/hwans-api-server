package com.hwans.apiserver.common.config;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.security.jwt.JwtAccessDeniedHandler;
import com.hwans.apiserver.common.security.jwt.JwtAuthenticationEntryPoint;
import com.hwans.apiserver.common.security.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@Component
public class WebSecurityConfig {
    // 인증 없이 허용할 Swagger 관련 Urls
    private static final String[] SWAGGER_PERMIT_URL_ARRAY = {
            "/",
            "/v3/api-docs",
            "/swagger-ui/**",
            "/swagger-resources/**",
    };

    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .httpBasic().disable()
                .csrf().disable()
                .cors().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)

                .and()
                .exceptionHandling()
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler)

                .and()
                .authorizeRequests()
                .antMatchers(SWAGGER_PERMIT_URL_ARRAY).permitAll()
                .antMatchers("/stomp/**").permitAll()
                .antMatchers(
                        HttpMethod.POST,
                        Constants.API_PREFIX + "/v1/account",
                        Constants.API_PREFIX + "/v1/account/verify-email",
                        Constants.API_PREFIX + "/v1/authentication/token",
                        Constants.API_PREFIX + "/v1/authentication/refresh-token").permitAll()
                //.antMatchers("/h2-console/**").permitAll() // Local H2 콘솔 테스트 환경
                .anyRequest().authenticated()

                //.and().headers().frameOptions().disable() // Local H2 콘솔 테스트 환경

                .and()
                .apply(new JwtSecurityConfig(tokenProvider, redisTemplate));

        return http.build();
    }
}
