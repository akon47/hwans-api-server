package com.hwans.apiserver.dto.chat;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅 메시지 Dto
 */
@Getter
@Builder
@Schema(description = "채팅 메시지 Dto")
public class ChatMessageDto implements Serializable {
    @Schema(description = "메시지 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "메시지 내용", requiredMode = Schema.RequiredMode.REQUIRED, example = "안녕히세요")
    @NotBlank
    String content;
    @Schema(description = "메시지 주인", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    SimpleAccountDto owner;
    @Schema(description = "메시지 생성 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    LocalDateTime createdAt;
}
