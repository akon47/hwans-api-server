package com.hwans.apiserver.entity.account;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class AccountRoleId implements Serializable {
    private String account;
    private String role;
}
