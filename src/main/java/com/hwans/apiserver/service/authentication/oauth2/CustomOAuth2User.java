package com.hwans.apiserver.service.authentication.oauth2;

import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.entity.account.role.RoleType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {
    private final ProviderAttributes providerAttributes;
    private final Collection<GrantedAuthority> authorities;
    private final boolean needRegister;

    public CustomOAuth2User(ProviderAttributes providerAttributes, Account foundAccount) {
        this.providerAttributes = providerAttributes;
        if (foundAccount == null) {
            this.authorities = Collections.singletonList(new SimpleGrantedAuthority(RoleType.SOCIAL.getName()));
            this.needRegister = true;
        } else {
            this.authorities = foundAccount.getRoles().stream().map(Role::getName).map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
            this.needRegister = false;
        }
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
