package com.hwans.apiserver.mapper;

import com.hwans.apiserver.dto.chat.ChatRoomDto;
import com.hwans.apiserver.dto.chat.ChatRoomRequestDto;
import com.hwans.apiserver.entity.chat.ChatRoom;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class ChatRoomMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    public abstract ChatRoom toEntity(ChatRoomRequestDto createChannelRequestDto);

    public abstract ChatRoomDto entityToDto(ChatRoom chatRoom);
}
