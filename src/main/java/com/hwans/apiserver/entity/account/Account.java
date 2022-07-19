package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.*;
import lombok.experimental.Accessors;

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

    public Set<Role> getRoles() {
        return accountRoles.stream().map(x -> x.getRole()).collect(Collectors.toSet());
    }
}