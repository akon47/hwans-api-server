package com.hwans.apiserver.dto.attachment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Builder
@ApiModel(description = "파일 Dto")
public class FileDto implements Serializable {
    @ApiModelProperty(value = "파일 Id")
    UUID id;
    @ApiModelProperty(value = "Mime 타입")
    String contentType;
    @ApiModelProperty(value = "파일 URL")
    @With
    String url;
    @ApiModelProperty(value = "파일 크기 (bytes)")
    Long fileSize;
    @ApiModelProperty(value = "파일 이름")
    String fileName;
    @JsonIgnore
    String localFilePath;
}
