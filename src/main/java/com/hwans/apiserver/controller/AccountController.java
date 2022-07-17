package com.hwans.apiserver.controller;

import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.service.AccountService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@Api(tags = "사용자 계정")
@RequestMapping(value = "/api")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @ApiOperation(value = "사용자 계정 생성", notes = "새로운 사용자 계정을 생성한다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/account")
    public AccountDto signup(@ApiParam(value = "새로운 사용자 생성 정보", required = true) @RequestBody AccountCreateDto userCreateDto) {
        return accountService.createAccount(userCreateDto);
    }

    @ApiOperation(value = "현재 사용자 계정 정보 조회", notes = "현재 사용자 계정 정보를 조회합니다.", tags = "사용자 계정")
    @GetMapping(value = "/v1/account/me")
    public AccountDto getActualAccount() {
        return accountService.getCurrentAccount();
    }
}
