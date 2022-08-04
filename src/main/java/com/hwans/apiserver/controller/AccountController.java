package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.service.account.AccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "사용자 계정")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @ApiOperation(value = "사용자 계정 생성", notes = "새로운 사용자 계정을 생성한다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/accounts")
    public AccountDto signup(@ApiParam(value = "새로운 사용자 생성 정보", required = true) @RequestBody AccountCreateDto userCreateDto) {
        return accountService.createAccount(userCreateDto);
    }

    @ApiOperation(value = "사용자 이메일 인증 코드 발송", notes = "새로운 사용자 계정 생성을 위해 이메일 인증 코드를 발송합니다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/accounts/verify-email")
    public void verifyEmail(@ApiParam(value = "인증 코드를 발송할 이메일 주소", required = true) @RequestParam String email) {
        accountService.sendEmailVerifyCode(email);
    }

    @ApiOperation(value = "현재 사용자 계정 정보 조회", notes = "현재 사용자 계정 정보를 조회합니다.", tags = "사용자 계정")
    @GetMapping(value = "/v1/accounts/me")
    public AccountDto getActualAccount() {
        return accountService.getCurrentAccount();
    }
}
