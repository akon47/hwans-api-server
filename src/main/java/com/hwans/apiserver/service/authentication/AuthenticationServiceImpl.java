package com.hwans.apiserver.service.authentication;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.dto.authentication.AuthenticationInfoDto;
import com.hwans.apiserver.dto.authentication.TokenDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService, UserDetailsService {
    private final AccountRepository accountRepository;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String NO_ACCOUNT_ID = "계정 Id 정보를 찾을 수 없습니다."; // TODO: 보안적으로 문제가 될 수 있는 정보 노출이므로 예외 메시지 수정 필요
    private static final String NO_PASSWORD_MATCH = "계정 비밀번호가 잘못되었습니다."; // TODO: 보안적으로 문제가 될 수 있는 정보 노출이므로 예외 메시지 수정 필요

    /**
     * 사용자 인증 토큰을 발급합니다.
     * @param authenticationInfoDto 사용자 인중 정보
     * @return 발급된 토큰
     */
    @Override
    @Transactional
    public TokenDto issueToken(AuthenticationInfoDto authenticationInfoDto) {
        var foundAccount = accountRepository
                .findByEmailAndDeletedIsFalse(authenticationInfoDto.getEmail())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_ACCOUNT_ID));
        if (!passwordEncoder.matches(authenticationInfoDto.getPassword(), foundAccount.getPassword())) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST, NO_PASSWORD_MATCH);
        }
        var authenticationToken = new UsernamePasswordAuthenticationToken(authenticationInfoDto.getEmail(), authenticationInfoDto.getPassword());
        var authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var token = tokenProvider.createToken(authentication);
        accountRepository.save(foundAccount.withRefreshToken(token.getRefreshToken()));
        return token;
    }

    /**
     * 사용자의 엑세스 토큰을 사용 중지시킵니다.
     * @param accessToken 엑세스 토큰
     */
    @Override
    @Transactional
    public void redeemToken(String accessToken) {
        accessToken = tokenProvider.extractTokenFromHeader(accessToken);
        var accountEmail = tokenProvider
                .getAccountEmailFromAccessToken(accessToken)
                .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));

        accountRepository
                .findByEmailAndDeletedIsFalse(accountEmail)
                .ifPresent((foundAccount) ->
                {
                    foundAccount.clearRefreshToken();
                    accountRepository.save(foundAccount);
                });

        redisTemplate.opsForValue().set(accessToken, "redeem", Duration.ofMillis(Constants.ACCESS_TOKEN_EXPIRES_TIME));
    }

    /**
     * @param accessToken 엑세스 토큰
     * @param refreshToken 리프레시 토큰
     * @return 재발급된 토큰
     */
    @Override
    @Transactional
    public TokenDto reissueToken(String accessToken, String refreshToken) {
        accessToken = tokenProvider.extractTokenFromHeader(accessToken);
        refreshToken = tokenProvider.extractTokenFromHeader(refreshToken);

        var accountEmail = tokenProvider
                .getAccountEmailForReissueToken(accessToken, refreshToken)
                .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));

        var foundAccount = accountRepository
                .findByEmailAndDeletedIsFalse(accountEmail)
                .orElseThrow(() -> new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED));
        if (foundAccount.validateRefreshToken(refreshToken)) {
            String authorities = foundAccount.getRoles().stream()
                    .map(x -> x.getName())
                    .collect(Collectors.joining(","));
            var token = tokenProvider.createToken(foundAccount.getEmail(), authorities);
            accountRepository.save(foundAccount.withRefreshToken(token.getRefreshToken()));

            // 새로 토큰을 발급받았으므로 이전에 발급하여 사용중인 AccessToken은 사용중지 처리한다.
            redisTemplate.opsForValue().set(accessToken, "redeem", Duration.ofMillis(Constants.ACCESS_TOKEN_EXPIRES_TIME));
            return token;
        } else {
            throw new RestApiException(ErrorCodes.Unauthorized.INVALID_REFRESH_TOKEN);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return accountRepository
                .findByEmailAndDeletedIsFalse(username)
                .map(this::createAuthenticationDetails)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_ACCOUNT_ID));
    }

    private UserAuthenticationDetails createAuthenticationDetails(Account account) {
        var authorities = account
                .getRoles().stream()
                .map(x -> new SimpleGrantedAuthority(x.getName()))
                .collect(Collectors.toList());
        return new UserAuthenticationDetails(
                account.getEmail(),
                account.getPassword(),
                authorities,
                account.getId(),
                account.getEmail(),
                account.getBlogId());
    }

    @Bean
    public Function<UserDetails, UserAuthenticationDetails> fetchCurrentUserAuthenticationDetails() {
        return (principal -> {
            if(principal == null)
                throw new RestApiException(ErrorCodes.Unauthorized.UNAUTHORIZED);

            String email = principal.getUsername();
            return accountRepository
                    .findByEmailAndDeletedIsFalse(email)
                    .map(this::createAuthenticationDetails)
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_ACCOUNT_ID));
        });
    }
}