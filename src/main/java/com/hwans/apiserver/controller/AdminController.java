package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.admin.AdminAccountDto;
import com.hwans.apiserver.dto.admin.AdminActiveViewersDto;
import com.hwans.apiserver.dto.admin.AdminStatisticsDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.service.admin.AdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * 어드민 Controller
 */
@Validated
@RestController
@Api(tags = "어드민")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @ApiOperation(value = "회원 목록 조회", notes = "회원(손님 제외) 목록을 조회한다. 정지된 회원도 포함된다.", tags = "어드민")
    @GetMapping(value = "/v1/admin/accounts")
    public SliceDto<AdminAccountDto> getAccounts(@ApiParam(value = "이메일/이름/블로그Id 검색어") @RequestParam(required = false) Optional<String> search,
                                                 @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                 @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return adminService.getAccounts(search, cursorId, size);
    }

    @ApiOperation(value = "회원 정지", notes = "회원 계정을 정지(비활성화)한다.", tags = "어드민")
    @PutMapping(value = "/v1/admin/accounts/{accountId}/disable")
    public void disableAccount(@ApiParam(value = "계정 Id") @PathVariable UUID accountId) {
        adminService.disableAccount(accountId);
    }

    @ApiOperation(value = "회원 정지 해제", notes = "정지된 회원 계정을 복구한다.", tags = "어드민")
    @PutMapping(value = "/v1/admin/accounts/{accountId}/restore")
    public void restoreAccount(@ApiParam(value = "계정 Id") @PathVariable UUID accountId) {
        adminService.restoreAccount(accountId);
    }

    @ApiOperation(value = "대시보드 통계 조회", notes = "관리자 대시보드 통계를 조회한다.", tags = "어드민")
    @GetMapping(value = "/v1/admin/statistics")
    public AdminStatisticsDto getStatistics() {
        return adminService.getStatistics();
    }

    @ApiOperation(value = "실시간 접속자 조회", notes = "현재 게시글을 보고 있는 접속자(IP/회원/User-Agent/게시글) 목록을 조회한다.", tags = "어드민")
    @GetMapping(value = "/v1/admin/active-viewers")
    public AdminActiveViewersDto getActiveViewers() {
        return adminService.getActiveViewers();
    }
}
