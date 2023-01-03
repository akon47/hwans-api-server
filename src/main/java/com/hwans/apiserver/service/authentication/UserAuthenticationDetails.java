package com.hwans.apiserver.service.authentication;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * 인증된 사용자의 상세 정보를 제공한다.
 */
@Getter
public class UserAuthenticationDetails extends User implements UserDetails {
    private final UUID id;
    private final String email;
    private final String blogId;

    public UserAuthenticationDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, UUID id, String email, String blogId) {
        super(username, password == null ? "" : password, authorities);

        this.id = id;
        this.email = email;
        this.blogId = blogId;
    }

    public UserAuthenticationDetails(String accountEmail, Collection<? extends GrantedAuthority> authorities) {
        super(accountEmail, "", authorities);

        this.id = null;
        this.email = accountEmail;
        this.blogId = null;
    }
}
