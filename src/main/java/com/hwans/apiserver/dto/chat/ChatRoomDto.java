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
 * 채팅방 Dto
 */
@Getter
@Builder
@Schema(description = "채팅방 Dto")
public class ChatRoomDto implements Serializable {
    @Schema(description = "채팅방 Id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    UUID id;
    @Schema(description = "채팅방 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "테스트 채팅방")
    @NotBlank
    String title;
    @Schema(description = "채팅방 설명", requiredMode = Schema.RequiredMode.REQUIRED, example = "테스트 채팅방 입니다.")
    String description;
    @Schema(description = "채팅방 주인", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    SimpleAccountDto owner;
    @Schema(description = "채팅방 개설 시간", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    LocalDateTime createdAt;
}
