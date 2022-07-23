package com.hwans.apiserver.service;

import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;

public interface AuthenticationService {
    TokenDto authenticate(AuthenticationInfoDto authenticationInfoDto);
    TokenDto reissueToken(String accessToken, String refreshToken);
}
