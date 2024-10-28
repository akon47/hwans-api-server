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
 * 채팅 메시지 Dto
 */
@Getter
@Builder
@ApiModel(description = "채팅 메시지 Dto")
public class ChatMessageDto implements Serializable {
    @ApiModelProperty(value = "메시지 Id", required = true)
    @NotNull
    UUID id;
    @ApiModelProperty(value = "메시지 내용", required = true, example = "안녕히세요")
    @NotBlank
    String content;
    @ApiModelProperty(value = "메시지 주인", required = true)
    @NotNull
    SimpleAccountDto owner;
    @ApiModelProperty(value = "메시지 생성 시간", required = true)
    @NotNull
    LocalDateTime createdAt;
}
