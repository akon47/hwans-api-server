package com.hwans.apiserver.service.account;

import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;

public interface AccountService {
    AccountDto createAccount(CreateAccountDto createAccountDto);
    AccountDto getCurrentAccount();
    String getCurrentAccountEmail();
    String setEmailVerifyCode(String email);
}
