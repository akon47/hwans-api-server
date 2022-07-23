package com.hwans.apiserver.entity.account.role;

import lombok.*;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_role")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Role {
    @Id
    @Column(length = 32)
    private String name;
    @OneToMany(mappedBy = "role")
    private final Set<AccountRole> accountRoles = new HashSet<>();
}
