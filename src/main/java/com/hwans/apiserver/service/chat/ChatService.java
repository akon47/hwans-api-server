package com.hwans.apiserver.service.chat;

import com.hwans.apiserver.dto.chat.ChatMessageDto;
import com.hwans.apiserver.dto.chat.ChatMessageRequestDto;
import com.hwans.apiserver.dto.chat.ChatRoomDto;
import com.hwans.apiserver.dto.chat.ChatRoomRequestDto;
import com.hwans.apiserver.dto.common.SliceDto;

import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 서비스 인터페이스
 */
public interface ChatService {

    /**
     * 새로운 채팅방을 생성한다.
     *
     * @param chatRoomRequestDto 채팅방 생성 요청 모델
     * @return 생성된 채팅방
     */
    ChatRoomDto createChatRoom(ChatRoomRequestDto chatRoomRequestDto);

    /**
     * 새로운 메시지를 생성한다.
     *
     * @param authorAccountId       작성자 계정 Id
     * @param chatRoomId            채팅방 Id
     * @param chatMessageRequestDto 메시지 요청 모델
     * @return 생성된 메시지 모델
     */
    ChatMessageDto createChatMessage(UUID authorAccountId, UUID chatRoomId, ChatMessageRequestDto chatMessageRequestDto);

    /**
     * 채팅방의 메시지 목록을 반환한다.
     *
     * @param chatRoomId 채팅방 Id
     * @param cursorId   페이징 조회를 위한 기준 cursorId
     * @param size       조회를 원하는 최대 size
     * @return 조회된 메시지 목록 (페이징)
     */
    SliceDto<ChatMessageDto> getChatMessages(UUID chatRoomId, Optional<UUID> cursorId, int size);
}
