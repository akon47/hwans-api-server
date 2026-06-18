package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.dto.admin.AdminAccountDto;
import com.hwans.apiserver.dto.admin.AdminActiveViewersDto;
import com.hwans.apiserver.dto.admin.AdminStatisticsDto;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

/**
 * 관리자 서비스 인터페이스
 */
public interface AdminService {
    /**
     * 회원(손님 제외) 목록을 조회한다. 정지된 회원도 포함된다.
     *
     * @param search   이메일/이름/블로그Id 검색어 (없으면 전체)
     * @param cursorId 페이징 조회를 위한 기준 cursorId
     * @param size     조회를 원하는 최대 size
     * @return 조회된 회원 목록(페이징)
     */
    SliceDto<AdminAccountDto> getAccounts(Optional<String> search, Optional<UUID> cursorId, int size);

    /**
     * 회원 계정을 정지(비활성화)한다.
     *
     * @param accountId 정지할 계정 Id
     */
    void disableAccount(UUID accountId);

    /**
     * 정지된 회원 계정을 복구한다.
     *
     * @param accountId 복구할 계정 Id
     */
    void restoreAccount(UUID accountId);

    /**
     * 관리자 대시보드 통계를 조회한다.
     *
     * @return 통계 정보
     */
    AdminStatisticsDto getStatistics();

    /**
     * 현재 실시간으로 게시글을 보고 있는 접속자 목록(IP/회원/User-Agent/게시글)을 조회한다.
     *
     * @return 실시간 접속자 목록과 전체 접속 세션 수
     */
    AdminActiveViewersDto getActiveViewers();
}
