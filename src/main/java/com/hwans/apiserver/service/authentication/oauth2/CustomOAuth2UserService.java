package com.hwans.apiserver.service.authentication.oauth2;

import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AccountRepository accountRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var providerType = ProviderType
                .valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        var providerAttributes = ProviderAttributes
                .of(providerType, super.loadUser(userRequest).getAttributes());

        var foundAccount = accountRepository
                .findByEmailAndDeletedIsFalse(providerAttributes.getEmail())
                .orElse(null);
        if (foundAccount != null) {
            var authorities = foundAccount.getRoles().stream()
                    .map(Role::getName)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());
            var token = jwtTokenProvider.createToken(foundAccount.getEmail(), authorities);
            accountRepository.save(foundAccount.withRefreshToken(token.getRefreshToken()));
            return new CustomOAuth2User(providerAttributes, authorities, token.getAccessToken(), token.getRefreshToken());
        } else {
            var registerToken = jwtTokenProvider.createRegisterToken(providerAttributes.getEmail());
            return new CustomOAuth2User(providerAttributes, registerToken);
        }
    }
}
