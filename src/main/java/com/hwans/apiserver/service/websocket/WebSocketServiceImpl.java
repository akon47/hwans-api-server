package com.hwans.apiserver.service.websocket;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hwans.apiserver.dto.websocket.MessageDto;
import com.hwans.apiserver.dto.websocket.MessageType;
import com.hwans.apiserver.dto.websocket.PostViewerCountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketServiceImpl extends TextWebSocketHandler implements WebSocketService {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    // 세션이 현재 보고 있는 게시글 Id (게시글 상세 페이지 시청 중)
    private final Map<String, String> sessionViewingPost = new ConcurrentHashMap<>();
    // 게시글 Id별 현재 시청 중인 세션 Id 집합
    private final Map<String, Set<String>> postViewers = new ConcurrentHashMap<>();
    // 시청자 상태(sessionViewingPost / postViewers) 변경의 원자성을 보장하기 위한 락
    private final Object presenceLock = new Object();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        notifySessionCountChanged();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var sessionId = session.getId();
        sessions.remove(sessionId);
        clearViewingPost(sessionId);
        notifySessionCountChanged();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            var received = objectMapper.readValue(message.getPayload(), MessageDto.class);
            if (received.getType() == null) {
                return;
            }
            switch (received.getType()) {
                case VIEW_POST -> updateViewingPost(session.getId(), asPostId(received.getPayload()));
                case WATCH_POSTS -> sendPostViewerCounts(session, asPostIdList(received.getPayload()));
                default -> {
                    // 그 외 타입은 서버 -> 클라이언트 전용이므로 무시한다.
                }
            }
        } catch (Exception e) {
            log.info("handleTextMessage failed");
            log.trace("handleTextMessage failed trace: ", e);
        }
    }

    private String asPostId(Object payload) {
        if (payload == null) {
            return null;
        }
        var postId = payload.toString().trim();
        return postId.isEmpty() ? null : postId;
    }

    private List<String> asPostIdList(Object payload) {
        if (payload instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::asPostId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    /**
     * 세션이 현재 보고 있는 게시글을 갱신하고, 영향을 받은 게시글들의 시청자 수를 브로드캐스트한다.
     */
    private void updateViewingPost(String sessionId, String newPostId) {
        List<String> affectedPostIds = new ArrayList<>();
        synchronized (presenceLock) {
            var previousPostId = sessionViewingPost.get(sessionId);
            if (Objects.equals(previousPostId, newPostId)) {
                return;
            }
            if (previousPostId != null) {
                removeViewer(previousPostId, sessionId);
                affectedPostIds.add(previousPostId);
            }
            if (newPostId != null) {
                sessionViewingPost.put(sessionId, newPostId);
                postViewers.computeIfAbsent(newPostId, key -> new HashSet<>()).add(sessionId);
                affectedPostIds.add(newPostId);
            } else {
                sessionViewingPost.remove(sessionId);
            }
        }
        affectedPostIds.forEach(this::broadcastPostViewerCount);
    }

    /**
     * 세션이 연결 종료될 때 시청 중이던 게시글에서 제거한다.
     */
    private void clearViewingPost(String sessionId) {
        String previousPostId;
        synchronized (presenceLock) {
            previousPostId = sessionViewingPost.remove(sessionId);
            if (previousPostId != null) {
                removeViewer(previousPostId, sessionId);
            }
        }
        if (previousPostId != null) {
            broadcastPostViewerCount(previousPostId);
        }
    }

    // presenceLock 보유 상태에서만 호출한다.
    private void removeViewer(String postId, String sessionId) {
        var viewers = postViewers.get(postId);
        if (viewers != null) {
            viewers.remove(sessionId);
            if (viewers.isEmpty()) {
                postViewers.remove(postId);
            }
        }
    }

    private int viewerCountOf(String postId) {
        var viewers = postViewers.get(postId);
        return viewers == null ? 0 : viewers.size();
    }

    private void broadcastPostViewerCount(String postId) {
        var message = MessageDto.builder()
                .type(MessageType.POST_VIEWER_COUNT_CHANGED)
                .payload(new PostViewerCountDto(postId, viewerCountOf(postId)))
                .build();
        var serialized = serialize(message);
        if (serialized != null) {
            sessions.values().forEach(session -> trySend(session, serialized));
        }
    }

    private void sendPostViewerCounts(WebSocketSession session, List<String> postIds) {
        var counts = postIds.stream()
                .map(postId -> new PostViewerCountDto(postId, viewerCountOf(postId)))
                .toList();
        var message = MessageDto.builder().type(MessageType.POST_VIEWER_COUNTS).payload(counts).build();
        var serialized = serialize(message);
        if (serialized != null) {
            trySend(session, serialized);
        }
    }

    private void notifySessionCountChanged() {
        var message = MessageDto.builder().type(MessageType.SESSION_COUNT_CHANGED).payload(sessions.size()).build();
        var serialized = serialize(message);
        if (serialized != null) {
            sessions.values().forEach(session -> trySend(session, serialized));
        }
    }

    private String serialize(MessageDto message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.info("websocket message serialize failed");
            return null;
        }
    }

    private void trySend(WebSocketSession session, String text) {
        try {
            if (session.isOpen()) {
                // WebSocketSession#sendMessage는 동시 호출에 안전하지 않으므로 세션 단위로 동기화한다.
                synchronized (session) {
                    session.sendMessage(new TextMessage(text));
                }
            }
        } catch (Exception e) {
            log.trace("websocket send failed trace: ", e);
        }
    }

    @Override
    public WebSocketHandler getWebSocketHandler() {
        return this;
    }
}
