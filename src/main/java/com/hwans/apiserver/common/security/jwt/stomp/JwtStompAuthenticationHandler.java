package com.hwans.apiserver.common.security.jwt.stomp;

import com.hwans.apiserver.common.Constants;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class JwtStompAuthenticationHandler implements ChannelInterceptor {
    private final JwtTokenProvider tokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearerToken = accessor.getFirstNativeHeader(Constants.AUTHORIZATION_HEADER);
            String jwt = tokenProvider.extractTokenFromHeader(bearerToken);
            if (StringUtils.hasText(jwt)) {
                var authentication = tokenProvider.getAuthentication(jwt);
                // TODO: STOMP 채널 연결에 대한 권한 처리
                return message;
            }
        }
        return message;
    }
}
