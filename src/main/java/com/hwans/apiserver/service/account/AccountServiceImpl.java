package com.hwans.apiserver.service.account;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.entity.account.role.AccountRole;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.account.AccountRoleRepository;
import com.hwans.apiserver.repository.role.RoleRepository;
import com.hwans.apiserver.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final RoleRepository roleRepository;
    private final AccountMapper accountMapper;

    private static final String ALREADY_EXISTS_ID = "이미 존재하는 사용자 계정 아이디 입니다.";
    private static final String FAILED_TO_CREATE_ID = "사용자 계정 생성에 실패했습니다.";
    private static final String NO_CURRENT_ACCOUNT_INFO = "현재 계정에 대한 정보를 찾을 수 없습니다.";

    @Transactional
    @Override
    public AccountDto createAccount(AccountCreateDto accountCreateDto) {
        var account = accountMapper.toEntity(accountCreateDto);

        // 이미 해당 계정 이메일이 존재할 경우
        if(accountRepository.existsByEmail(account.getEmail())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, ALREADY_EXISTS_ID);
        }

        // 새 사용자 계정 정보 저장
        var savedAccount = accountRepository.save(account);
        var userRole = roleRepository.saveIfNotExist("ROLE_USER");
        savedAccount.addRole(userRole);
        return accountMapper.toDto(savedAccount);
    }

    @Override
    public AccountDto getCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var foundAccount = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_CURRENT_ACCOUNT_INFO));
        return accountMapper.toDto(foundAccount);
    }
}
