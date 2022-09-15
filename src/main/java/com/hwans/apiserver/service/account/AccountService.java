package com.hwans.apiserver.service.account;

import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.account.ModifyAccountDto;
import com.hwans.apiserver.dto.account.ResetPasswordDto;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.UUID;

@Validated
public interface AccountService {
    @Validated(CreateAccountDto.ByEmailVerifyCode.class)
    AccountDto createAccount(@Valid CreateAccountDto createAccountDto);
    @Validated(CreateAccountDto.ByRegisterToken.class)
    AccountDto createAccount(@Valid CreateAccountDto createAccountDto, String registerToken);
    AccountDto modifyAccount(UUID accountId, ModifyAccountDto modifyAccountDto);
    void resetPassword(ResetPasswordDto resetPasswordDto);
    AccountDto getCurrentAccount();
    String getCurrentAccountEmail();
    String setEmailVerifyCode(String email);
    String setResetPasswordToken(String email);
    AccountDto setProfileImage(UUID accountId, UUID fileId);
    AccountDto getAccount(UUID accountId);
}
