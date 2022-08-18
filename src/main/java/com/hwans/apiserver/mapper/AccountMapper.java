package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.entity.account.Account;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public abstract class AccountMapper {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(target = "password", qualifiedByName = "encodePassword")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "refreshToken", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "biography", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "homepage", ignore = true)
    public abstract Account toEntity(CreateAccountDto userCreateDto);

    public abstract AccountDto toDto(Account account);

    @Named("encodePassword")
    String encoderPassword(String password) {
        return passwordEncoder.encode(password);
    }
}
