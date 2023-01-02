package com.hwans.apiserver.service.account;

import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.account.ModifyAccountDto;
import com.hwans.apiserver.dto.account.ResetPasswordDto;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import java.util.UUID;

/**
 * 계정 서비스 인터페이스
 */
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

    /**
     * 계정의 프로필 이미지를 설정(변경)합니다.
     *
     * @param accountId 변경할 대상 계정 Id
     * @param fileId 프로필 이미지의 Id
     * @return 변경된 계정에 대한 데이터 모델
     */
    AccountDto setProfileImage(UUID accountId, UUID fileId);

    /**
     * 계정을 조회합니다.
     *
     * @param accountId 조회할 계정 Id
     * @return 계정 데이터 모델
     */
    AccountDto getAccount(UUID accountId);
}
