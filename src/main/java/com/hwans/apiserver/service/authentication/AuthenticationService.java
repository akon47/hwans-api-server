package com.hwans.apiserver.service.authentication;

import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;

public interface AuthenticationService {
    TokenDto issueToken(AuthenticationInfoDto authenticationInfoDto, boolean forceIssueRefreshToken);
    TokenDto reissueToken(String accessToken, String refreshToken);
    void redeemToken(String accessToken, boolean withRefreshToken);
}
