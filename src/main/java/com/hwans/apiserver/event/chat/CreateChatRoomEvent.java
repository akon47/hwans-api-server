package com.hwans.apiserver.event.chat;

import com.hwans.apiserver.entity.chat.ChatRoom;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 채팅방 생성 이벤트
 */
@Getter
public class CreateChatRoomEvent extends ApplicationEvent {
    private final ChatRoom chatRoom;

    public CreateChatRoomEvent(Object source, ChatRoom chatRoom) {
        super(source);
        this.chatRoom = chatRoom;
    }
}
