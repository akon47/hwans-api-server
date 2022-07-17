package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.entity.account.Account;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Mapper(componentModel = "spring")
public abstract class AccountMapper {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Mapping(source = "password", target = "password", ignore = true)
    public abstract Account toEntity(AccountCreateDto userCreateDtoDto);

    @AfterMapping
    public void afterMapping(@MappingTarget Account target, AccountCreateDto source) {
        target.setPassword(passwordEncoder.encode(source.getPassword()));
    }

    public abstract AccountDto toDto(Account account);
}
