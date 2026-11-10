package com.hwans.apiserver.service.moderation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.admin.AdminModerationDto;
import com.hwans.apiserver.entity.admin.BlockedIp;
import com.hwans.apiserver.entity.admin.SiteSetting;
import com.hwans.apiserver.repository.admin.BlockedIpRepository;
import com.hwans.apiserver.repository.admin.SiteSettingRepository;
import com.hwans.apiserver.service.admin.AdminAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 차단 서비스: IP 차단(손님 댓글·신고를 막는다)과 금칙어(댓글·손님 이름에 들어 있으면 등록을 막는다).
 * 댓글을 쓸 때마다 확인하므로 목록은 메모리에 잠깐 담아 두고, 관리자가 바꾸면 바로 비운다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModerationService {
    private static final String BANNED_WORDS_KEY = "banned-words";
    private static final long CACHE_MILLIS = 30_000L;

    private final BlockedIpRepository blockedIpRepository;
    private final SiteSettingRepository siteSettingRepository;
    private final AdminAuditService adminAuditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile Set<String> cachedBlockedIps = Set.of();
    private volatile List<String> cachedBannedWords = List.of();
    private volatile long cachedAt = 0L;

    // ───────────── 확인 ─────────────

    /**
     * 차단된 IP 면 거절한다.
     */
    public void assertNotBlocked(String ip) {
        refreshCacheIfStale();
        if (ip != null && cachedBlockedIps.contains(ip)) {
            throw new RestApiException(ErrorCodes.Forbidden.BLOCKED_IP);
        }
    }

    /**
     * 금칙어가 들어 있으면 거절한다. (대소문자·공백을 무시하고 본다)
     */
    public void assertCleanText(String... texts) {
        refreshCacheIfStale();
        if (cachedBannedWords.isEmpty()) {
            return;
        }
        for (var text : texts) {
            if (text == null) {
                continue;
            }
            var normalized = normalize(text);
            if (cachedBannedWords.stream().anyMatch(normalized::contains)) {
                throw new RestApiException(ErrorCodes.BadRequest.BANNED_WORD);
            }
        }
    }

    private String normalize(String value) {
        return value.toLowerCase().replaceAll("\\s+", "");
    }

    private void refreshCacheIfStale() {
        if (System.currentTimeMillis() - cachedAt < CACHE_MILLIS) {
            return;
        }
        try {
            cachedBlockedIps = blockedIpRepository.findAll().stream().map(BlockedIp::getIp).collect(Collectors.toSet());
            cachedBannedWords = loadBannedWords().stream().map(this::normalize).filter(word -> !word.isEmpty()).toList();
            cachedAt = System.currentTimeMillis();
        } catch (Exception e) {
            // 목록을 못 읽으면 지난 목록으로 계속 확인한다. (차단 확인 때문에 댓글 작성 자체가 실패하지 않게)
            log.warn("moderation lists could not be loaded: {}", e.getMessage());
        }
    }

    private void invalidateCache() {
        cachedAt = 0L;
    }

    // ───────────── 관리 ─────────────

    @Transactional(readOnly = true)
    public List<AdminModerationDto.BlockedIp> getBlockedIps() {
        return blockedIpRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public AdminModerationDto.BlockedIp blockIp(String ip, String reason) {
        var normalizedIp = ip.strip();
        var existing = blockedIpRepository.findByIp(normalizedIp);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }
        var saved = blockedIpRepository.save(BlockedIp.builder()
                .ip(normalizedIp)
                .reason(reason == null || reason.isBlank() ? null : reason.strip())
                .createdBy(adminAuditService.currentEmail())
                .build());
        adminAuditService.record("IP_BLOCK", "IP", normalizedIp, "IP 차단: " + normalizedIp + (saved.getReason() != null ? " (" + saved.getReason() + ")" : ""));
        invalidateCache();
        return toDto(saved);
    }

    @Transactional
    public void unblockIp(UUID id) {
        var blocked = blockedIpRepository.findById(id)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
        blockedIpRepository.delete(blocked);
        adminAuditService.record("IP_UNBLOCK", "IP", blocked.getIp(), "IP 차단 해제: " + blocked.getIp());
        invalidateCache();
    }

    @Transactional(readOnly = true)
    public AdminModerationDto.BannedWords getBannedWords() {
        return AdminModerationDto.BannedWords.builder().words(loadBannedWords()).build();
    }

    @Transactional
    public AdminModerationDto.BannedWords setBannedWords(List<String> words) {
        var cleaned = words.stream().map(String::strip).filter(word -> !word.isEmpty()).distinct().toList();
        String value;
        try {
            value = objectMapper.writeValueAsString(cleaned);
        } catch (Exception e) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        if (value.length() > 4000) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        var setting = siteSettingRepository.findById(BANNED_WORDS_KEY)
                .orElseGet(() -> SiteSetting.builder().key(BANNED_WORDS_KEY).build());
        setting.setValue(value);
        siteSettingRepository.save(setting);
        adminAuditService.record("BANNED_WORDS_UPDATE", "SITE", BANNED_WORDS_KEY, "금칙어 " + cleaned.size() + "개로 변경");
        invalidateCache();
        return AdminModerationDto.BannedWords.builder().words(cleaned).build();
    }

    private List<String> loadBannedWords() {
        return siteSettingRepository.findById(BANNED_WORDS_KEY)
                .map(SiteSetting::getValue)
                .map(value -> {
                    try {
                        return objectMapper.readValue(value, new TypeReference<List<String>>() {
                        });
                    } catch (Exception e) {
                        return List.<String>of();
                    }
                })
                .orElse(List.of());
    }

    private AdminModerationDto.BlockedIp toDto(BlockedIp blocked) {
        return AdminModerationDto.BlockedIp.builder()
                .id(blocked.getId())
                .ip(blocked.getIp())
                .reason(blocked.getReason())
                .createdBy(blocked.getCreatedBy())
                .createdAt(blocked.getCreatedAt())
                .build();
    }
}
