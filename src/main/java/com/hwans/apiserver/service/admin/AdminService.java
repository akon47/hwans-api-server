package com.hwans.apiserver.service.admin;

import com.hwans.apiserver.dto.admin.*;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.PageDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.blog.OpenType;

import java.util.List;
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
     * 한 회원의 글·댓글·신고 이력을 모아 조회한다.
     *
     * @param accountId 계정 Id
     * @return 회원 상세
     */
    AdminAccountDetailDto getAccountDetail(UUID accountId);

    /**
     * 정지된 회원 계정을 복구한다.
     *
     * @param accountId 복구할 계정 Id
     */
    void restoreAccount(UUID accountId);

    /**
     * 회원에게 관리자 권한을 주거나 뺀다.
     *
     * @param actorAccountId 요청한 관리자 계정 Id (자기 자신의 권한은 뺄 수 없다)
     * @param accountId      대상 계정 Id
     * @param admin          true 면 부여, false 면 회수
     * @return 바뀐 계정 정보
     */
    AdminAccountDto setAdminRole(UUID actorAccountId, UUID accountId, boolean admin);

    /**
     * 관리자 대시보드 통계를 조회한다.
     *
     * @return 통계 정보
     */
    AdminStatisticsDto getStatistics();

    /**
     * 관리자 대시보드(요약·최근 30일 추이·최근 활동)를 조회한다.
     *
     * @return 대시보드 정보
     */
    AdminDashboardDto getDashboard();

    /**
     * 모든 글(비공개·임시저장 포함, 삭제 제외)을 조회한다.
     *
     * @param search   제목/작성자 검색어
     * @param openType 공개 유형 필터 (없으면 전체)
     * @param page     페이지 번호 (0부터)
     * @param size     페이지 크기
     * @return 글 목록
     */
    PageDto<SimplePostDto> getPosts(Optional<String> search, Optional<OpenType> openType, int page, int size);

    /**
     * 글의 공개 유형을 바꾼다. (예: 문제가 있는 글을 비공개로)
     *
     * @param postId   글 Id
     * @param openType 바꿀 공개 유형
     * @return 바뀐 글
     */
    SimplePostDto changePostOpenType(UUID postId, OpenType openType);

    /**
     * 글을 삭제한다.
     *
     * @param postId 글 Id
     */
    void deletePost(UUID postId);

    /**
     * 모든 댓글(삭제 제외)을 조회한다.
     *
     * @param search 내용/작성자/글 제목 검색어
     * @param page   페이지 번호 (0부터)
     * @param size   페이지 크기
     * @return 댓글 목록
     */
    PageDto<AdminCommentDto> getComments(Optional<String> search, int page, int size);

    /**
     * 댓글을 삭제한다.
     *
     * @param commentId 댓글 Id
     */
    void deleteComment(UUID commentId);

    /**
     * 모든 회원의 개인 액세스 토큰을 조회한다. (최근 발급 순, 최대 300개)
     *
     * @return 토큰 목록
     */
    List<AdminTokenDto> getTokens();

    /**
     * 토큰을 폐기한다.
     *
     * @param tokenId 토큰 레코드 Id
     */
    void revokeToken(UUID tokenId);

    /**
     * 서버 상태(버전·가동 시간·메모리·DB/Redis·첨부파일·대기 작업)를 조회한다.
     *
     * @return 시스템 상태
     */
    AdminSystemDto getSystem();

    /**
     * 현재 실시간으로 게시글을 보고 있는 접속자 목록(IP/회원/User-Agent/게시글)을 조회한다.
     *
     * @return 실시간 접속자 목록과 전체 접속 세션 수
     */
    AdminActiveViewersDto getActiveViewers();
}
