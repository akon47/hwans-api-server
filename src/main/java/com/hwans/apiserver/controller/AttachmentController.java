package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.dto.attachment.SimpleFileDto;
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

import java.util.UUID;

@RestController
@Api(tags = "파일")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class AttachmentController {
    private final AttachmentService attachmentService;

    // TODO: API 서버에서 처리하는게 아닌 파일 서버를 따로 구성하여 해당 서버가 정적 파일에 대한 응답을 해주도록 수정 필요
    @ApiOperation(value = "파일 다운로드", notes = "파일 Id를 이용하여 파일을 다운로드한다.", tags = "파일", response = Void.class)
    @GetMapping(value = "/v1/attachments/{fileId}")
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

    @ApiOperation(value = "이미지 파일 업로드", notes = "이미지 파일을 업로드합니다.", tags = "파일")
    @PostMapping(value = "/v1/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SimpleFileDto uploadImageFile(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                            @ApiParam(value = "이미지 파일", required = true) @RequestPart MultipartFile imageFile) {
        var attachment = attachmentService.saveImageFile(userAuthenticationDetails.getId(), imageFile);
        return SimpleFileDto.builder()
                .id(attachment.getId())
                .url(attachment.getUrl())
                .fileName(attachment.getFileName())
                .build();
    }
}
