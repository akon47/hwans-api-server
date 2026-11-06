package com.hwans.apiserver.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 채팅방 생성/수정 Dto
 */
@Getter
@Schema(description = "채팅방 생성/수정 Dto")
public class ChatRoomRequestDto implements Serializable {
    @Schema(description = "채팅방 이름", requiredMode = Schema.RequiredMode.REQUIRED, example = "테스트 채팅방")
    @NotBlank
    String title;
    @Schema(description = "채팅방 설명", requiredMode = Schema.RequiredMode.REQUIRED, example = "테스트 채팅방 입니다.")
    String description;
}
