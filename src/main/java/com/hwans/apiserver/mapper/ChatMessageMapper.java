package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.chat.ChatMessageDto;
import com.hwans.apiserver.dto.chat.ChatRoomDto;
import com.hwans.apiserver.dto.chat.ChatRoomRequestDto;
import com.hwans.apiserver.entity.chat.ChatMessage;
import com.hwans.apiserver.entity.chat.ChatRoom;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class ChatMessageMapper {
    @org.mapstruct.Mapping(source = "author", target = "owner")
    public abstract ChatMessageDto entityToDto(ChatMessage chatMessage);
}
