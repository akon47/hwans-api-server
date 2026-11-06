package com.hwans.apiserver.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * 간단한 파일 Dto
 */
@Getter
@Builder
@Schema(description = "간단한 파일 Dto")
public class SimpleFileDto implements Serializable {
    @Schema(description = "파일 Id")
    UUID id;
    @Schema(description = "파일 URL")
    String url;
    @Schema(description = "파일 이름")
    String fileName;
}
