package com.hwans.apiserver.service.token;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.token.DeviceAuthorizationDto;
import com.hwans.apiserver.dto.token.DeviceCodeDto;
import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import com.hwans.apiserver.entity.token.AccessTokenScope;
import com.hwans.apiserver.entity.token.DeviceAuthorization;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.token.DeviceAuthorizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

/**
 * 기기 로그인 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class DeviceFlowServiceImpl implements DeviceFlowService {
    /**
     * 모음 없음(우연히 단어가 되지 않게), 헷갈리는 글자 없음(0/O, 1/I). 20^8 가지.
     */
    private static final String USER_CODE_ALPHABET = "BCDFGHJKLMNPQRSTVWXZ";
    private static final String DEVICE_CODE_PREFIX = "hsd_";
    private static final String DEFAULT_CLIENT_NAME = "AI agent";
    private static final int MAX_CLIENT_NAME_LENGTH = 100;
    private static final int MAX_TTL_DAYS = 3650;

    private final DeviceAuthorizationRepository deviceAuthorizationRepository;
    private final AccountRepository accountRepository;
    private final AccessTokenService accessTokenService;
    private final SecureRandom random = new SecureRandom();

    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;
    @Value("${blog.access-token.default-days:30}")
    private int defaultTtlDays;
    @Value("${blog.access-token.device-code-minutes:10}")
    private int deviceCodeMinutes;

    @Override
    @Transactional
    public DeviceCodeDto start(String clientName, String scope, String requestedIp, String userAgent) {
        var name = clientName == null || clientName.isBlank() ? DEFAULT_CLIENT_NAME : clientName.trim().replace("�", "?");
        if (name.length() > MAX_CLIENT_NAME_LENGTH) {
            name = name.substring(0, MAX_CLIENT_NAME_LENGTH);
        }
        var scopes = AccessTokenScope.parse(scope);

        var secret = new byte[32];
        random.nextBytes(secret);
        var deviceCode = DEVICE_CODE_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String userCode;
        do {
            userCode = newUserCode();
        } while (deviceAuthorizationRepository.existsByUserCodeAndStatus(userCode, DeviceAuthorization.Status.PENDING));

        var minutes = Math.max(1, deviceCodeMinutes);
        deviceAuthorizationRepository.save(DeviceAuthorization.create(
                AccessTokenServiceImpl.hash(deviceCode), userCode, name, AccessTokenScope.join(scopes),
                requestedIp, userAgent, LocalDateTime.now().plusMinutes(minutes)));
        log.info("device authorization started client=\"{}\" scopes={} ip={} code={}", name, scopes, requestedIp, userCode);

        var verificationUri = webBaseUrl + "/device";
        return DeviceCodeDto.builder()
                .deviceCode(deviceCode)
                .userCode(userCode)
                .verificationUri(verificationUri)
                .verificationUriComplete(verificationUri + "?code=" + UriUtils.encodeQueryParam(userCode, StandardCharsets.UTF_8))
                .expiresIn(minutes * 60)
                .interval(INTERVAL_SECONDS)
                .build();
    }

    @Override
    @Transactional
    public PollResult poll(String deviceCode, String clientIp) {
        if (deviceCode == null || deviceCode.isBlank()) {
            return PollResult.error("invalid_request", "device_code is required");
        }
        var found = deviceAuthorizationRepository.findByDeviceCodeHash(AccessTokenServiceImpl.hash(deviceCode.trim()));
        if (found.isEmpty()) {
            return PollResult.error("invalid_grant", "unknown device_code");
        }
        var authorization = found.get();
        if (authorization.getStatus() == DeviceAuthorization.Status.ISSUED) {
            return PollResult.error("invalid_grant", "this device_code was already used");
        }
        if (authorization.getStatus() == DeviceAuthorization.Status.DENIED) {
            return PollResult.error("access_denied", "the request was denied by the user");
        }
        if (authorization.isExpired()) {
            return PollResult.error("expired_token", "the code expired - start again");
        }
        if (authorization.getStatus() == DeviceAuthorization.Status.PENDING) {
            var lastPolledAt = authorization.getLastPolledAt();
            authorization.polled();
            if (lastPolledAt != null && lastPolledAt.isAfter(LocalDateTime.now().minusSeconds(INTERVAL_SECONDS - 1))) {
                return PollResult.error("slow_down", "poll at most every " + INTERVAL_SECONDS + " seconds");
            }
            return PollResult.error("authorization_pending", "waiting for approval at " + webBaseUrl + "/device");
        }

        // APPROVED: 지금 토큰을 발급하고(원문은 저장하지 않음) 코드를 소모한다.
        var account = authorization.getAccount();
        if (account == null || account.isDeleted()) {
            return PollResult.error("access_denied", "the approving account is not active");
        }
        var ttlDays = authorization.getTokenTtlDays() == null ? defaultTtlDays : authorization.getTokenTtlDays();
        var issued = accessTokenService.create(account.getId(), authorization.getClientName(),
                AccessTokenScope.parse(authorization.getScopes()), ttlDays, AccessTokenOrigin.DEVICE);
        authorization.issued();
        log.info("device authorization {} issued token {} to client=\"{}\" for {} ip={}",
                authorization.getUserCode(), issued.getAccessToken().getTokenId(), authorization.getClientName(), account.getEmail(), clientIp);
        return PollResult.issued(issued);
    }

    @Override
    public DeviceAuthorizationDto getPending(String userCode) {
        return toDto(findPending(userCode));
    }

    @Override
    @Transactional
    public DeviceAuthorizationDto approve(String userCode, UUID accountId, Integer ttlDays) {
        var days = ttlDays == null ? defaultTtlDays : ttlDays;
        if (days < 0 || days > MAX_TTL_DAYS) {
            throw new RestApiException(ErrorCodes.BadRequest.INVALID_PARAMETER, "만료 기간은 0(무기한)~" + MAX_TTL_DAYS + "일이어야 합니다.");
        }
        var account = AccessTokenServiceImpl.requireAccount(accountRepository, accountId);
        if (account.isGuest()) {
            throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST);
        }
        var authorization = findPending(userCode);
        authorization.approve(account, days);
        log.info("device authorization {} approved by {} ({} days)", authorization.getUserCode(), account.getEmail(), days);
        return toDto(authorization);
    }

    @Override
    @Transactional
    public DeviceAuthorizationDto deny(String userCode, UUID accountId) {
        var account = AccessTokenServiceImpl.requireAccount(accountRepository, accountId);
        var authorization = findPending(userCode);
        authorization.deny(account);
        log.info("device authorization {} denied by {}", authorization.getUserCode(), account.getEmail());
        return toDto(authorization);
    }

    @Override
    @Transactional
    public long purgeExpired() {
        return deviceAuthorizationRepository.deleteAllByExpiresAtBefore(LocalDateTime.now().minusDays(1));
    }

    private DeviceAuthorization findPending(String userCode) {
        var normalized = normalize(userCode);
        if (normalized == null) {
            throw new RestApiException(ErrorCodes.NotFound.NOT_FOUND_DEVICE_CODE);
        }
        return deviceAuthorizationRepository
                .findFirstByUserCodeAndStatusOrderByCreatedAtDesc(normalized, DeviceAuthorization.Status.PENDING)
                .filter(x -> !x.isExpired())
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND_DEVICE_CODE));
    }

    /**
     * "bcdf ghjk", "BCDF-GHJK" → "BCDF-GHJK" (코드가 될 수 없으면 null)
     */
    static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        var letters = raw.toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");
        return letters.length() == 8 ? letters.substring(0, 4) + "-" + letters.substring(4) : null;
    }

    private String newUserCode() {
        var sb = new StringBuilder(9);
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(USER_CODE_ALPHABET.charAt(random.nextInt(USER_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private DeviceAuthorizationDto toDto(DeviceAuthorization authorization) {
        return DeviceAuthorizationDto.builder()
                .userCode(authorization.getUserCode())
                .clientName(authorization.getClientName())
                .scopes(new ArrayList<>(AccessTokenScope.parse(authorization.getScopes()).stream().map(AccessTokenScope::getKey).toList()))
                .requestedIp(authorization.getRequestedIp())
                .requestedUa(authorization.getRequestedUa())
                .status(authorization.getStatus())
                .createdAt(authorization.getCreatedAt())
                .expiresAt(authorization.getExpiresAt())
                .build();
    }
}
