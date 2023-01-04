package com.hwans.apiserver.dto.attachment;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

/**
 * 간단한 파일 Dto
 */
@Getter
@Builder
@ApiModel(description = "간단한 파일 Dto")
public class SimpleFileDto implements Serializable {
    @ApiModelProperty(value = "파일 Id")
    UUID id;
    @ApiModelProperty(value = "파일 URL")
    String url;
    @ApiModelProperty(value = "파일 이름")
    String fileName;
}
