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
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @ApiOperation(value = "파일 조회", notes = "파일 Id를 이용하여 파일을 조회한다.", tags = "파일")
    @GetMapping(value = "/attachments/{fileId}")
    public ResponseEntity<AttachmentResource> getFile(@ApiParam(value = "파일 Id") @PathVariable UUID fileId) {
        var resource = attachmentService.getFileAsResource(fileId);

        var headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").build());
        headers.setContentType(resource.getContentType());
        headers.setContentLength(resource.getContentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
