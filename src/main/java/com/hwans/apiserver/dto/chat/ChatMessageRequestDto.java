package com.hwans.apiserver.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;

/**
 * 채팅 메시지 Dto
 */
@Getter
@Builder
@Schema(description = "채팅 메시지 생성/수정 요청 Dto")
public class ChatMessageRequestDto {
    @Schema(description = "메시지 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "안녕히세요")
    @NotBlank
    String content;
}
