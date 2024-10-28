package com.hwans.apiserver.event.chat;

import com.hwans.apiserver.entity.chat.ChatMessage;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 채팅 메시지 생성 이벤트
 */
@Getter
public class CreateChatMessageEvent extends ApplicationEvent {
    private final ChatMessage chatMessage;

    public CreateChatMessageEvent(Object source, ChatMessage chatMessage) {
        super(source);
        this.chatMessage = chatMessage;
    }
}
