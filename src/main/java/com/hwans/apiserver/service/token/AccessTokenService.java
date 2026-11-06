package com.hwans.apiserver.service.token;

import com.hwans.apiserver.common.security.token.AccessTokenDetails;
import com.hwans.apiserver.dto.token.AccessTokenDto;
import com.hwans.apiserver.dto.token.IssuedAccessTokenDto;
import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import com.hwans.apiserver.entity.token.AccessTokenScope;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 개인 액세스 토큰 서비스 인터페이스
 */
public interface AccessTokenService {
    /**
     * 토큰 문자열 접두어 (hwanstory personal)
     */
    String PREFIX = "hsp_";

    /**
     * 토큰을 발급한다. 토큰 원문은 응답에서 한 번만 볼 수 있다.
     *
     * @param accountId 토큰 주인 계정 Id
     * @param name      토큰 이름
     * @param scopes    권한
     * @param ttlDays   만료까지 일수 (null 또는 0 이면 무기한)
     * @param origin    발급 경로
     * @return 발급된 토큰 (원문 포함)
     */
    IssuedAccessTokenDto create(UUID accountId, String name, Set<AccessTokenScope> scopes, Integer ttlDays, AccessTokenOrigin origin);

    /**
     * 제시된 토큰 문자열을 검증하고, 유효하면 토큰 주인 정보를 반환한다. 마지막 사용 시각·IP 를 기록한다.
     *
     * @param presentedToken Authorization 헤더의 토큰 문자열 (Bearer 제외)
     * @param clientIp       요청 IP
     * @return 유효한 경우 토큰 주인 정보
     */
    Optional<AccessTokenDetails> authenticate(String presentedToken, String clientIp);

    /**
     * 계정의 토큰 목록을 조회한다 (폐기·만료된 것 포함, 최근 발급 순).
     *
     * @param accountId 계정 Id
     * @return 토큰 목록
     */
    List<AccessTokenDto> getTokens(UUID accountId);

    /**
     * 토큰 공개 Id 로 토큰 정보를 조회한다.
     *
     * @param tokenId 토큰 공개 Id
     * @return 토큰 정보
     */
    AccessTokenDto getToken(String tokenId);

    /**
     * 계정의 토큰을 폐기한다.
     *
     * @param accountId 계정 Id
     * @param id        토큰 레코드 Id
     */
    void revoke(UUID accountId, UUID id);

    /**
     * 토큰 공개 Id 로 토큰을 폐기한다 (에이전트가 작업을 마치고 자기 토큰을 폐기할 때).
     *
     * @param tokenId 토큰 공개 Id
     */
    void revokeByTokenId(String tokenId);
}
