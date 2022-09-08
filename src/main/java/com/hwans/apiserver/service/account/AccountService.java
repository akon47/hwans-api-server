package com.hwans.apiserver.service.account;

import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;

import java.util.UUID;

public interface AccountService {
    AccountDto createAccount(CreateAccountDto createAccountDto);
    AccountDto createAccount(CreateAccountDto createAccountDto, String registerToken);
    AccountDto getCurrentAccount();
    String getCurrentAccountEmail();
    String setEmailVerifyCode(String email);
    AccountDto setProfileImage(UUID accountId, UUID fileId);
    AccountDto getAccount(UUID accountId);
}
