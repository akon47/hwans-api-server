package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.admin.*;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.PageDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.websocket.ActiveViewerSessionDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.account.role.Role;
import com.hwans.apiserver.entity.account.role.RoleType;
import com.hwans.apiserver.entity.blog.Comment;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.entity.token.AccessToken;
import com.hwans.apiserver.entity.token.DeviceAuthorization;
import com.hwans.apiserver.mapper.AccountMapper;
import com.hwans.apiserver.mapper.PostMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.account.AccountRoleRepository;
import com.hwans.apiserver.repository.attachment.AttachmentRepository;
import com.hwans.apiserver.repository.blog.CommentRepository;
import com.hwans.apiserver.repository.blog.PostRepository;
import com.hwans.apiserver.repository.role.RoleRepository;
import com.hwans.apiserver.repository.token.AccessTokenRepository;
import com.hwans.apiserver.repository.token.DeviceAuthorizationRepository;
import com.hwans.apiserver.service.blog.BlogService;
import com.hwans.apiserver.service.websocket.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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
    // 대시보드 일별 추이 기간
    private static final int DAILY_DAYS = 30;
    // 관리자 토큰 목록 최대 개수
    private static final int TOKEN_LIST_LIMIT = 300;

    private final AccountRepository accountRepository;
    private final AccountRoleRepository accountRoleRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final AccessTokenRepository accessTokenRepository;
    private final AttachmentRepository attachmentRepository;
    private final DeviceAuthorizationRepository deviceAuthorizationRepository;
    private final BlogService blogService;
    private final WebSocketService webSocketService;
    private final PostMapper postMapper;
    private final AccountMapper accountMapper;
    private final Optional<BuildProperties> buildProperties;
    private final Environment environment;
    private final RedisConnectionFactory redisConnectionFactory;

    @Value("${attachments.path}")
    private String attachmentsBasePath;

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
    @Transactional
    public AdminAccountDto setAdminRole(UUID actorAccountId, UUID accountId, boolean admin) {
        var account = accountRepository
                .findById(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        // 자기 자신의 관리자 권한은 뺄 수 없다 (관리자가 아무도 없어지는 것을 막는다)
        if (!admin && account.getId().equals(actorAccountId)) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        if (account.isGuest()) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        if (admin) {
            Role adminRole = roleRepository.saveIfNotExist(RoleType.ADMIN.getName());
            account.addRole(adminRole);
        } else {
            account.removeRole(RoleType.ADMIN);
            accountRoleRepository
                    .findByAccount_IdAndRole_Name(account.getId(), RoleType.ADMIN.getName())
                    .ifPresent(accountRoleRepository::delete);
        }
        return toAdminAccountDto(account);
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
    public AdminDashboardDto getDashboard() {
        var today = LocalDate.now();
        var since = today.minusDays(DAILY_DAYS - 1).atStartOfDay();

        var memberDates = accountRepository.findMemberCreatedAtSince(since);
        var postDates = postRepository.findCreatedAtSince(since);
        var commentDates = commentRepository.findCreatedAtSince(since);

        var daily = new ArrayList<AdminDashboardDto.DailyCount>(DAILY_DAYS);
        for (int i = DAILY_DAYS - 1; i >= 0; i--) {
            var date = today.minusDays(i);
            daily.add(AdminDashboardDto.DailyCount.builder()
                    .date(date)
                    .members(countOn(memberDates, date))
                    .posts(countOn(postDates, date))
                    .comments(countOn(commentDates, date))
                    .build());
        }

        var viewerCounts = webSocketService.getActiveViewerCounts();
        var viewingCount = viewerCounts.values().stream().mapToInt(Integer::intValue).sum();

        var recentPosts = postRepository.findAllForAdmin(null, null, PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(postMapper::EntityToSimplePostDto).toList();
        var recentComments = commentRepository.findAllForAdmin(null, PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream().map(this::toAdminCommentDto).toList();
        var recentMembers = accountRepository.findAllForAdmin(null, PageRequest.of(0, 6))
                .stream().map(this::toAdminAccountDto).toList();

        return AdminDashboardDto.builder()
                .memberCount(accountRepository.countActiveMembers())
                .postCount(postRepository.countByDeletedIsFalse())
                .publicPostCount(postRepository.countByDeletedIsFalseAndOpenType(OpenType.PUBLIC))
                .commentCount(commentRepository.countByDeletedIsFalse())
                .totalHits(postRepository.sumHitsForAdmin())
                .sessionCount(webSocketService.getSessionCount())
                .viewingCount(viewingCount)
                .todayMembers(countOn(memberDates, today))
                .todayPosts(countOn(postDates, today))
                .todayComments(countOn(commentDates, today))
                .daily(daily)
                .recentPosts(recentPosts)
                .recentComments(recentComments)
                .recentMembers(recentMembers)
                .build();
    }

    private long countOn(List<LocalDateTime> dates, LocalDate date) {
        return dates.stream().filter(x -> x != null && x.toLocalDate().equals(date)).count();
    }

    @Override
    public PageDto<SimplePostDto> getPosts(Optional<String> search, Optional<OpenType> openType, int page, int size) {
        var searchKeyword = search.filter(x -> !x.isBlank()).orElse(null);
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var found = postRepository.findAllForAdmin(searchKeyword, openType.orElse(null), pageable);
        return PageDto.of(found, postMapper::EntityToSimplePostDto);
    }

    @Override
    @Transactional
    public SimplePostDto changePostOpenType(UUID postId, OpenType openType) {
        var post = postRepository
                .findById(postId)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        post.setOpenType(openType);
        return postMapper.EntityToSimplePostDto(post);
    }

    @Override
    @Transactional
    public void deletePost(UUID postId) {
        var post = postRepository
                .findById(postId)
                .filter(x -> !x.isDeleted())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_POST));
        post.setDeleted();
    }

    @Override
    public PageDto<AdminCommentDto> getComments(Optional<String> search, int page, int size) {
        var searchKeyword = search.filter(x -> !x.isBlank()).orElse(null);
        var pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var found = commentRepository.findAllForAdmin(searchKeyword, pageable);
        return PageDto.of(found, this::toAdminCommentDto);
    }

    @Override
    @Transactional
    public void deleteComment(UUID commentId) {
        blogService.deleteComment(commentId);
    }

    @Override
    public List<AdminTokenDto> getTokens() {
        return accessTokenRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, TOKEN_LIST_LIMIT))
                .stream().map(this::toAdminTokenDto).toList();
    }

    @Override
    @Transactional
    public void revokeToken(UUID tokenId) {
        var token = accessTokenRepository
                .findById(tokenId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        token.revoke();
    }

    @Override
    public AdminSystemDto getSystem() {
        var runtime = ManagementFactory.getRuntimeMXBean();
        var memory = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        var attachmentDirectory = new File(attachmentsBasePath);
        var profile = String.join(",", environment.getActiveProfiles());

        boolean databaseOk;
        try {
            accountRepository.count();
            databaseOk = true;
        } catch (Exception e) {
            databaseOk = false;
        }
        boolean redisOk;
        try (var connection = redisConnectionFactory.getConnection()) {
            redisOk = "PONG".equalsIgnoreCase(connection.ping());
        } catch (Exception e) {
            redisOk = false;
        }

        return AdminSystemDto.builder()
                .appVersion(buildProperties.map(BuildProperties::getVersion).orElse(null))
                .buildTime(buildProperties.map(BuildProperties::getTime).map(Instant::toString).orElse(null))
                .profile(profile.isEmpty() ? "default" : profile)
                .javaVersion(System.getProperty("java.version"))
                .startedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(runtime.getStartTime()), ZoneId.systemDefault()))
                .uptimeMillis(runtime.getUptime())
                .processors(Runtime.getRuntime().availableProcessors())
                .heapUsedBytes(memory.getUsed())
                .heapMaxBytes(memory.getMax())
                .databaseOk(databaseOk)
                .redisOk(redisOk)
                .websocketSessions(webSocketService.getSessionCount())
                .attachmentCount(attachmentRepository.count())
                .attachmentBytes(attachmentRepository.sumFileSize())
                .attachmentPath(attachmentsBasePath)
                .attachmentFreeBytes(attachmentDirectory.exists() ? attachmentDirectory.getUsableSpace() : 0)
                .attachmentTotalBytes(attachmentDirectory.exists() ? attachmentDirectory.getTotalSpace() : 0)
                .scheduledPostCount(postRepository.countByDeletedIsFalseAndOpenType(OpenType.SCHEDULED))
                .pendingDeviceCount(deviceAuthorizationRepository.countByStatus(DeviceAuthorization.Status.PENDING))
                .activeTokenCount(accessTokenRepository.countByRevokedAtIsNull())
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
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, SimplePostDto> postMap = postIds.isEmpty()
                ? Collections.emptyMap()
                : blogService.getPostsByIds(postIds).stream()
                .collect(Collectors.toMap(SimplePostDto::getId, Function.identity(), (a, b) -> a));

        // 로그인 세션의 이메일 -> 회원 (이름 표시용, N+1 방지를 위해 한 번에 조회)
        var emails = presences.stream()
                .map(ActiveViewerSessionDto::email)
                .filter(Objects::nonNull)
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

    private AdminCommentDto toAdminCommentDto(Comment comment) {
        var post = comment.getPost();
        var postAuthor = post != null ? post.getAuthor() : null;
        return AdminCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(accountMapper.toSimpleDto(comment.getAuthor()))
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount())
                .postBlogId(postAuthor != null ? postAuthor.getBlogId() : null)
                .postUrl(post != null ? post.getPostUrl() : null)
                .postTitle(post != null ? post.getTitle() : null)
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private AdminTokenDto toAdminTokenDto(AccessToken token) {
        var now = LocalDateTime.now();
        var expired = token.getExpiresAt() != null && token.getExpiresAt().isBefore(now);
        var owner = token.getAccount();
        return AdminTokenDto.builder()
                .id(token.getId())
                .tokenId(token.getTokenId())
                .name(token.getName())
                .scopes(token.getScopeKeys().stream().sorted().toList())
                .origin(token.getOrigin())
                .owner(owner != null ? accountMapper.toSimpleDto(owner) : null)
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .lastUsedAt(token.getLastUsedAt())
                .lastUsedIp(token.getLastUsedIp())
                .revokedAt(token.getRevokedAt())
                .expired(expired)
                .usable(token.getRevokedAt() == null && !expired)
                .build();
    }
}
