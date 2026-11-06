package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.ClientIp;
import com.hwans.apiserver.common.security.token.RedisRateLimiter;
import com.hwans.apiserver.dto.token.DeviceApproveRequestDto;
import com.hwans.apiserver.dto.token.DeviceAuthorizationDto;
import com.hwans.apiserver.dto.token.DeviceCodeDto;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.token.DeviceFlowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 기기 로그인 Controller (RFC 8628 형태).
 * 에이전트가 코드를 요청하고(인증 없음), 사람이 웹에서 승인하면(JWT), 에이전트가 폴링으로 토큰을 받는다(인증 없음).
 */
@RestController
@Tag(name = "기기 로그인")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class DeviceAuthorizationController {
    private static final int STARTS_PER_MINUTE_PER_IP = 10;

    private final DeviceFlowService deviceFlowService;
    private final RedisRateLimiter rateLimiter;

    @Operation(summary = "기기 로그인 시작", description = "에이전트가 호출한다. device_code(비밀)와 사람이 승인 화면에 넣을 user_code 를 돌려준다.")
    @PostMapping(value = "/v1/authentication/device/code")
    public ResponseEntity<DeviceCodeDto> start(@Parameter(description = "승인 화면에 보일 클라이언트 이름", example = "Claude Code - 블로그 글쓰기") @RequestParam(value = "client_name", required = false) String clientName,
                                               @Parameter(description = "요청 권한 (공백 구분: posts social, 생략하면 모두)", example = "posts social") @RequestParam(required = false) String scope,
                                               HttpServletRequest request) {
        var clientIp = ClientIp.of(request);
        if (!rateLimiter.allow("device-code", clientIp, STARTS_PER_MINUTE_PER_IP)) {
            throw new RestApiException(ErrorCodes.TooManyRequests.TOO_MANY_REQUESTS);
        }
        var started = deviceFlowService.start(clientName, scope, clientIp, request.getHeader("User-Agent"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(started);
    }

    @Operation(summary = "기기 로그인 토큰 폴링", description = "에이전트가 interval 초마다 호출한다. 승인되면 200 과 access_token, 아니면 400 과 error(authorization_pending | slow_down | access_denied | expired_token | invalid_grant).")
    @PostMapping(value = "/v1/authentication/device/token")
    public ResponseEntity<Map<String, Object>> token(@Parameter(description = "기기 로그인 시작 때 받은 device_code", required = true) @RequestParam(value = "device_code", required = false) String deviceCode,
                                                     HttpServletRequest request) {
        var result = deviceFlowService.poll(deviceCode, ClientIp.of(request));
        var body = new LinkedHashMap<String, Object>();
        if (result.error() != null) {
            body.put("error", result.error());
            body.put("error_description", result.errorDescription());
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(body);
        }
        var issued = result.issued();
        body.put("access_token", issued.getToken());
        body.put("token_type", "Bearer");
        body.put("scope", String.join(" ", issued.getAccessToken().getScopes()));
        body.put("expires_at", issued.getAccessToken().getExpiresAt());
        body.put("token_id", issued.getAccessToken().getTokenId());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @Operation(summary = "기기 로그인 요청 조회", description = "사람이 승인 화면에서 코드로 요청 내용을(클라이언트 이름, IP, 권한) 확인한다.")
    @GetMapping(value = "/v1/authentication/device/{userCode}")
    public DeviceAuthorizationDto getPending(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                             @Parameter(description = "사용자 코드", example = "BCDF-GHJK") @PathVariable String userCode) {
        return deviceFlowService.getPending(userCode);
    }

    @Operation(summary = "기기 로그인 승인", description = "요청을 승인한다. 현재 사용자가 토큰 주인이 되며, 토큰은 에이전트의 다음 폴링에서 발급된다.")
    @PostMapping(value = "/v1/authentication/device/{userCode}/approve")
    public DeviceAuthorizationDto approve(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                          @Parameter(description = "사용자 코드", example = "BCDF-GHJK") @PathVariable String userCode,
                                          @Parameter(description = "승인 옵션") @RequestBody(required = false) DeviceApproveRequestDto approveRequestDto) {
        var ttlDays = approveRequestDto == null ? null : approveRequestDto.getTtlDays();
        return deviceFlowService.approve(userCode, userAuthenticationDetails.getId(), ttlDays);
    }

    @Operation(summary = "기기 로그인 거부", description = "요청을 거부한다. 에이전트는 다음 폴링에서 access_denied 를 받는다.")
    @PostMapping(value = "/v1/authentication/device/{userCode}/deny")
    public DeviceAuthorizationDto deny(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                       @Parameter(description = "사용자 코드", example = "BCDF-GHJK") @PathVariable String userCode) {
        return deviceFlowService.deny(userCode, userAuthenticationDetails.getId());
    }
}
