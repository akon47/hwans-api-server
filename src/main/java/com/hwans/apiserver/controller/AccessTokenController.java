package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.common.security.token.AccessTokenDetails;
import com.hwans.apiserver.dto.token.AccessTokenDto;
import com.hwans.apiserver.dto.token.CreateAccessTokenDto;
import com.hwans.apiserver.dto.token.CurrentAccessTokenDto;
import com.hwans.apiserver.dto.token.IssuedAccessTokenDto;
import com.hwans.apiserver.entity.token.AccessTokenOrigin;
import com.hwans.apiserver.entity.token.AccessTokenScope;
import com.hwans.apiserver.service.account.AccountService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.token.AccessTokenService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * 개인 액세스 토큰 Controller.
 * 목록·발급·폐기는 웹 로그인(JWT)으로만 할 수 있고, {@code /current} 는 토큰 자신이 쓰는 엔드포인트다.
 */
@Validated
@RestController
@Api(tags = "액세스 토큰")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AccessTokenController {
    private final AccessTokenService accessTokenService;
    private final AccountService accountService;

    @Value("${blog.web-base-url:https://hwanstory.kr}")
    private String webBaseUrl;
    @Value("${blog.api-base-url:https://api.hwanstory.kr}")
    private String apiBaseUrl;

    @ApiOperation(value = "내 액세스 토큰 목록 조회", notes = "현재 사용자가 발급한 개인 액세스 토큰 목록을 조회한다 (폐기·만료된 것 포함).", tags = "액세스 토큰")
    @GetMapping(value = "/v1/tokens")
    public List<AccessTokenDto> getTokens(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails) {
        return accessTokenService.getTokens(userAuthenticationDetails.getId());
    }

    @ApiOperation(value = "액세스 토큰 발급", notes = "개인 액세스 토큰을 발급한다. 토큰 원문은 이 응답에서 한 번만 볼 수 있다.", tags = "액세스 토큰")
    @PostMapping(value = "/v1/tokens")
    public IssuedAccessTokenDto createToken(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                            @ApiParam(value = "발급 요청", required = true) @RequestBody @Valid final CreateAccessTokenDto createAccessTokenDto) {
        return accessTokenService.create(userAuthenticationDetails.getId(), createAccessTokenDto.getName(),
                AccessTokenScope.fromKeys(createAccessTokenDto.getScopes()), createAccessTokenDto.getTtlDays(), AccessTokenOrigin.CONSOLE);
    }

    @ApiOperation(value = "액세스 토큰 폐기", notes = "개인 액세스 토큰을 폐기한다. 폐기 즉시 그 토큰을 쓰던 에이전트·스크립트는 401 을 받는다.", tags = "액세스 토큰")
    @DeleteMapping(value = "/v1/tokens/{id}")
    public void revokeToken(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                            @ApiParam(value = "토큰 레코드 Id") @PathVariable UUID id) {
        accessTokenService.revoke(userAuthenticationDetails.getId(), id);
    }

    @ApiOperation(value = "현재 토큰 정보 조회", notes = "지금 요청에 쓰인 액세스 토큰과 그 주인 계정 정보를 조회한다 (토큰으로 인증한 경우에만).", tags = "액세스 토큰")
    @GetMapping(value = "/v1/tokens/current")
    public CurrentAccessTokenDto getCurrentToken(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails) {
        var details = requireAccessTokenDetails();
        return CurrentAccessTokenDto.builder()
                .account(accountService.getAccount(userAuthenticationDetails.getId()))
                .token(accessTokenService.getToken(details.getTokenId()))
                .webBaseUrl(webBaseUrl)
                .apiBaseUrl(apiBaseUrl)
                .build();
    }

    @ApiOperation(value = "현재 토큰 폐기", notes = "지금 요청에 쓰인 액세스 토큰을 폐기한다 (에이전트가 작업을 마쳤을 때).", tags = "액세스 토큰")
    @DeleteMapping(value = "/v1/tokens/current")
    public void revokeCurrentToken() {
        accessTokenService.revokeByTokenId(requireAccessTokenDetails().getTokenId());
    }

    private static AccessTokenDetails requireAccessTokenDetails() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof AccessTokenDetails details) {
            return details;
        }
        throw new RestApiException(ErrorCodes.BadRequest.BAD_REQUEST, "액세스 토큰(hsp_…)으로 인증된 요청이 아닙니다.");
    }
}
