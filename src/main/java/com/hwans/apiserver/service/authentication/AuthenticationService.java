package com.hwans.apiserver.service.authentication;

import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;

public interface AuthenticationService {
    /**
     * 사용자 인증 토큰을 발급합니다.
     *
     * @param authenticationInfoDto 계정 인증 정보 데이터 모델
     * @return 발급된 토큰 데이터 모델
     */
    TokenDto issueToken(AuthenticationInfoDto authenticationInfoDto);

    /**
     * 사용자의 인증 토큰을 재발급합니다.
     *
     * @param accessToken 발급된 access token
     * @param refreshToken 발급된 refresh token
     * @return 재발급된 토큰 데이터 모델
     */
    TokenDto reissueToken(String accessToken, String refreshToken);

    /**
     * 사용자의 엑세스 토큰을 사용 중지시킵니다. (로그아웃)
     *
     * @param accessToken 엑세스 토큰
     */
    void redeemToken(String accessToken);
}
