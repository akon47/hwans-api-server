package com.hwans.apiserver.dto.chat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;

/**
 * 채팅 메시지 Dto
 */
@Getter
@Builder
@ApiModel(description = "채팅 메시지 생성/수정 요청 Dto")
public class ChatMessageRequestDto {
    @ApiModelProperty(value = "메시지 내용", required = true, example = "안녕히세요")
    @NotBlank
    String content;
}
