package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_account")
@Getter
@Setter
@Accessors(chain = true)
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
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "tb_account_role",
            joinColumns = @JoinColumn(name = "account_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_name", referencedColumnName = "name"))
    private Set<Role> roles = new HashSet<>();
}
