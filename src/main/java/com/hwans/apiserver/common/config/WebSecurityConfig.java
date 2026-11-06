package com.hwans.apiserver.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.security.jwt.JwtAccessDeniedHandler;
import com.hwans.apiserver.common.security.jwt.JwtAuthenticationEntryPoint;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.common.security.token.AccessTokenAuthenticationFilter;
import com.hwans.apiserver.service.authentication.oauth2.CustomOAuth2UserService;
import com.hwans.apiserver.service.authentication.oauth2.handler.OAuth2AuthenticationFailureHandler;
import com.hwans.apiserver.service.authentication.oauth2.handler.OAuth2AuthenticationSuccessHandler;
import com.hwans.apiserver.service.authentication.oauth2.repository.HttpCookieOAuth2AuthorizationRequestRepository;
import com.hwans.apiserver.service.token.AccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Collections;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
    private static final String API = Constants.API_PREFIX;

    // 인증 없이 허용할 OpenAPI 문서(springdoc) Urls
    private static final String[] SWAGGER_PERMIT_URL_ARRAY = {
            "/",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
    };

    // 인증 없이 허용할 AI 에이전트용 문서·도우미 스크립트, 검색엔진 크롤러용 서버 렌더 페이지 Urls
    private static final String[] AGENT_DOCS_PERMIT_URL_ARRAY = {
            "/agents.md",
            "/llms.txt",
            "/cli/**",
            "/prerender",
            "/prerender/**",
    };

    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AccessTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    @Value("${allowedOrigins}")
    private String[] allowedOrigins;

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
        // 서블릿이 여러 개(H2 콘솔 등)여도 모호하지 않게 PathPattern 매처를 명시적으로 쓴다.
        var matcher = PathPatternRequestMatcher.withDefaults();
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> corsConfiguration()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(matchers(matcher, null, SWAGGER_PERMIT_URL_ARRAY)).permitAll()
                        .requestMatchers(matchers(matcher, null, "/stomp/**", "/ws/**")).permitAll()
                        .requestMatchers(matchers(matcher, HttpMethod.GET, "/rss", "/rss/**", "/sitemap.xml")).permitAll()
                        .requestMatchers(matchers(matcher, HttpMethod.GET, AGENT_DOCS_PERMIT_URL_ARRAY)).permitAll()
                        // 사용자별 GET 은 blog/** permitAll 보다 먼저 authenticated 로 명시해야 한다 (순서 중요)
                        .requestMatchers(matchers(matcher, HttpMethod.GET,
                                API + "/v1/blog/*/posts/*/likes",
                                API + "/v1/blog/*/posts/*/bookmarks",
                                API + "/v1/blog/comments/*/likes",
                                API + "/v1/blog/me/bookmarks",
                                API + "/v1/blog/me/statistics")).authenticated()
                        .requestMatchers(matchers(matcher, HttpMethod.GET,
                                API + "/v1/blog/**",
                                API + "/v1/attachments/**")).permitAll()
                        .requestMatchers(matchers(matcher, HttpMethod.PUT,
                                API + "/v1/accounts/password",
                                API + "/v1/blog/comments/*")).permitAll()
                        .requestMatchers(matchers(matcher, HttpMethod.DELETE,
                                API + "/v1/blog/comments/*")).permitAll()
                        .requestMatchers(matchers(matcher, HttpMethod.POST,
                                API + "/v1/accounts",
                                API + "/v1/accounts/verify-email",
                                API + "/v1/accounts/reset-password-email",
                                API + "/v1/authentication/token",
                                API + "/v1/authentication/refresh-token",
                                // 기기 로그인: 에이전트가 코드를 요청하고 토큰을 폴링하는 엔드포인트 (승인/거부는 로그인 필요)
                                API + "/v1/authentication/device/code",
                                API + "/v1/authentication/device/token",
                                API + "/v1/blog/*/posts/*/comments/guest",
                                API + "/v1/blog/comments/*/guest")).permitAll()
                        .requestMatchers(matchers(matcher, null, API + "/v1/admin/**")).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .with(new JwtSecurityConfig(tokenProvider, redisTemplate,
                        new AccessTokenAuthenticationFilter(accessTokenService, objectMapper)), Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri(API + "/v1/authentication/oauth2")
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository()))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .redirectionEndpoint(redirection -> redirection.baseUri(API + "/v1/authentication/oauth2/code/*"))
                        .successHandler(oAuth2AuthenticationSuccessHandler())
                        .failureHandler(oAuth2AuthenticationFailureHandler()));
        return http.build();
    }

    private static RequestMatcher[] matchers(PathPatternRequestMatcher.Builder builder, HttpMethod method, String... patterns) {
        return Arrays.stream(patterns)
                .map(pattern -> builder.matcher(method, pattern))
                .toArray(RequestMatcher[]::new);
    }

    /**
     * Cors 설정을 생성하여 반환한다.
     *
     * @return Cors 설정
     */
    private CorsConfiguration corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(Collections.singletonList(CorsConfiguration.ALL));
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        configuration.setAllowCredentials(true);
        return configuration.applyPermitDefaultValues();
    }
}
