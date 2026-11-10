package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.ClientIp;
import com.hwans.apiserver.common.security.token.RedisRateLimiter;
import com.hwans.apiserver.dto.site.AnnouncementDto;
import com.hwans.apiserver.dto.site.ReportRequestDto;
import com.hwans.apiserver.service.admin.AdminAuditService;
import com.hwans.apiserver.service.moderation.ModerationService;
import com.hwans.apiserver.service.site.SiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 사이트 공통 Controller: 공지 배너 조회와 글·댓글 신고 (둘 다 로그인 없이 쓸 수 있다)
 */
@Validated
@RestController
@Tag(name = "사이트")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class SiteController {
    // 한 IP 가 1분에 접수할 수 있는 신고 수
    private static final int REPORTS_PER_MINUTE_PER_IP = 5;

    private final SiteService siteService;
    private final AdminAuditService adminAuditService;
    private final RedisRateLimiter rateLimiter;
    private final ModerationService moderationService;

    @Operation(summary = "사이트 공지 조회", description = "화면 맨 위에 보여줄 공지를 조회한다. 공지가 없으면 enabled=false.")
    @GetMapping(value = "/v1/site/announcement")
    public AnnouncementDto getAnnouncement() {
        return siteService.getPublicAnnouncement();
    }

    @Operation(summary = "글·댓글 신고", description = "글 또는 댓글을 신고한다. 로그인하지 않아도 된다. (IP당 분당 5회)")
    @PostMapping(value = "/v1/site/reports")
    public void createReport(@Parameter(description = "신고 내용", required = true) @RequestBody @Valid final ReportRequestDto request,
                             HttpServletRequest httpRequest) {
        var clientIp = ClientIp.of(httpRequest);
        moderationService.assertNotBlocked(clientIp);
        if (!rateLimiter.allow("report", clientIp, REPORTS_PER_MINUTE_PER_IP)) {
            throw new RestApiException(ErrorCodes.TooManyRequests.TOO_MANY_REQUESTS);
        }
        siteService.createReport(request, adminAuditService.currentEmail(), clientIp);
    }
}
