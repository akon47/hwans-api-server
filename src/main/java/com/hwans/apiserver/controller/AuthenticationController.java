package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.service.authentication.AuthenticationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@RestController
@Api(tags = "인증")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @ApiOperation(value = "계정 인증 토큰 발급", notes = "사용자 계정을 인증하여 토큰을 발급받는다. (로그인)", tags = "인증")
    @PostMapping(value = "/v1/authentication/token")
    public TokenDto issueToken(@ApiParam(value = "사용자 계정 정보", required = true) @RequestBody AuthenticationInfoDto authenticationInfoDto) {
        return authenticationService.issueToken(authenticationInfoDto);
    }

    @ApiOperation(value = "계정 인증 토큰 반환", notes = "현재 사용중인 AccessToken 을 반환한다. (로그아웃)", tags = "인증")
    @DeleteMapping(value = "/v1/authentication/token")
    public void redeemToken(@ApiParam(value = "AccessToken", required = true) @RequestHeader(value = Constants.AUTHORIZATION_HEADER) @NotBlank String accessToken) {
        authenticationService.redeemToken(accessToken);
    }

    @ApiOperation(value = "계정 인증 토큰 재발급", notes = "인증 토큰 정보를 이용하여 토큰을 재발급받는다.", tags = "인증")
    @PostMapping(value = "/v1/authentication/refresh-token")
    public TokenDto reissueToken(
            @ApiParam(value = "AccessToken", required = true) @RequestHeader(value = Constants.AUTHORIZATION_HEADER) @NotBlank String accessToken,
            @ApiParam(value = "RefreshToken", required = true) @RequestHeader(value = Constants.REFRESH_TOKEN_HEADER) @NotBlank String refreshToken) {
        return authenticationService.reissueToken(accessToken, refreshToken);
    }
}
