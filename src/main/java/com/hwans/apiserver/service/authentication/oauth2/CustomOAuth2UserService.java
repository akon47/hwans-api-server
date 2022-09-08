package com.hwans.apiserver.service.authentication.oauth2;

import com.hwans.apiserver.repository.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AccountRepository accountRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        var providerType = ProviderType
                .valueOf(userRequest.getClientRegistration().getRegistrationId().toUpperCase());

        var providerAttributes = ProviderAttributes
                .of(providerType, super.loadUser(userRequest).getAttributes());

        var foundAccount = accountRepository
                .findByEmailAndDeletedIsFalse(providerAttributes.getEmail())
                .orElse(null);

        return new CustomOAuth2User(providerAttributes, foundAccount);
    }
}
