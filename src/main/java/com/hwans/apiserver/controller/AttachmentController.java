package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.blog.*;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.service.account.AccountService;
import com.hwans.apiserver.service.attachment.AttachmentResource;
import com.hwans.apiserver.service.attachment.AttachmentService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.blog.BlogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

@RestController
@Api(tags = "파일")
@RequestMapping
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    @ApiOperation(value = "파일 다운로드", notes = "파일 Id를 이용하여 파일을 다운로드한다.", tags = "파일", response = Void.class)
    @GetMapping(value = "/attachments/{fileId}")
    public HttpEntity getFile(@ApiParam(value = "파일 Id") @PathVariable UUID fileId) {
        var resource = attachmentService.getFileAsResource(fileId);

        var headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFileName()).build());
        headers.setContentType(resource.getContentType());
        headers.setContentLength(resource.getContentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
