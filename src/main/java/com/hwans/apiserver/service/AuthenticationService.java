package com.hwans.apiserver.service;

import com.hwans.apiserver.dto.authentication.SigninDto;
import com.hwans.apiserver.dto.authentication.TokenDto;

public interface AuthenticationService {
    TokenDto authenticate(SigninDto signinDto);
}
