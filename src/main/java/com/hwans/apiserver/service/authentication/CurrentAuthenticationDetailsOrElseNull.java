package com.hwans.apiserver.service.authentication;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 사용자의 정보를 넣어주기 위한 어노테이션
 * (인증되지 않은 사용자의 경우 null을 설정한다)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : @fetchCurrentUserAuthenticationDetails.apply(#this)")
public @interface CurrentAuthenticationDetailsOrElseNull {
}