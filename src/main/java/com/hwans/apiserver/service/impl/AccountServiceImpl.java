package com.hwans.apiserver.service.impl;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.Role;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Transactional
    @Override
    public AccountDto createAccount(AccountCreateDto accountCreateDto) {
        Account account = accountMapper.toEntity(accountCreateDto);

        // 이미 해당 계정 아이디가 존재할 경우
        if(accountRepository.existsById(account.getId())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, "이미 존재하는 사용자 계정 아이디 입니다.");
        }

        account.getRoles().add(new Role("ROLE_USER"));

        // 새 사용자 계정 정보 저장
        Optional<Account> savedAccount = Optional.ofNullable(accountRepository.save(account));
        return accountMapper.toDto(savedAccount.orElseThrow(() -> new RestApiException(ErrorCodes.InternalServerError.INTERNAL_SERVER_ERROR, "사용자 계정 생성에 실패했습니다.")));
    }
}
