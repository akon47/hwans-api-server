package com.hwans.apiserver.common.config;

import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetailsOrElseNull;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI(springdoc) 문서 설정. UI 는 /swagger-ui/index.html, 명세는 /v3/api-docs.
 * Bearer 토큰(웹 로그인 JWT 또는 개인 액세스 토큰 hsp_…)을 모든 API 의 기본 보안 요구사항으로 둔다.
 */
@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "JWT";

    static {
        // 현재 사용자 주입용 파라미터는 문서에 노출하지 않는다.
        SpringDocUtils.getConfig()
                .addAnnotationsToIgnore(CurrentAuthenticationDetails.class, CurrentAuthenticationDetailsOrElseNull.class)
                .addRequestWrapperToIgnore(UserAuthenticationDetails.class);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hwans-API-Server")
                        .description("Hwan'Story 블로그 API. 웹 로그인 JWT 또는 AI 에이전트용 개인 액세스 토큰(hsp_…)을 `Authorization: Bearer` 로 보낸다. 에이전트 가이드: /agents.md")
                        .version("v1"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
