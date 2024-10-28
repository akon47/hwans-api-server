package com.hwans.apiserver.service.chat;

import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.chat.ChatMessageDto;
import com.hwans.apiserver.dto.chat.ChatMessageRequestDto;
import com.hwans.apiserver.dto.chat.ChatRoomDto;
import com.hwans.apiserver.dto.chat.ChatRoomRequestDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.entity.chat.ChatMessage;
import com.hwans.apiserver.event.chat.CreateChatMessageEvent;
import com.hwans.apiserver.event.chat.CreateChatRoomEvent;
import com.hwans.apiserver.mapper.ChatMessageMapper;
import com.hwans.apiserver.mapper.ChatRoomMapper;
import com.hwans.apiserver.repository.account.AccountRepository;
import com.hwans.apiserver.repository.chat.ChatMessageRepository;
import com.hwans.apiserver.repository.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AccountRepository accountRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 새로운 채팅방을 생성한다.
     *
     * @param chatRoomRequestDto 채팅방 생성 요청 모델
     * @return 생성된 채팅방
     */
    @Override
    @Transactional
    public ChatRoomDto createChatRoom(ChatRoomRequestDto chatRoomRequestDto) {
        var savedChatRoom = chatRoomRepository.save(chatRoomMapper.toEntity(chatRoomRequestDto));
        eventPublisher.publishEvent(new CreateChatRoomEvent(this, savedChatRoom));
        return chatRoomMapper.entityToDto(savedChatRoom);
    }

    /**
     * 새로운 메시지를 생성한다.
     *
     * @param authorAccountId       작성자 계정 Id
     * @param chatRoomId            채팅방 Id
     * @param chatMessageRequestDto 메시지 요청 모델
     * @return 생성된 메시지 모델
     */
    @Override
    @Transactional
    public ChatMessageDto createChatMessage(UUID authorAccountId, UUID chatRoomId, ChatMessageRequestDto chatMessageRequestDto) {
        var authorAccount = accountRepository
                .findById(authorAccountId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        var chatRoom = chatRoomRepository
                .findById(chatRoomId)
                .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));

        var savedMessage = chatMessageRepository.save(ChatMessage
                .builder()
                .content(chatMessageRequestDto.getContent())
                .author(authorAccount)
                .chatRoom(chatRoom)
                .build());

        eventPublisher.publishEvent(new CreateChatMessageEvent(this, savedMessage));
        return chatMessageMapper.entityToDto(savedMessage);
    }

    /**
     * 채팅방의 메시지 목록을 반환한다.
     *
     * @param chatRoomId 채팅방 Id
     * @param cursorId   페이징 조회를 위한 기준 cursorId
     * @param size       조회를 원하는 최대 size
     * @return 조회된 메시지 목록 (페이징)
     */
    @Override
    public SliceDto<ChatMessageDto> getChatMessages(UUID chatRoomId, Optional<UUID> cursorId, int size) {
        List<ChatMessage> foundMessages;
        if (cursorId.isPresent()) {
            var foundCursorMessage = chatMessageRepository
                    .findById(cursorId.get())
                    .orElseThrow(() -> new RestApiException(ErrorCodes.NotFound.NOT_FOUND));
            foundMessages = chatMessageRepository.findByIdLessThanOrderByIdDesc(chatRoomId, foundCursorMessage.getId(), foundCursorMessage.getCreatedAt(), PageRequest.of(0, size + 1));
        } else {
            foundMessages = chatMessageRepository.findAllByOrderByIdDesc(chatRoomId, PageRequest.of(0, size + 1));
        }

        var last = foundMessages.size() <= size;
        return SliceDto.<ChatMessageDto>builder()
                .data(foundMessages.stream().limit(size).map(chatMessageMapper::entityToDto).toList())
                .size((int) foundMessages.stream().limit(size).count())
                .empty(foundMessages.isEmpty())
                .first(cursorId.isEmpty())
                .last(last)
                .cursorId(last ? null : foundMessages.stream().limit(size).skip(size - 1).findFirst().map(ChatMessage::getId).orElse(null))
                .build();
    }
}
