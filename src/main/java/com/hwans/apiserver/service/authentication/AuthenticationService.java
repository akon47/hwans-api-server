package com.hwans.apiserver.service.authentication;

import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;

public interface AuthenticationService {
    /**
     * 사용자 인증 토큰을 발급합니다.
     *
     * @param authenticationInfoDto 계정 인증 정보 데이터 모델
     * @param forceIssueRefreshToken 이미 RefreshToken이 발급되어 있는 경우라도 항상 새로 발급할지 여부 (기존에 발급되어 있는 RefreshToken은 만료된다)
     * @return 발급된 토큰 데이터 모델
     */
    TokenDto issueToken(AuthenticationInfoDto authenticationInfoDto, boolean forceIssueRefreshToken);

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
     * @param withRefreshToken 사용자에게 발급되어 있는 리프레시 토큰도 같이 사용을 중지시킬지 여부
     */
    void redeemToken(String accessToken, boolean withRefreshToken);
}
