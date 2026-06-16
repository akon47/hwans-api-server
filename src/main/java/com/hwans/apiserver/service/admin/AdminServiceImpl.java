package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.admin.AdminAccountDto;
import com.hwans.apiserver.dto.admin.AdminStatisticsDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 관리자 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {
    private final AccountRepository accountRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Override
    public SliceDto<AdminAccountDto> getAccounts(Optional<String> search, Optional<UUID> cursorId, int size) {
        var searchKeyword = search.filter(x -> !x.isBlank()).orElse(null);
        List<Account> found;
        if (cursorId.isPresent()) {
            var cursor = accountRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            found = accountRepository.findAllForAdminByCursor(searchKeyword, cursor.getId(), cursor.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            found = accountRepository.findAllForAdmin(searchKeyword, PageRequest.of(0, size + 1));
        }
        return SliceDto.of(found, size, cursorId.isEmpty(), this::toAdminAccountDto, Account::getId);
    }

    @Override
    @Transactional
    public void disableAccount(UUID accountId) {
        var account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        // 관리자 계정은 정지할 수 없다.
        if (account.hasRole(RoleType.ADMIN)) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        account.disable();
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void restoreAccount(UUID accountId) {
        var account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        account.restore();
        accountRepository.save(account);
    }

    @Override
    public AdminStatisticsDto getStatistics() {
        return AdminStatisticsDto.builder()
                .memberCount(accountRepository.countActiveMembers())
                .postCount(postRepository.countByDeletedIsFalse())
                .commentCount(commentRepository.countByDeletedIsFalse())
                .build();
    }

    private AdminAccountDto toAdminAccountDto(Account account) {
        return AdminAccountDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .name(account.getName())
                .blogId(account.getBlogId())
                .profileImageUrl(account.getProfileImageUrl())
                .deleted(account.isDeleted())
                .roles(account.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(account.getCreatedAt())
                .build();
    }
}
