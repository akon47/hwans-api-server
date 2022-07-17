package com.hwans.apiserver.controller;

import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.authentication.SigninDto;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.service.AuthenticationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "인증")
@RequestMapping(value = "/api")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @ApiOperation(value = "계정 인증 토큰 발급", notes = "사용자 계정을 인증하여 토큰을 발급받는다.", tags = "인증")
    @PostMapping(value = "/v1/authentication/token")
    public TokenDto signin(@ApiParam(value = "사용자 계정 정보", required = true) @RequestBody SigninDto signinDto) {
        return authenticationService.authenticate(signinDto);
    }
}
