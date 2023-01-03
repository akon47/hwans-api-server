package com.hwans.apiserver.service.authentication;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 현재 사용자의 정보를 넣어주기 위한 어노테이션
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@AuthenticationPrincipal(expression = "@fetchCurrentUserAuthenticationDetails.apply(#this == 'anonymousUser' ? null : #this)")
public @interface CurrentAuthenticationDetails {
}