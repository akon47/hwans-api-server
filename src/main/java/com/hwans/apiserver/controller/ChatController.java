package com.hwans.apiserver.controller;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.errors.errorcode.ErrorCodes;
import com.hwans.apiserver.common.errors.exception.RestApiException;
import com.hwans.apiserver.dto.chat.ChatMessageDto;
import com.hwans.apiserver.dto.chat.ChatMessageRequestDto;
import com.hwans.apiserver.dto.common.SliceDto;
import com.hwans.apiserver.service.account.AccountService;
import com.hwans.apiserver.service.authentication.CurrentAuthenticationDetails;
import com.hwans.apiserver.service.authentication.UserAuthenticationDetails;
import com.hwans.apiserver.service.chat.ChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;

/**
 * 채팅 Controller
 */
@RestController
@Api(tags = "채팅")
@RequestMapping(value = Constants.API_PREFIX)
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;
    private final AccountService accountService;

    @ApiOperation(value = "채팅 메시지 조회", notes = "채팅방 메시지를 조회한다.", tags = "채팅")
    @GetMapping(value = "/chat/rooms/{chatRoomId}/messages")
    public SliceDto<ChatMessageDto> getMessages(@ApiParam(value = "채팅방 Id") @PathVariable UUID chatRoomId,
                                                @ApiParam(value = "페이징 조회를 위한 CursorId") @RequestParam(required = false) Optional<UUID> cursorId,
                                                @ApiParam(value = "조회할 최대 페이지 수") @RequestParam(required = false, defaultValue = "20") int size) {
        return chatService.getChatMessages(chatRoomId, cursorId, size);
    }

    @ApiOperation(value = "채팅 메시지 생성", notes = "채팅방에 메시지를 작성한다.", tags = "채팅")
    @PostMapping(value = "/chat/rooms/{chatRoomId}/messages")
    public ChatMessageDto createMessage(@CurrentAuthenticationDetails UserAuthenticationDetails userAuthenticationDetails,
                                        @ApiParam(value = "채팅방 Id") @PathVariable UUID chatRoomId,
                                        @ApiParam(value = "메시지 데이터", required = true) @RequestBody @Valid final ChatMessageRequestDto chatMessageRequestDto) {
        var accountId = userAuthenticationDetails.getId();
        var account = accountService.getAccount(accountId);
        if (account.isGuest()) {
            throw new RestApiException(ErrorCodes.Forbidden.FORBIDDEN);
        }

        return chatService.createChatMessage(accountId, chatRoomId, chatMessageRequestDto);
    }
}
