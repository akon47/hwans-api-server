package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.admin.AdminAccountDto;
import com.hwans.apiserver.dto.admin.AdminActiveViewerDto;
import com.hwans.apiserver.dto.admin.AdminActiveViewersDto;
import com.hwans.apiserver.dto.admin.AdminStatisticsDto;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.websocket.ActiveViewerSessionDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.service.blog.BlogService;
import com.hwans.apiserver.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
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
    private final BlogService blogService;
    private final WebSocketService webSocketService;

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

    @Override
    public AdminActiveViewersDto getActiveViewers() {
        var presences = webSocketService.getActiveViewerPresences();
        var totalSessions = webSocketService.getSessionCount();
        if (presences.isEmpty()) {
            return AdminActiveViewersDto.builder()
                    .totalSessions(totalSessions)
                    .viewers(List.of())
                    .build();
        }

        // 보고 있는 게시글 Id -> 공개 게시글 정보 (공개 글만 조회되므로 비공개 글은 매핑되지 않는다)
        var postIds = presences.stream()
                .map(ActiveViewerSessionDto::postId)
                .map(this::parseUuid)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, SimplePostDto> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : blogService.getPostsByIds(postIds).stream()
                .collect(Collectors.toMap(SimplePostDto::getId, Function.identity(), (a, b) -> a));

        // 로그인 세션의 이메일 -> 회원 (이름 표시용, N+1 방지를 위해 한 번에 조회)
        var emails = presences.stream()
                .map(ActiveViewerSessionDto::email)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Account> accountMap = emails.isEmpty()
                ? Collections.emptyMap()
                : accountRepository.findAllByEmailInAndDeletedIsFalse(emails).stream()
                .collect(Collectors.toMap(Account::getEmail, Function.identity(), (a, b) -> a));

        var viewers = presences.stream()
                .map(presence -> toAdminActiveViewerDto(presence, postMap, accountMap))
                .collect(Collectors.toList());

        return AdminActiveViewersDto.builder()
                .totalSessions(totalSessions)
                .viewers(viewers)
                .build();
    }

    private AdminActiveViewerDto toAdminActiveViewerDto(ActiveViewerSessionDto presence,
                                                        Map<UUID, SimplePostDto> postMap,
                                                        Map<String, Account> accountMap) {
        var postId = parseUuid(presence.postId());
        var post = postId != null ? postMap.get(postId) : null;
        var account = presence.email() != null ? accountMap.get(presence.email()) : null;
        return AdminActiveViewerDto.builder()
                .ip(presence.ip())
                .userAgent(presence.userAgent())
                .connectedAt(presence.connectedAt())
                .memberName(account != null ? account.getName() : null)
                .memberEmail(presence.email())
                .postId(postId)
                .postTitle(post != null ? post.getTitle() : null)
                .postUrl(post != null ? post.getPostUrl() : null)
                .blogId(post != null && post.getAuthor() != null ? post.getAuthor().getBlogId() : null)
                .build();
    }

    private UUID parseUuid(String value) {
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
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
