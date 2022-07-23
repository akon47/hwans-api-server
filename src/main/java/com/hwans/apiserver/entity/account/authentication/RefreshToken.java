package com.hwans.apiserver.entity.account.authentication;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
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
public class RefreshToken extends BaseEntity implements Serializable {
    @Id
    @Column(length = 32)
    private String accountId;
    @Column(length = 255)
    private String refreshToken;
    @OneToOne(mappedBy = "refreshToken")
    private Account account;
}
