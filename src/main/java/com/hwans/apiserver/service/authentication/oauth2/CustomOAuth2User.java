package com.hwans.apiserver.service.authentication.oauth2;

import com.hwans.apiserver.entity.account.role.RoleType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {
    private final ProviderAttributes providerAttributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean needRegister;
    private final String accessToken;
    private final String refreshToken;
    private final String registerToken;

    public CustomOAuth2User(ProviderAttributes providerAttributes, Collection<? extends GrantedAuthority> authorities, String accessToken, String refreshToken) {
        this.providerAttributes = providerAttributes;
        this.authorities = authorities;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.registerToken = null;
        this.needRegister = false;
    }

    public CustomOAuth2User(ProviderAttributes providerAttributes, String registerToken) {
        this.providerAttributes = providerAttributes;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(RoleType.SOCIAL.getName()));
        this.registerToken = registerToken;
        this.accessToken = null;
        this.refreshToken = null;
        this.needRegister = true;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return providerAttributes.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return providerAttributes.getEmail();
    }
}
