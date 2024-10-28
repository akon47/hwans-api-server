package com.hwans.apiserver.repository.chat;

import com.hwans.apiserver.entity.chat.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {
}