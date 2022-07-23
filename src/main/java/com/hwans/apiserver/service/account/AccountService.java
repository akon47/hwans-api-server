package com.hwans.apiserver.service.account;

import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;

public interface AccountService {
    AccountDto createAccount(AccountCreateDto userCreateDto);
    AccountDto getCurrentAccount();
}
