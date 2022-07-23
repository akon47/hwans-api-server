package com.hwans.apiserver.entity.account.authentication;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.AccountRoleId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "tb_account_refresh_token")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRefreshToken extends BaseEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column
    private Long id;
    @MapsId
    @OneToOne
    @JoinColumn(name = "account_id")
    private Account account;
    @Column
    private String refreshToken;
}
