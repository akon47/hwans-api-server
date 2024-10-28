package com.hwans.apiserver.dto.chat;

import com.hwans.apiserver.dto.account.SimpleAccountDto;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 채팅방 Dto
 */
@Getter
@Builder
@ApiModel(description = "채팅방 Dto")
public class ChatRoomDto implements Serializable {
    @ApiModelProperty(value = "채팅방 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "채팅방 이름", required = true, example = "테스트 채팅방")
    @NotBlank
    String title;
    @ApiModelProperty(value = "채팅방 설명", required = true, example = "테스트 채팅방 입니다.")
    String description;
    @ApiModelProperty(value = "채팅방 주인", required = true)
    @NotNull
    SimpleAccountDto owner;
    @ApiModelProperty(value = "채팅방 개설 시간", required = true)
    @NotNull
    LocalDateTime createdAt;
}
