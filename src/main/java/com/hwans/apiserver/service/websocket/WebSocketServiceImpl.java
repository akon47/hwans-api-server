package com.hwans.apiserver.service.websocket;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hwans.apiserver.common.config.ClientInfoHandshakeInterceptor;
import com.hwans.apiserver.common.security.jwt.JwtStatus;
import com.hwans.apiserver.common.security.jwt.JwtTokenProvider;
import com.hwans.apiserver.dto.notification.NotificationDto;
import com.hwans.apiserver.dto.websocket.ActiveViewerSessionDto;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.annotation.PreDestroy;

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

    // 사용자 이메일별 인증된 세션 Id 집합 (한 사용자가 여러 탭/기기로 접속 가능)
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();
    // 세션 Id별 인증된 사용자 이메일
    private final Map<String, String> sessionUser = new ConcurrentHashMap<>();
    // 사용자-세션 매핑(userSessions / sessionUser) 변경의 원자성을 보장하기 위한 락
    private final Object userLock = new Object();

    // 세션 Id별 클라이언트 정보(IP/User-Agent/접속 시각). 핸드셰이크 때 캡처한 값을 보관한다.
    private final Map<String, ClientInfo> sessionClientInfo = new ConcurrentHashMap<>();

    /**
     * 핸드셰이크 시점에 캡처한 세션의 클라이언트 정보.
     */
    private record ClientInfo(String ip, String userAgent, LocalDateTime connectedAt) {
    }

    private final JwtTokenProvider jwtTokenProvider;

    // 모든 아웃바운드 전송을 단일 스레드로 직렬화한다.
    // SockJS 세션 라이프사이클 콜백(afterConnectionEstablished/Closed 등)은 해당 세션의 내부 락을
    // 보유한 상태로 호출되므로, 그 안에서 직접 sendMessage(=다른 세션의 락 획득)를 호출하면
    // 동시 접속 시 세션 간 락 순서가 역전되어 데드락이 발생한다. 전송을 별도 스레드로 분리하면
    // 콜백 스레드는 어떤 세션 락도 잡지 않고, 전송 스레드는 항상 한 번에 한 세션 락만 잡는다.
    private final ExecutorService sendExecutor = Executors.newSingleThreadExecutor(runnable -> {
        var thread = new Thread(runnable, "websocket-send");
        thread.setDaemon(true);
        return thread;
    });

    @PreDestroy
    public void shutdownSendExecutor() {
        sendExecutor.shutdownNow();
    }

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        sessionClientInfo.put(session.getId(), extractClientInfo(session));
        notifySessionCountChanged();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        var sessionId = session.getId();
        sessions.remove(sessionId);
        sessionClientInfo.remove(sessionId);
        clearViewingPost(sessionId);
        unregisterUser(sessionId);
        notifySessionCountChanged();
    }

    private ClientInfo extractClientInfo(WebSocketSession session) {
        var attributes = session.getAttributes();
        var ip = asString(attributes.get(ClientInfoHandshakeInterceptor.CLIENT_IP), "unknown");
        var userAgent = asString(attributes.get(ClientInfoHandshakeInterceptor.USER_AGENT), "unknown");
        return new ClientInfo(ip, userAgent, LocalDateTime.now());
    }

    private String asString(Object value, String defaultValue) {
        return (value instanceof String s && !s.isBlank()) ? s : defaultValue;
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
                case AUTHENTICATE -> authenticate(session, received.getPayload());
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
            sessions.values().forEach(session -> enqueueSend(session, serialized));
        }
    }

    private void sendPostViewerCounts(WebSocketSession session, List<String> postIds) {
        var counts = postIds.stream()
                .map(postId -> new PostViewerCountDto(postId, viewerCountOf(postId)))
                .toList();
        var message = MessageDto.builder().type(MessageType.POST_VIEWER_COUNTS).payload(counts).build();
        var serialized = serialize(message);
        if (serialized != null) {
            enqueueSend(session, serialized);
        }
    }

    private void notifySessionCountChanged() {
        var message = MessageDto.builder().type(MessageType.SESSION_COUNT_CHANGED).payload(sessions.size()).build();
        var serialized = serialize(message);
        if (serialized != null) {
            sessions.values().forEach(session -> enqueueSend(session, serialized));
        }
    }

    /**
     * AUTHENTICATE 메시지로 받은 액세스 토큰을 검증하여 현재 세션을 로그인 사용자와 연결한다.
     * 토큰이 없거나 유효하지 않으면 아무 일도 하지 않는다.
     */
    private void authenticate(WebSocketSession session, Object payload) {
        if (payload == null) {
            return;
        }
        var token = payload.toString().trim();
        if (token.isEmpty() || jwtTokenProvider.validateAccessToken(token) != JwtStatus.ACCESS) {
            return;
        }
        var email = jwtTokenProvider.getAccountEmailFromAccessToken(token).orElse(null);
        if (email != null) {
            registerUser(session.getId(), email);
        }
    }

    private void registerUser(String sessionId, String email) {
        synchronized (userLock) {
            var previousEmail = sessionUser.put(sessionId, email);
            if (previousEmail != null && !previousEmail.equals(email)) {
                removeSessionFromUser(previousEmail, sessionId);
            }
            userSessions.computeIfAbsent(email, key -> new HashSet<>()).add(sessionId);
        }
    }

    private void unregisterUser(String sessionId) {
        synchronized (userLock) {
            var email = sessionUser.remove(sessionId);
            if (email != null) {
                removeSessionFromUser(email, sessionId);
            }
        }
    }

    // userLock 보유 상태에서만 호출한다.
    private void removeSessionFromUser(String email, String sessionId) {
        var sessionIds = userSessions.get(email);
        if (sessionIds != null) {
            sessionIds.remove(sessionId);
            if (sessionIds.isEmpty()) {
                userSessions.remove(email);
            }
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

    // 실제 전송을 전용 단일 스레드로 위임한다(데드락 방지). trySend는 이 스레드에서만 호출되므로
    // 어떤 호출 스레드도 세션 락을 잡지 않는다.
    private void enqueueSend(WebSocketSession session, String text) {
        try {
            sendExecutor.execute(() -> trySend(session, text));
        } catch (RejectedExecutionException e) {
            // 종료 중이라 큐에 넣지 못한 경우 무시한다.
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
    public void sendNotification(String accountEmail, NotificationDto notification) {
        if (accountEmail == null || notification == null) {
            return;
        }
        List<WebSocketSession> targets;
        synchronized (userLock) {
            var sessionIds = userSessions.get(accountEmail);
            if (sessionIds == null || sessionIds.isEmpty()) {
                return;
            }
            targets = sessionIds.stream()
                    .map(sessions::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
        var message = MessageDto.builder().type(MessageType.NOTIFICATION).payload(notification).build();
        var serialized = serialize(message);
        if (serialized != null) {
            targets.forEach(session -> enqueueSend(session, serialized));
        }
    }

    @Override
    public Map<String, Integer> getActiveViewerCounts() {
        var result = new HashMap<String, Integer>();
        synchronized (presenceLock) {
            postViewers.forEach((postId, viewers) -> {
                if (!viewers.isEmpty()) {
                    result.put(postId, viewers.size());
                }
            });
        }
        return result;
    }

    @Override
    public List<ActiveViewerSessionDto> getActiveViewerPresences() {
        // 시청 상태 스냅샷을 락 안에서 복사한 뒤, 락 밖에서 클라이언트/사용자 정보를 조합한다.
        // (sessionClientInfo / sessionUser는 ConcurrentHashMap이므로 단일 get은 락 없이 안전하다 → 락 중첩 회피)
        Map<String, String> viewingSnapshot;
        synchronized (presenceLock) {
            viewingSnapshot = new HashMap<>(sessionViewingPost);
        }
        var result = new ArrayList<ActiveViewerSessionDto>(viewingSnapshot.size());
        viewingSnapshot.forEach((sessionId, postId) -> {
            var info = sessionClientInfo.get(sessionId);
            var ip = info != null ? info.ip() : "unknown";
            var userAgent = info != null ? info.userAgent() : "unknown";
            var connectedAt = info != null ? info.connectedAt() : null;
            var email = sessionUser.get(sessionId);
            result.add(new ActiveViewerSessionDto(ip, userAgent, connectedAt, postId, email));
        });
        return result;
    }

    @Override
    public int getSessionCount() {
        return sessions.size();
    }

    @Override
    public WebSocketHandler getWebSocketHandler() {
        return this;
    }
}
