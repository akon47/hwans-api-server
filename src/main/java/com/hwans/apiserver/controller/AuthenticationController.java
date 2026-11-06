package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.service.authentication.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

/**
 * 인증 Controller
 */
@RestController
@Tag(name = "인증")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @Operation(summary = "계정 인증 토큰 발급 (로그인)", description = "사용자 계정을 인증하여 토큰을 발급받는다.")
    @PostMapping(value = "/v1/authentication/token")
    public TokenDto issueToken(@Parameter(description = "사용자 계정 정보", required = true) @RequestBody AuthenticationInfoDto authenticationInfoDto) {
        return authenticationService.issueToken(authenticationInfoDto);
    }

    @Operation(summary = "계정 인증 토큰 반환 (로그아웃)", description = "현재 사용중인 AccessToken을 만료시킨다.")
    @DeleteMapping(value = "/v1/authentication/token")
    public void redeemToken(@Parameter(description = "AccessToken", required = true) @RequestHeader(value = Constants.AUTHORIZATION_HEADER) @NotBlank String accessToken) {
        authenticationService.redeemToken(accessToken);
    }

    @Operation(summary = "계정 인증 토큰 재발급", description = "인증 토큰 정보를 이용하여 토큰을 재발급받는다.")
    @PostMapping(value = "/v1/authentication/refresh-token")
    public TokenDto reissueToken(
            @Parameter(description = "AccessToken", required = true) @RequestHeader(value = Constants.AUTHORIZATION_HEADER) @NotBlank String accessToken,
            @Parameter(description = "RefreshToken", required = true) @RequestHeader(value = Constants.REFRESH_TOKEN_HEADER) @NotBlank String refreshToken) {
        return authenticationService.reissueToken(accessToken, refreshToken);
    }
}
