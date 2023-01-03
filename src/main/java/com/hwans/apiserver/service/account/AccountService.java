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
    /**
     * 계정을 생성한다.
     *
     * @param createAccountDto 계정 생성 데이터 모델 (인증코드가 필요하다)
     * @return 생성된 계정 데이터 모델
     */
    @Validated(CreateAccountDto.ByEmailVerifyCode.class)
    AccountDto createAccount(@Valid CreateAccountDto createAccountDto);

    /**
     * 계정 등록 토큰을 이용하여 계정을 생성한다. (인증코드가 필요 없다)
     *
     * @param createAccountDto 계정 생성 데이터 모델
     * @param registerToken 계정 등록 토큰
     * @return 생성된 계정 데이터 모델
     */
    @Validated(CreateAccountDto.ByRegisterToken.class)
    AccountDto createAccount(@Valid CreateAccountDto createAccountDto, String registerToken);

    /**
     * 사용자 계정의 정보를 수정한다.
     *
     * @param accountId 계정 Id
     * @param modifyAccountDto 계정의 수정을 위한 데이터 모델
     * @return 수정된 사용자 계정 데이터 모델
     */
    AccountDto modifyAccount(UUID accountId, ModifyAccountDto modifyAccountDto);

    /**
     * 사용자 계정의 비밀번호를 변경한다.
     *
     * @param resetPasswordDto 비밀번호 재설정 데이터 모델
     */
    void resetPassword(ResetPasswordDto resetPasswordDto);

    /**
     * 현재 인증되어 있는 사용자 계정을 얻는다.
     *
     * @return 사용자 계정 데이터 모델
     */
    AccountDto getCurrentAccount();

    /**
     * 현재 인증 정보에 따른 계정의 이메일 주소를 얻는다.
     *
     * @return 이메일
     */
    String getCurrentAccountEmail();

    /**
     * 계정의 인증코드 발급을 위한 인증코드를 발급한다.
     *
     * @param email 인증코드 발급을 원하는 이메일
     * @return 인증코드
     */
    String setEmailVerifyCode(String email);

    /**
     * 계정의 비밀번호 리셋을 위한 비밀번호 리셋 토큰을 발급한다.
     *
     * @param email 비밀번호 리셋 토큰 발급을 원하는 이메일
     * @return 토큰
     */
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
