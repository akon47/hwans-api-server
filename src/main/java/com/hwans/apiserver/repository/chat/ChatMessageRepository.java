package com.hwans.apiserver.repository.chat;

import com.hwans.apiserver.entity.chat.ChatMessage;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    @Query("select x from ChatMessage as x where x.deleted = false and x.chatRoom.id = :chatRoomId order by x.createdAt desc, x.id desc")
    List<ChatMessage> findAllByOrderByIdDesc(@Param("chatRoomId") UUID chatRoomId, Pageable page);

    @Query("select x from ChatMessage as x where x.deleted = false and x.chatRoom.id = :chatRoomId and ((x.createdAt < :createdAt and x.id < :id) or (x.createdAt < :createdAt)) order by x.createdAt desc, x.id desc")
    List<ChatMessage> findByIdLessThanOrderByIdDesc(@Param("chatRoomId") UUID chatRoomId, @Param("uuid") UUID id, @Param("createdAt") LocalDateTime createdAt, Pageable page);
}