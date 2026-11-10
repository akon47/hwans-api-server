package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.admin.*;
import com.hwans.apiserver.dto.blog.SimplePostDto;
import com.hwans.apiserver.dto.common.PageDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.dto.site.AnnouncementDto;
import com.hwans.apiserver.entity.admin.Report;
import com.hwans.apiserver.entity.blog.OpenType;
import com.hwans.apiserver.service.admin.AdminAuditService;
import com.hwans.apiserver.service.site.SiteService;
import com.hwans.apiserver.service.admin.AdminService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 어드민 Controller (모든 엔드포인트는 WebSecurityConfig 에서 ROLE_ADMIN 만 허용된다)
 */
@Validated
@RestController
@Tag(name = "어드민")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final AdminAuditService adminAuditService;
    private final SiteService siteService;
    private final com.hwans.apiserver.service.moderation.ModerationService moderationService;
    private final com.hwans.apiserver.service.stats.StatsService statsService;

    // ───────────── 인사이트 ─────────────

    @Operation(summary = "사이트 인사이트 조회", description = "기간 동안의 날짜별 조회수, 많이 읽힌 글, 많이 찾는 검색어를 조회한다.")
    @GetMapping(value = "/v1/admin/insights")
    public com.hwans.apiserver.dto.stats.PostStatsDto.Insights getInsights(@Parameter(description = "조회 기간(일, 7~90)") @RequestParam(required = false, defaultValue = "30") int days) {
        return statsService.getInsights(days);
    }

    // ───────────── 차단 ─────────────

    @Operation(summary = "차단한 IP 목록 조회", description = "차단한 IP 를 최근 순으로 조회한다.")
    @GetMapping(value = "/v1/admin/blocked-ips")
    public List<AdminModerationDto.BlockedIp> getBlockedIps() {
        return moderationService.getBlockedIps();
    }

    @Operation(summary = "IP 차단", description = "IP 를 차단한다. 차단된 IP 는 손님 댓글과 신고를 할 수 없다.")
    @PostMapping(value = "/v1/admin/blocked-ips")
    public AdminModerationDto.BlockedIp blockIp(@Parameter(description = "차단 요청", required = true) @RequestBody @Valid final AdminModerationDto.BlockIpRequest request) {
        return moderationService.blockIp(request.getIp(), request.getReason());
    }

    @Operation(summary = "IP 차단 해제", description = "IP 차단을 해제한다.")
    @DeleteMapping(value = "/v1/admin/blocked-ips/{blockedIpId}")
    public void unblockIp(@Parameter(description = "차단 Id") @PathVariable UUID blockedIpId) {
        moderationService.unblockIp(blockedIpId);
    }

    @Operation(summary = "금칙어 조회", description = "금칙어 목록을 조회한다.")
    @GetMapping(value = "/v1/admin/banned-words")
    public AdminModerationDto.BannedWords getBannedWords() {
        return moderationService.getBannedWords();
    }

    @Operation(summary = "금칙어 저장", description = "금칙어 목록을 통째로 바꾼다. 댓글과 손님 이름에 들어 있으면 등록을 막는다.")
    @PutMapping(value = "/v1/admin/banned-words")
    public AdminModerationDto.BannedWords setBannedWords(@Parameter(description = "금칙어 목록", required = true) @RequestBody @Valid final AdminModerationDto.BannedWords request) {
        return moderationService.setBannedWords(request.getWords());
    }

    @Operation(summary = "회원 상세 조회", description = "한 회원의 글·댓글·신고 이력을 모아 조회한다.")
    @GetMapping(value = "/v1/admin/accounts/{accountId}/detail")
    public AdminAccountDetailDto getAccountDetail(@Parameter(description = "계정 Id") @PathVariable UUID accountId) {
        return adminService.getAccountDetail(accountId);
    }

    // ───────────── 활동 기록 ─────────────

    @Operation(summary = "관리자 활동 기록 조회", description = "관리자가 바꾼 것(정지·삭제·권한·공지·신고 처리 등)의 기록을 최근 순으로 조회한다.")
    @GetMapping(value = "/v1/admin/audit-logs")
    public PageDto<AdminAuditLogDto> getAuditLogs(@Parameter(description = "페이지 번호 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
                                                  @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "30") int size) {
        return adminAuditService.getLogs(page, size);
    }

    // ───────────── 공지 ─────────────

    @Operation(summary = "사이트 공지 조회", description = "저장된 공지를 조회한다. (꺼져 있어도 내용이 보인다)")
    @GetMapping(value = "/v1/admin/announcement")
    public AnnouncementDto getAnnouncement() {
        return siteService.getAnnouncement();
    }

    @Operation(summary = "사이트 공지 저장", description = "화면 맨 위 공지 배너의 내용·종류·링크·표시 여부를 저장한다.")
    @PutMapping(value = "/v1/admin/announcement")
    public AnnouncementDto setAnnouncement(@Parameter(description = "공지", required = true) @RequestBody @Valid final AnnouncementDto request) {
        return siteService.setAnnouncement(request);
    }

    // ───────────── 신고 ─────────────

    @Operation(summary = "신고 목록 조회", description = "글·댓글 신고를 최근 순으로 조회한다. status 를 주면 그 상태만 본다.")
    @GetMapping(value = "/v1/admin/reports")
    public PageDto<AdminReportDto> getReports(@Parameter(description = "처리 상태 필터") @RequestParam(required = false) Optional<Report.Status> status,
                                              @Parameter(description = "페이지 번호 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
                                              @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") int size) {
        return siteService.getReports(status, page, size);
    }

    @Operation(summary = "신고 처리", description = "신고를 처리 완료/기각/다시 열기로 바꾼다. deleteTarget 이면 신고된 글·댓글도 삭제한다.")
    @PutMapping(value = "/v1/admin/reports/{reportId}")
    public AdminReportDto handleReport(@Parameter(description = "신고 Id") @PathVariable UUID reportId,
                                       @Parameter(description = "처리 요청", required = true) @RequestBody @Valid final AdminReportHandleRequestDto request) {
        return siteService.handleReport(reportId, request.getStatus(), request.isDeleteTarget());
    }

    // ───────────── 대시보드 ─────────────

    @Operation(summary = "대시보드 조회", description = "요약 수치, 최근 30일 일별 추이, 최근 글·댓글·가입 회원을 한 번에 조회한다.")
    @GetMapping(value = "/v1/admin/dashboard")
    public AdminDashboardDto getDashboard() {
        return adminService.getDashboard();
    }

    @Operation(summary = "대시보드 통계 조회", description = "관리자 대시보드 통계를 조회한다.")
    @GetMapping(value = "/v1/admin/statistics")
    public AdminStatisticsDto getStatistics() {
        return adminService.getStatistics();
    }

    @Operation(summary = "시스템 상태 조회", description = "버전·가동 시간·메모리·DB/Redis 연결·첨부파일 용량·대기 작업 수를 조회한다.")
    @GetMapping(value = "/v1/admin/system")
    public AdminSystemDto getSystem() {
        return adminService.getSystem();
    }

    // ───────────── 회원 ─────────────

    @Operation(summary = "회원 목록 조회", description = "회원(손님 제외) 목록을 조회한다. 정지된 회원도 포함된다.")
    @GetMapping(value = "/v1/admin/accounts")
    public SliceDto<AdminAccountDto> getAccounts(@Parameter(description = "이메일/이름/블로그Id 검색어") @RequestParam(required = false) Optional<String> search,
                                                 @Parameter(description = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                 @Parameter(description = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return adminService.getAccounts(search, cursorId, size);
    }

    @Operation(summary = "회원 정지", description = "회원 계정을 정지(비활성화)한다.")
    @PutMapping(value = "/v1/admin/accounts/{accountId}/disable")
    public void disableAccount(@Parameter(description = "계정 Id") @PathVariable UUID accountId) {
        adminService.disableAccount(accountId);
    }

    @Operation(summary = "회원 정지 해제", description = "정지된 회원 계정을 복구한다.")
    @PutMapping(value = "/v1/admin/accounts/{accountId}/restore")
    public void restoreAccount(@Parameter(description = "계정 Id") @PathVariable UUID accountId) {
        adminService.restoreAccount(accountId);
    }

    @Operation(summary = "관리자 권한 부여/회수", description = "회원에게 관리자 권한을 주거나 뺀다. 자기 자신의 권한은 뺄 수 없다.")
    @PutMapping(value = "/v1/admin/accounts/{accountId}/roles")
    public AdminAccountDto setAdminRole(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                        @Parameter(description = "계정 Id") @PathVariable UUID accountId,
                                        @Parameter(description = "권한 요청", required = true) @RequestBody @Valid final AdminRoleRequestDto request) {
        return adminService.setAdminRole(userAuthenticationDetails.getId(), accountId, request.isAdmin());
    }

    // ───────────── 글 ─────────────

    @Operation(summary = "글 목록 조회", description = "모든 글(비공개·임시저장·예약 포함, 삭제 제외)을 페이지 번호로 조회한다.")
    @GetMapping(value = "/v1/admin/posts")
    public PageDto<SimplePostDto> getPosts(@Parameter(description = "제목/작성자 검색어") @RequestParam(required = false) Optional<String> search,
                                           @Parameter(description = "공개 유형 필터") @RequestParam(required = false) Optional<OpenType> openType,
                                           @Parameter(description = "페이지 번호 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
                                           @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") int size) {
        return adminService.getPosts(search, openType, page, size);
    }

    @Operation(summary = "글 공개 유형 변경", description = "글의 공개 유형을 바꾼다. (문제가 있는 글을 비공개로 돌릴 때)")
    @PutMapping(value = "/v1/admin/posts/{postId}/open-type")
    public SimplePostDto changePostOpenType(@Parameter(description = "글 Id") @PathVariable UUID postId,
                                            @Parameter(description = "공개 유형", required = true) @RequestBody @Valid final AdminOpenTypeRequestDto request) {
        return adminService.changePostOpenType(postId, request.getOpenType());
    }

    @Operation(summary = "글 삭제", description = "글을 삭제한다.")
    @DeleteMapping(value = "/v1/admin/posts/{postId}")
    public void deletePost(@Parameter(description = "글 Id") @PathVariable UUID postId) {
        adminService.deletePost(postId);
    }

    // ───────────── 댓글 ─────────────

    @Operation(summary = "댓글 목록 조회", description = "모든 댓글(삭제 제외)을 페이지 번호로 조회한다.")
    @GetMapping(value = "/v1/admin/comments")
    public PageDto<AdminCommentDto> getComments(@Parameter(description = "내용/작성자/글 제목 검색어") @RequestParam(required = false) Optional<String> search,
                                                @Parameter(description = "페이지 번호 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
                                                @Parameter(description = "페이지 크기") @RequestParam(required = false, defaultValue = "20") int size) {
        return adminService.getComments(search, page, size);
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제한다.")
    @DeleteMapping(value = "/v1/admin/comments/{commentId}")
    public void deleteComment(@Parameter(description = "댓글 Id") @PathVariable UUID commentId) {
        adminService.deleteComment(commentId);
    }

    // ───────────── 액세스 토큰 ─────────────

    @Operation(summary = "액세스 토큰 목록 조회", description = "모든 회원의 개인 액세스 토큰을 조회한다. (토큰 원문은 없다)")
    @GetMapping(value = "/v1/admin/tokens")
    public List<AdminTokenDto> getTokens() {
        return adminService.getTokens();
    }

    @Operation(summary = "액세스 토큰 폐기", description = "토큰을 폐기한다. 그 토큰을 쓰는 에이전트는 즉시 401 을 받는다.")
    @DeleteMapping(value = "/v1/admin/tokens/{tokenId}")
    public void revokeToken(@Parameter(description = "토큰 레코드 Id") @PathVariable UUID tokenId) {
        adminService.revokeToken(tokenId);
    }

    // ───────────── 실시간 ─────────────

    @Operation(summary = "실시간 접속자 조회", description = "현재 게시글을 보고 있는 접속자(IP/회원/User-Agent/게시글) 목록을 조회한다.")
    @GetMapping(value = "/v1/admin/active-viewers")
    public AdminActiveViewersDto getActiveViewers() {
        return adminService.getActiveViewers();
    }
}
