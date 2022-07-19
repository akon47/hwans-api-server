package com.hwans.apiserver.entity.account;

import com.hwans.apiserver.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tb_account_role")
@IdClass(AccountRoleId.class)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRole extends BaseEntity implements Serializable {
    @Id
    @ManyToOne
    @JoinColumn(name="account_id")
    private Account account;
    @Id
    @ManyToOne
    @JoinColumn(name="role_name")
    private Role role;
}
