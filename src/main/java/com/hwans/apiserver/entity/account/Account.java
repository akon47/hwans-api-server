package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.authentication.RefreshToken;
import com.hwans.apiserver.entity.account.role.AccountRole;
import com.hwans.apiserver.entity.account.role.Role;
import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "tb_account")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Account extends BaseEntity {
    @Id
    @Column(length = 32)
    private String id;
    @Column(length = 64)
    private String password;
    @Column(length = 32)
    private String name;
    @Column(nullable = false)
    private boolean deleted;
    @OneToMany(mappedBy = "account")
    private final Set<AccountRole> accountRoles = new HashSet<>();
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "account_id")
    @With
    private RefreshToken refreshToken;

    public Set<Role> getRoles() {
        return accountRoles.stream().map(x -> x.getRole()).collect(Collectors.toSet());
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = new RefreshToken(id, refreshToken, this);
    }

    public void clearRefreshToken() {
        setRefreshToken(null);
    }

    public boolean validateRefreshToken(String refreshToken) {
        if(this.refreshToken == null)
            return true;

        return this.refreshToken.getRefreshToken().equals(refreshToken);
    }
}