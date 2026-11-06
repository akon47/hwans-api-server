package com.hwans.apiserver.common.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.common.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청에 401 과 공통 오류 JSON 을 돌려준다. (JwtFilter 가 request attribute 에 넣어 둔 예외가 있으면 그 코드를 쓴다)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        var restApiException = new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED);
        var exception = request.getAttribute("exception");
        if (exception instanceof RestApiException attributeException) {
            restApiException = attributeException;
        }

        response.getWriter().print(objectMapper.writeValueAsString(new ErrorResponseDto(restApiException)));
    }
}
