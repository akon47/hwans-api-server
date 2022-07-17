package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.errors.dto.ErrorResponseDto;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import nonapi.io.github.classgraph.json.JSONSerializer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        response.getWriter().print(JSONSerializer.serializeObject(ErrorResponseDto.builder()
                .name(ErrorCodes.Unauthorized.UNAUTHORIZED.getName())
                .message("유효한 자격증명이 존재하지 않습니다.")
                .build()));
    }
}
