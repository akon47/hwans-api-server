package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.attachment.SimpleFileDto;
import com.hwans.apiserver.service.attachment.AttachmentResource;
import com.hwans.apiserver.service.attachment.AttachmentService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@RestController
@Api(tags = "파일")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    // TODO: API 서버에서 처리하는게 아닌 파일 서버를 따로 구성하여 해당 서버가 정적 파일에 대한 응답을 해주도록 수정 필요
    @ApiOperation(value = "파일 다운로드", notes = "파일 Id를 이용하여 파일을 다운로드한다.", tags = "파일")
    @GetMapping(value = "/v1/attachments/{fileId}")
    public HttpEntity getFile(@ApiParam(value = "파일 Id") @PathVariable UUID fileId) {
        var resource = attachmentService.getFileAsResource(fileId);

        var headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder("attachment").filename(resource.getFileName()).build());
        return createAttachmentResponseEntity(resource, headers);
    }

    @ApiOperation(value = "파일 다운로드", notes = "파일 Id를 이용하여 파일을 다운로드한다.", tags = "파일")
    @GetMapping(value = "/v1/attachments/{fileId}/{fileTypeWithExt}")
    public HttpEntity getFile(@ApiParam(value = "파일 Id") @PathVariable UUID fileId,
                              @ApiParam(value = "파일 타입 + 확장자") @PathVariable String fileTypeWithExt) {
        var resource = attachmentService.getFileAsResource(fileId, fileTypeWithExt);

        var headers = new HttpHeaders();
        return createAttachmentResponseEntity(resource, headers);
    }

    @ApiOperation(value = "파일 업로드", notes = "파일을 업로드합니다.", tags = "파일")
    @PostMapping(value = "/v1/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimpleFileDto uploadFile(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                    @ApiParam(value = "파일", required = true) @RequestPart MultipartFile file) {
        var attachment = attachmentService.saveFile(userAuthenticationDetails.getId(), file);
        return SimpleFileDto.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .fileName(attachment.getFileName())
                .build();
    }

    @ApiOperation(value = "파일 업로드", notes = "파일을 업로드합니다.", tags = "파일")
    @PostMapping(value = "/v1/attachments", params = {"fileUrl"})
    public SimpleFileDto uploadFileFromUrl(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                           @ApiParam(value = "파일", required = true) @RequestParam(value = "fileUrl") String fileUrl) {
        var attachment = attachmentService.saveFileFromUrl(userAuthenticationDetails.getId(), fileUrl);
        return SimpleFileDto.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .fileName(attachment.getFileName())
                .build();
    }

    private HttpEntity createAttachmentResponseEntity(AttachmentResource resource, HttpHeaders headers) {
        headers.setContentType(resource.getContentType());
        headers.setContentLength(resource.getContentLength());
        headers.setCacheControl(CacheControl.maxAge(Duration.ofDays(7)));
        headers.setLastModified(ZonedDateTime.of(resource.getLastModifiedAt(), ZoneId.systemDefault()));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }
}
