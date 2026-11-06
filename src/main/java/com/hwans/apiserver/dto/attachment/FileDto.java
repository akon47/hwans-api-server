package com.hwans.apiserver.dto.attachment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.io.Serializable;
import java.util.UUID;

/**
 * 파일 Dto
 */
@Getter
@Builder
@Schema(description = "파일 Dto")
public class FileDto implements Serializable {
    @Schema(description = "파일 Id")
    UUID id;
    @Schema(description = "Mime 타입")
    String contentType;
    @Schema(description = "파일 URL")
    @With
    String url;
    @Schema(description = "파일 크기 (bytes)")
    Long fileSize;
    @Schema(description = "파일 이름")
    String fileName;
    @JsonIgnore
    String localFilePath;
}
