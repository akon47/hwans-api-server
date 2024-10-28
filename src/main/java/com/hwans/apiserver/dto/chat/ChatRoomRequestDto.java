package com.hwans.apiserver.dto.chat;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 채팅방 생성/수정 Dto
 */
@Getter
@ApiModel(description = "채팅방 생성/수정 Dto")
public class ChatRoomRequestDto implements Serializable {
    @ApiModelProperty(value = "채팅방 이름", required = true, example = "테스트 채팅방")
    @NotBlank
    String title;
    @ApiModelProperty(value = "채팅방 설명", required = true, example = "테스트 채팅방 입니다.")
    String description;
}
