package com.hwans.apiserver.common.security.jwt;

import com.hwans.apiserver.common.errors.dto.ErrorResponseDto;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
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

        var restApiException = new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED);
        var exception = request.getAttribute("exception");
        if(RestApiException.class.isInstance(exception)) {
            restApiException = RestApiException.class.cast(exception);
        }

        response.getWriter().print(JSONSerializer.serializeObject(new ErrorResponseDto(restApiException)));
    }
}
