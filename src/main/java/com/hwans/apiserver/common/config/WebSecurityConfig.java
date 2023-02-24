package com.hwans.apiserver.common.config;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.security.jwt.JwtAccessDeniedHandler;
import com.hwans.apiserver.common.security.jwt.JwtAuthenticationEntryPoint;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.service.authentication.oauth2.CustomOAuth2UserService;
import com.hwans.apiserver.service.authentication.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.hwans.apiserver.service.authentication.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import com.hwans.apiserver.service.authentication.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
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
import org.springframework.web.cors.CorsConfiguration;

import java.util.Collections;

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

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Oauth 인증 성공 핸들러
     */
    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler();
    }

    /**
     * Oauth 인증 실패 핸들러
     */
    @Bean
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler();
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                    .httpBasic().disable()
                    .csrf().disable()
                    .cors().configurationSource(request -> corsConfiguration())
                .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    .exceptionHandling()
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler)
                .and()
                    .authorizeRequests()
                    .antMatchers(SWAGGER_PERMIT_URL_ARRAY).permitAll()
                    .antMatchers("/stomp/**").permitAll()
                    .antMatchers(
                            HttpMethod.GET,
                            Constants.API_PREFIX + "/v1/blog/*/posts/*/likes").authenticated()
                    .antMatchers(
                            HttpMethod.GET,
                            Constants.API_PREFIX + "/v1/blog/**",
                            Constants.API_PREFIX + "/v1/attachments/**").permitAll()
                    .antMatchers(
                            HttpMethod.PUT,
                            Constants.API_PREFIX + "/v1/accounts/password",
                            Constants.API_PREFIX + "/v1/blog/comments/*").permitAll()
                    .antMatchers(
                            HttpMethod.DELETE,
                            Constants.API_PREFIX + "/v1/blog/comments/*").permitAll()
                    .antMatchers(
                            HttpMethod.POST,
                            Constants.API_PREFIX + "/v1/accounts",
                            Constants.API_PREFIX + "/v1/accounts/verify-email",
                            Constants.API_PREFIX + "/v1/accounts/reset-password-email",
                            Constants.API_PREFIX + "/v1/authentication/token",
                            Constants.API_PREFIX + "/v1/authentication/refresh-token",
                            Constants.API_PREFIX + "/v1/blog/*/posts/*/comments/guest",
                            Constants.API_PREFIX + "/v1/blog/comments/*/guest").permitAll()
                    .antMatchers(Constants.API_PREFIX + "/v1/admin/**").hasRole("ADMIN")
                    //.antMatchers("/h2-console/**").permitAll() // Local H2 콘솔 테스트 환경
                    .anyRequest().authenticated()
                    //.and().headers().frameOptions().disable() // Local H2 콘솔 테스트 환경
                .and()
                    .apply(new JwtSecurityConfig(tokenProvider, redisTemplate))
                .and()
                    .oauth2Login()
                    .authorizationEndpoint()
                    .baseUri(Constants.API_PREFIX + "/v1/authentication/oauth2")
                    .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository())
                .and()
                    .userInfoEndpoint()
                    .userService(customOAuth2UserService)
                .and()
                    .redirectionEndpoint()
                    .baseUri(Constants.API_PREFIX + "/v1/authentication/oauth2/code/*")
                .and()
                    .successHandler(oAuth2AuthenticationSuccessHandler())
                    .failureHandler(oAuth2AuthenticationFailureHandler());
        return http.build();
    }

    /**
     * Cors 설정을 생성하여 반환한다.
     *
     * @return Cors 설정
     */
    private CorsConfiguration corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(Collections.singletonList(CorsConfiguration.ALL));
        return configuration.applyPermitDefaultValues();
    }
}
