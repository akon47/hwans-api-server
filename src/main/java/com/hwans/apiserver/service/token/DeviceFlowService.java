package com.hwans.apiserver.service.token;

import com.hwans.apiserver.dto.token.DeviceAuthorizationDto;
import com.hwans.apiserver.dto.token.DeviceCodeDto;
import com.hwans.apiserver.dto.token.IssuedAccessTokenDto;

import java.util.UUID;

/**
 * 기기 로그인(RFC 8628 형태) 서비스 인터페이스.
 * <ol>
 *   <li>에이전트: {@code POST /api/v1/authentication/device/code} → device_code(비밀) + user_code + 승인 주소</li>
 *   <li>사람: 웹 {@code /device?code=USER-CODE} 에서 클라이언트 이름·IP·권한을 확인하고 승인</li>
 *   <li>에이전트: {@code POST /api/v1/authentication/device/token} 을 interval 초마다 폴링 → 승인 뒤 첫 폴링에서 토큰 발급</li>
 * </ol>
 */
public interface DeviceFlowService {
    /**
     * 폴링 최소 간격 (초)
     */
    int INTERVAL_SECONDS = 5;

    /**
     * 폴링 결과. error 가 null 이면 issued 에 발급된 토큰이 있다.
     * error 는 RFC 8628 코드: authorization_pending, slow_down, access_denied, expired_token, invalid_grant, invalid_request
     */
    record PollResult(String error, String errorDescription, IssuedAccessTokenDto issued) {
        public static PollResult error(String code, String description) {
            return new PollResult(code, description, null);
        }

        public static PollResult issued(IssuedAccessTokenDto issued) {
            return new PollResult(null, null, issued);
        }
    }

    DeviceCodeDto start(String clientName, String scope, String requestedIp, String userAgent);

    PollResult poll(String deviceCode, String clientIp);

    /**
     * 승인 대기 중인(만료되지 않은) 요청을 조회한다.
     *
     * @param userCode 사용자 코드 (대소문자·하이픈 무관)
     * @return 요청 정보
     */
    DeviceAuthorizationDto getPending(String userCode);

    /**
     * 요청을 승인한다. 토큰은 에이전트의 다음 폴링에서 발급된다.
     *
     * @param userCode  사용자 코드
     * @param accountId 승인하는 계정 Id (토큰 주인이 된다)
     * @param ttlDays   발급할 토큰의 만료까지 일수 (null 이면 기본값, 0 이면 무기한)
     * @return 요청 정보
     */
    DeviceAuthorizationDto approve(String userCode, UUID accountId, Integer ttlDays);

    DeviceAuthorizationDto deny(String userCode, UUID accountId);

    /**
     * 만료된 지 오래된 요청을 지운다.
     *
     * @return 지운 개수
     */
    long purgeExpired();
}
