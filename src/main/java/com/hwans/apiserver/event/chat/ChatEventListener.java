package com.hwans.apiserver.event.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatEventListener {
    @Async
    @TransactionalEventListener
    public void onCreateChatRoom(CreateChatRoomEvent event) {

    }

    @Async
    @TransactionalEventListener
    public void onCreateChatMessage(CreateChatMessageEvent event) {

    }
}
