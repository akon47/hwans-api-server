package com.hwans.apiserver.common.config;

import com.hwans.apiserver.service.blog.BlogWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@RequiredArgsConstructor
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final BlogWebSocketHandler blogWebSocketHandler;

    @Value("${allowedOrigins}")
    private String[] allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(blogWebSocketHandler, "/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }
}
