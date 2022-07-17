package com.hwans.apiserver.entity.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_role")
@Getter
@Setter
@Accessors(chain = true)
public class Role {
    @Id
    @Column(length = 32)
    private String name;
    @ManyToMany(mappedBy = "roles")
    private Set<Account> accounts = new HashSet<>();

    public Role(String name){
        this.name = name;
    }

    public Role() {

    }
}
