package com.hwans.apiserver.service.account;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final AttachmentRepository attachmentRepository;
    private final AccountMapper accountMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    @Override
    public AccountDto createAccount(CreateAccountDto createAccountDto) {
        var account = accountMapper.toEntity(createAccountDto);

        // 이미 해당 계정 이메일이 존재할 경우
        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_EMAIL);
        }

        // 이미 해당 블로그 Id가 존재할 경우
        if (accountRepository.existsByBlogId(account.getBlogId())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_BLOG_ID);
        }

        String verifyCode = redisTemplate.opsForValue().get(createAccountDto.getEmail());
        if (verifyCode == null || verifyCode.equals(createAccountDto.getEmailVerifyCode()) == false) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_EMAIL_VERIFY_CODE);
        }

        // 새 사용자 계정 정보 저장
        var savedAccount = accountRepository.save(account);
        var userRole = roleRepository.saveIfNotExist(RoleType.USER.getName());
        savedAccount.addRole(userRole);
        return accountMapper.toDto(savedAccount);
    }

    @Override
    public String setEmailVerifyCode(String email) {
        // 이미 해당 계정 이메일이 존재할 경우
        if (accountRepository.existsByEmail(email)) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_EMAIL);
        }

        if (redisTemplate.hasKey(email) == false) {
            var verifyCode = createNewVerifyCode();
            redisTemplate.opsForValue().set(email, verifyCode, Duration.ofMillis(Constants.EMAIL_VERIFY_CODE_EXPIRES_TIME));
            return verifyCode;
        } else {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS_VERIFY_CODE);
        }
    }

    @Override
    @Transactional
    public AccountDto setProfileImage(UUID accountId, UUID fileId) {
        var foundAccount = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        var attachment = attachmentRepository
                .findById(fileId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        foundAccount.setProfileImage(attachment);
        return accountMapper.toDto(foundAccount);
    }

    @Override
    public AccountDto getAccount(UUID accountId) {
        var foundAccount = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        return accountMapper.toDto(foundAccount);
    }

    @Override
    public AccountDto getCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var foundAccount = accountRepository
                .findByEmailAndDeletedIsFalse(authentication.getName())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        return accountMapper.toDto(foundAccount);
    }

    @Override
    public String getCurrentAccountEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    private String createNewVerifyCode() {
        Random random = new Random();
        return String.valueOf(random.nextInt(100000, 1000000));
    }
}
