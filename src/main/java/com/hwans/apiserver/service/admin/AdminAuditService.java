package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.dto.admin.AdminAuditLogDto;
import com.hwans.apiserver.dto.common.PageDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.admin.AdminAuditLog;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.admin.AdminAuditLogRepository;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 활동 기록 서비스. 관리자가 무언가를 바꿀 때마다 한 줄씩 남긴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuditService {
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final AccountRepository accountRepository;

    /**
     * 지금 요청을 보낸 관리자의 행동을 기록한다. (호출한 쪽의 트랜잭션에 함께 묶인다)
     */
    @Transactional
    public void record(String action, String targetType, Object targetId, String summary) {
        var actorEmail = currentEmail();
        var actorName = actorEmail == null ? null
                : accountRepository.findByEmailAndDeletedIsFalse(actorEmail).map(Account::getName).orElse(null);
        adminAuditLogRepository.save(AdminAuditLog.builder()
                .actorEmail(actorEmail)
                .actorName(actorName)
                .action(action)
                .targetType(targetType)
                .targetId(targetId == null ? null : targetId.toString())
                .summary(summary == null ? null : summary.length() <= 500 ? summary : summary.substring(0, 500))
                .build());
    }

    /**
     * 지금 요청을 보낸 사람의 이메일 (로그인하지 않았으면 null)
     */
    public String currentEmail() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserAuthenticationDetails details) {
            return details.getEmail();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public PageDto<AdminAuditLogDto> getLogs(int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageDto.of(adminAuditLogRepository.findAll(pageable), log -> AdminAuditLogDto.builder()
                .id(log.getId())
                .actorEmail(log.getActorEmail())
                .actorName(log.getActorName())
                .action(log.getAction())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .summary(log.getSummary())
                .createdAt(log.getCreatedAt())
                .build());
    }
}
