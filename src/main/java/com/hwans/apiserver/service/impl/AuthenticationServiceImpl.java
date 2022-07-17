package com.hwans.apiserver.service.impl;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.jwt.TokenProvider;
import com.hwans.apiserver.dto.authentication.SigninDto;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService, UserDetailsService {
    private final AccountRepository accountRepository;
    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final PasswordEncoder passwordEncoder;

    private static final String NO_ACCOUNT_ID = "계정 Id 정보를 찾을 수 없습니다."; // TODO: 보안적으로 문제가 될 수 있는 정보 노출이므로 예외 메시지 수정 필요
    private static final String NO_PASSWORD_MATCH = "계정 비밀번호가 잘못되었습니다."; // TODO: 보안적으로 문제가 될 수 있는 정보 노출이므로 예외 메시지 수정 필요

    @Override
    public TokenDto authenticate(SigninDto signinDto) {
        var foundAccount = accountRepository.findById(signinDto.getId())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_ACCOUNT_ID));
        if (!passwordEncoder.matches(signinDto.getPassword(), foundAccount.getPassword())) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST, NO_PASSWORD_MATCH);
        }

        var authenticationToken = new UsernamePasswordAuthenticationToken(signinDto.getId(), signinDto.getPassword());
        var authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return tokenProvider.createToken(authentication);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accountRepository.findById(username)
                .map(this::createUserDetails)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_ACCOUNT_ID));
    }

    private UserDetails createUserDetails(Account account) {
        var authorities = account.getRoles().stream()
                .map(x -> new SimpleGrantedAuthority(x.getName()))
                .collect(Collectors.toList());
        return new User(
                String.valueOf(account.getId()),
                account.getPassword(),
                authorities);
    }
}