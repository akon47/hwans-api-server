package com.hwans.apiserver.service.account;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.account.AccountCreateDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.mail.MailMessageDto;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.account.AccountRoleRepository;
import com.hwans.apiserver.repository.role.RoleRepository;
import com.hwans.apiserver.service.mail.MailSenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Random;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final RoleRepository roleRepository;
    private final AccountMapper accountMapper;
    private final MailSenderService mailSenderService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String ALREADY_EXISTS_EMAIL = "이미 존재하는 사용자 계정 이메일 입니다.";
    private static final String FAILED_TO_CREATE_ID = "사용자 계정 생성에 실패했습니다.";
    private static final String NO_CURRENT_ACCOUNT_INFO = "현재 계정에 대한 정보를 찾을 수 없습니다.";
    private static final String ALREADY_EXISTS_VERIFY_CODE = "이전에 발송한 인증 코드가 아직 유효합니다.";

    @Transactional
    @Override
    public AccountDto createAccount(AccountCreateDto accountCreateDto) {
        var account = accountMapper.toEntity(accountCreateDto);

        // 이미 해당 계정 이메일이 존재할 경우
        if (accountRepository.existsByEmail(account.getEmail())) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, ALREADY_EXISTS_EMAIL);
        }

        String verifyCode = redisTemplate.opsForValue().get(accountCreateDto.getEmail());
        if (verifyCode == null || verifyCode.equals(accountCreateDto.getEmailVerifyCode()) == false) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_EMAIL_VERIFY_CODE);
        }

        // 새 사용자 계정 정보 저장
        var savedAccount = accountRepository.save(account);
        var userRole = roleRepository.saveIfNotExist("ROLE_USER");
        savedAccount.addRole(userRole);
        return accountMapper.toDto(savedAccount);
    }

    @Override
    public void sendEmailVerifyCode(String email) {
        // 이미 해당 계정 이메일이 존재할 경우
        if (accountRepository.existsByEmail(email)) {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, ALREADY_EXISTS_EMAIL);
        }

        if (redisTemplate.hasKey(email) == false) {
            var verifyCode = createNewVerifyCode();
            mailSenderService.sendMail(createVerifyEmailMessage(email, verifyCode));
            redisTemplate.opsForValue().set(email, verifyCode, Duration.ofMillis(Constants.EMAIL_VERIFY_CODE_EXPIRES_TIME));
        } else {
            throw new RestApiException(ErrorCodes.Conflict.ALREADY_EXISTS, ALREADY_EXISTS_VERIFY_CODE);
        }
    }

    @Override
    public AccountDto getCurrentAccount() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var foundAccount = accountRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND, NO_CURRENT_ACCOUNT_INFO));
        return accountMapper.toDto(foundAccount);
    }

    private MailMessageDto createVerifyEmailMessage(String email, String verifyCode) {
        return MailMessageDto.builder()
                .subject("[Hwan'story] 회원가입")
                .content(new StringBuilder()
                        .append("코드: ")
                        .append(verifyCode)
                        .toString())
                .to(email)
                .isHtmlContent(false)
                .build();
    }

    private String createNewVerifyCode() {
        Random random = new Random();
        return String.valueOf(random.nextInt(100000, 1000000));
    }
}
