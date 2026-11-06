package com.hwans.apiserver.service.token;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.token.AccessTokenDetails;
import com.hwans.apiserver.dto.token.AccessTokenDto;
import com.hwans.apiserver.dto.token.IssuedAccessTokenDto;
import com.hwans.apiserver.entity.account.Account;
import com.hwans.apiserver.entity.token.AccessToken;
import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import com.hwans.apiserver.entity.token.AccessTokenScope;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.token.AccessTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 개인 액세스 토큰 서비스 구현체.
 * 토큰은 {@code hsp_<16자 hex id>_<43자 base64url 비밀>} 형식이며 전체 토큰의 SHA-256 만 저장한다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AccessTokenServiceImpl implements AccessTokenService {
    private static final Pattern FORMAT = Pattern.compile("^hsp_([0-9a-f]{16})_([A-Za-z0-9_-]{43})$");
    private static final Duration TOUCH_EVERY = Duration.ofMinutes(1);
    private static final int MAX_TTL_DAYS = 3650;
    private static final int MAX_NAME_LENGTH = 100;

    private final AccessTokenRepository accessTokenRepository;
    private final AccountRepository accountRepository;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public IssuedAccessTokenDto create(UUID accountId, String name, Set<AccessTokenScope> scopes, Integer ttlDays, AccessTokenOrigin origin) {
        var account = accountRepository
                .findByIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
        if (account.isGuest()) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }

        var tokenName = name == null ? "" : name.trim();
        if (tokenName.isEmpty() || tokenName.length() > MAX_NAME_LENGTH) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "토큰 이름은 1~" + MAX_NAME_LENGTH + "자여야 합니다.");
        }
        if (scopes == null || scopes.isEmpty()) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "권한(scope)을 하나 이상 골라야 합니다: posts, social");
        }
        var days = ttlDays == null ? 0 : ttlDays;
        if (days < 0 || days > MAX_TTL_DAYS) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "만료 기간은 0(무기한)~" + MAX_TTL_DAYS + "일이어야 합니다.");
        }

        var idBytes = new byte[8];
        var secretBytes = new byte[32];
        random.nextBytes(idBytes);
        random.nextBytes(secretBytes);
        var tokenId = HexFormat.of().formatHex(idBytes);
        var token = PREFIX + tokenId + "_" + Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);
        var expiresAt = days == 0 ? null : LocalDateTime.now().plusDays(days);

        var saved = accessTokenRepository.save(AccessToken.create(account, tokenId, hash(token), tokenName, scopes, origin, expiresAt));
        log.info("access token {} issued to {} (name=\"{}\", scopes={}, origin={}, expires={})",
                tokenId, account.getEmail(), tokenName, scopes, origin, expiresAt);

        return IssuedAccessTokenDto.builder()
                .token(token)
                .accessToken(toDto(saved))
                .build();
    }

    @Override
    @Transactional
    public Optional<AccessTokenDetails> authenticate(String presentedToken, String clientIp) {
        if (presentedToken == null) {
            return Optional.empty();
        }
        var presented = presentedToken.trim();
        var matcher = FORMAT.matcher(presented);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        var found = accessTokenRepository.findByTokenId(matcher.group(1));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var token = found.get();
        var presentedHash = hash(presented).getBytes(StandardCharsets.US_ASCII);
        var storedHash = token.getTokenHash().getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(presentedHash, storedHash)) {
            return Optional.empty();
        }
        if (!token.isUsable()) {
            return Optional.empty();
        }
        var account = token.getAccount();
        if (account == null || account.isDeleted()) {
            return Optional.empty();
        }

        var now = LocalDateTime.now();
        if (token.getLastUsedAt() == null
                || token.getLastUsedAt().isBefore(now.minus(TOUCH_EVERY))
                || !Objects.equals(clientIp, token.getLastUsedIp())) {
            token.used(clientIp);
        }

        return Optional.of(AccessTokenDetails.builder()
                .tokenId(token.getTokenId())
                .accountId(account.getId())
                .accountEmail(account.getEmail())
                .scopes(token.getScopeKeys())
                .roles(account.getRoles().stream().map(x -> x.getName()).collect(Collectors.toCollection(LinkedHashSet::new)))
                .build());
    }

    @Override
    public List<AccessTokenDto> getTokens(UUID accountId) {
        return accessTokenRepository.findAllByAccount_IdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public AccessTokenDto getToken(String tokenId) {
        return accessTokenRepository.findByTokenId(tokenId)
                .map(this::toDto)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_ACCESS_TOKEN));
    }

    @Override
    @Transactional
    public void revoke(UUID accountId, UUID id) {
        var token = accessTokenRepository
                .findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_ACCESS_TOKEN));
        token.revoke();
        log.info("access token {} revoked by its owner", token.getTokenId());
    }

    @Override
    @Transactional
    public void revokeByTokenId(String tokenId) {
        var token = accessTokenRepository
                .findByTokenId(tokenId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_ACCESS_TOKEN));
        token.revoke();
        log.info("access token {} revoked by itself", tokenId);
    }

    private AccessTokenDto toDto(AccessToken token) {
        return AccessTokenDto.builder()
                .id(token.getId())
                .tokenId(token.getTokenId())
                .name(token.getName())
                .scopes(new ArrayList<>(token.getScopeKeys()))
                .origin(token.getOrigin())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .lastUsedAt(token.getLastUsedAt())
                .lastUsedIp(token.getLastUsedIp())
                .revokedAt(token.getRevokedAt())
                .expired(token.isExpired())
                .usable(token.isUsable())
                .build();
    }

    /**
     * 문자열의 SHA-256 을 소문자 hex 로 반환한다.
     */
    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 토큰 주인 계정을 조회한다 (내부용).
     */
    static Account requireAccount(AccountRepository accountRepository, UUID accountId) {
        return accountRepository
                .findByIdAndDeletedIsFalse(accountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NO_CURRENT_ACCOUNT_INFO));
    }
}
