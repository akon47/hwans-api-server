package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.account.CreateAccountDto;
import com.hwans.apiserver.dto.account.AccountDto;
import com.hwans.apiserver.dto.account.ModifyAccountDto;
import com.hwans.apiserver.service.account.AccountService;
import com.hwans.apiserver.service.attachment.AttachmentService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.mail.MailSenderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Email;

@Validated
@RestController
@Api(tags = "사용자 계정")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final MailSenderService mailSenderService;
    private final AttachmentService attachmentService;

    @ApiOperation(value = "사용자 계정 생성", notes = "새로운 사용자 계정을 생성한다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/accounts")
    public AccountDto signup(@ApiParam(value = "새로운 사용자 생성 정보", required = true) @RequestBody @Valid final CreateAccountDto userCreateDto,
                             @ApiParam(value = "OAuth2 인증을 통해 계정을 생성하는 경우 Register 토큰", required = false) @RequestParam(required = false) String registerToken) {
        if (registerToken != null) {
            return accountService.createAccount(userCreateDto, registerToken);
        } else {
            return accountService.createAccount(userCreateDto);
        }
    }

    @ApiOperation(value = "사용자 계정 정보 수정", notes = "새로운 사용자 계정 정보를 수정한다.", tags = "사용자 계정")
    @PutMapping(value = "/v1/accounts")
    public AccountDto modify(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                             @ApiParam(value = "사용자 정보", required = true) @RequestBody @Valid final ModifyAccountDto modifyAccountDto) {
        return accountService.modifyAccount(userAuthenticationDetails.getId(), modifyAccountDto);
    }

    @ApiOperation(value = "사용자 이메일 인증 코드 발송", notes = "새로운 사용자 계정 생성을 위해 이메일 인증 코드를 발송합니다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/accounts/verify-email")
    public void verifyEmail(@ApiParam(value = "인증 코드를 발송할 이메일 주소", required = true) @RequestParam @Email String email) {
        var verifyCode = accountService.setEmailVerifyCode(email);
        mailSenderService.sendMailVerifyCode(email, verifyCode);
    }

    @ApiOperation(value = "현재 사용자 계정 정보 조회", notes = "현재 사용자 계정 정보를 조회합니다.", tags = "사용자 계정")
    @GetMapping(value = "/v1/accounts/me")
    public AccountDto getActualAccount() {
        return accountService.getCurrentAccount();
    }

    @ApiOperation(value = "현재 사용자 프로필 이미지 생성", notes = "현재 사용자 프로필 이미지를 생성한다.", tags = "사용자 계정")
    @PostMapping(value = "/v1/accounts/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AccountDto uploadProfileImage(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                         @ApiParam(value = "사용자 프로필 이미지 파일", required = true) @RequestPart MultipartFile profileImageFile) {
        var attachment = attachmentService.saveFile(userAuthenticationDetails.getId(), profileImageFile);
        // TODO: 기존 프로필 이미지가 존재한다면 삭제 동작 필요
        return accountService.setProfileImage(userAuthenticationDetails.getId(), attachment.getId());
    }
}
