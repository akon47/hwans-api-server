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
import com.hwans.apiserver.dto.websocket.CursorBatchDto;
import com.hwans.apiserver.dto.websocket.CursorStateDto;
import com.hwans.apiserver.dto.websocket.MessageDto;
import com.hwans.apiserver.dto.websocket.MessageType;
import com.hwans.apiserver.dto.websocket.PostViewerCountDto;
import com.hwans.apiserver.dto.websocket.PostViewerDto;
import com.hwans.apiserver.dto.websocket.PostViewersDto;
import com.hwans.apiserver.dto.websocket.ReadPositionsDto;
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
import java.util.Comparator;
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
import jakarta.annotation.PreDestroy;

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

    // 세션 Id별 마지막 생존 신호(인바운드 메시지 수신) 시각(epoch millis).
    // 네트워크 전환(예: Wi-Fi -> LTE)이나 비정상 종료로 half-open 상태가 된 세션은
    // close 프레임이 도달하지 않아 afterConnectionClosed가 호출되지 않는다.
    // 리퍼(reapStaleSessions)가 이 값을 기준으로 죽은 세션을 수거한다.
    private final Map<String, Long> sessionLastSeen = new ConcurrentHashMap<>();

    // 마지막 생존 신호 이후 이 시간이 지나면 죽은 세션으로 보고 수거한다.
    // 클라이언트는 25초 주기로 PING을 보내므로, 75초는 핑 약 3회 누락에 해당한다.
    private static final long STALE_SESSION_TIMEOUT_MILLIS = 75_000L;

    // ───────────── 함께 읽는 사람 커서 ─────────────
    // 글 Id -> (세션 Id -> 커서 상태). 움직일 때마다 바로 중계하지 않고 flushCursors 가 약 100ms 마다 묶어 보낸다.
    private final Map<String, Map<String, CursorStateDto>> postCursors = new ConcurrentHashMap<>();
    // 다음 flush 때 보낼 글 Id
    private final Set<String> dirtyCursorPosts = ConcurrentHashMap.newKeySet();
    // 글 Id -> 다음 flush 때 "떠났다"고 알릴 세션 Id
    private final Map<String, Set<String>> leftCursors = new ConcurrentHashMap<>();
    // 세션별 마지막 커서 이동/말 수신 시각 (너무 잦은 전송은 버린다)
    private final Map<String, Long> cursorLastMoveAt = new ConcurrentHashMap<>();
    private final Map<String, Long> cursorLastChatAt = new ConcurrentHashMap<>();
    // 자기 세션 Id(CURSOR_SELF)를 이미 알려준 세션, 커서 공유가 꺼졌다고 이미 알려준 세션
    private final Set<String> cursorSelfSent = ConcurrentHashMap.newKeySet();
    private final Set<String> cursorDisabledNotified = ConcurrentHashMap.newKeySet();
    // 같은 글을 읽는 사람이 이보다 많으면 그 글의 커서 공유를 끈다 (전송량 = 읽는 사람 수 × 커서 수 × 10/초)
    private static final int MAX_CURSOR_VIEWERS = 120;
    // 한 묶음에 담는 커서 최대 개수 (최근에 움직인 순)
    private static final int MAX_CURSORS_PER_BATCH = 40;
    // 클라이언트는 이동 50ms·말 120ms 간격으로 보내되 마지막 값은 반드시 뒤따라 보낸다. 서버 쪽 간격은 스팸 방지용으로만 둔다.
    private static final long CURSOR_MOVE_MIN_INTERVAL_MILLIS = 25L;
    private static final long CURSOR_CHAT_MIN_INTERVAL_MILLIS = 30L;
    // 이 시간 동안 안 움직인 커서는 떠난 것으로 본다
    private static final long CURSOR_STALE_MILLIS = 90_000L;
    private static final int CURSOR_NAME_MAX = 24;
    private static final int CURSOR_MESSAGE_MAX = 120;

    // ───────────── 함께 읽는 사람 목록 ─────────────
    // 세션 Id -> 읽는 사람 목록에 보일 이름·색·글에 들어온 시각. 글을 보는 동안만 보관한다.
    private final Map<String, ViewerIdentity> sessionViewerIdentity = new ConcurrentHashMap<>();
    // 한 번에 알려 주는 읽는 사람 최대 수 (먼저 들어온 순). 나머지는 total 로만 알린다.
    private static final int MAX_LISTED_VIEWERS = 30;
    private static final String DEFAULT_VIEWER_NAME = "익명";
    private static final String DEFAULT_VIEWER_COLOR = "#3b5bdb";

    private record ViewerIdentity(String name, String color, long joinedAt) {
    }

    // ───────────── 읽는 위치 ─────────────
    // 세션 Id -> 지금 보는 글에서 읽고 있는 위치(본문 시작 0 ~ 끝 1). 글을 보는 동안만 보관한다.
    private final Map<String, Double> sessionReadPosition = new ConcurrentHashMap<>();
    private final Map<String, Long> readPositionLastAt = new ConcurrentHashMap<>();
    // 다음 flush 때 읽는 위치를 보낼 글 Id
    private final Set<String> dirtyReadPosts = ConcurrentHashMap.newKeySet();
    private volatile long readPositionsFlushedAt = 0L;
    // 클라이언트는 약 400ms 간격으로 보낸다. 서버 쪽 간격은 스팸 방지용.
    private static final long READ_POSITION_MIN_INTERVAL_MILLIS = 100L;
    private static final long READ_POSITIONS_FLUSH_INTERVAL_MILLIS = 300L;

    // ───────────── 떠오르는 이모지 · 댓글 작성 중 ─────────────
    // 세션별 마지막 전송 시각 (너무 잦은 전송은 버린다)
    private final Map<String, Long> reactionBurstLastAt = new ConcurrentHashMap<>();
    private final Map<String, Long> commentTypingLastAt = new ConcurrentHashMap<>();
    private static final long REACTION_BURST_MIN_INTERVAL_MILLIS = 250L;
    private static final long COMMENT_TYPING_MIN_INTERVAL_MILLIS = 1_000L;

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
        sessionLastSeen.put(session.getId(), System.currentTimeMillis());
        notifySessionCountChanged();
        sendActiveViewerCounts(session);
    }

    /**
     * 방금 연결된 세션에 지금 읽히고 있는 글들의 시청자 수를 한 번에 알려 준다.
     * 변경 브로드캐스트는 연결 이후의 변화만 전달하므로, 이 스냅샷이 없으면 새로 들어온 사람은
     * 누군가 글을 열거나 닫기 전까지 "지금 보는 중" 숫자를 알 수 없다.
     */
    private void sendActiveViewerCounts(WebSocketSession session) {
        var counts = getActiveViewerCounts().entrySet().stream()
                .map(entry -> new PostViewerCountDto(entry.getKey(), entry.getValue()))
                .toList();
        if (counts.isEmpty()) {
            return;
        }
        var message = MessageDto.builder().type(MessageType.POST_VIEWER_COUNTS).payload(counts).build();
        var serialized = serialize(message);
        if (serialized != null) {
            enqueueSend(session, serialized);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session.getId());
    }

    /**
     * 세션과 관련된 모든 상태를 제거하고 전체 세션 수 변경을 브로드캐스트한다.
     * 정상 종료(afterConnectionClosed)와 리퍼(reapStaleSessions) 양쪽에서 호출되며,
     * 각 맵 제거/clearViewingPost/unregisterUser는 모두 멱등하므로 중복 호출되어도 안전하다.
     */
    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        sessionClientInfo.remove(sessionId);
        sessionLastSeen.remove(sessionId);
        clearViewingPost(sessionId);
        unregisterUser(sessionId);
        cursorLastMoveAt.remove(sessionId);
        cursorLastChatAt.remove(sessionId);
        cursorSelfSent.remove(sessionId);
        cursorDisabledNotified.remove(sessionId);
        readPositionLastAt.remove(sessionId);
        reactionBurstLastAt.remove(sessionId);
        commentTypingLastAt.remove(sessionId);
        notifySessionCountChanged();
    }

    /**
     * 마지막 생존 신호가 타임아웃을 넘긴 세션을 닫고 수거한다. 스케줄러가 주기적으로 호출한다.
     * half-open 상태로 남은 유령 세션(네트워크 전환/비정상 종료)을 정리하는 유일한 경로다.
     */
    @Override
    public void reapStaleSessions() {
        var deadline = System.currentTimeMillis() - STALE_SESSION_TIMEOUT_MILLIS;
        var staleSessionIds = sessionLastSeen.entrySet().stream()
                .filter(entry -> entry.getValue() < deadline)
                .map(Map.Entry::getKey)
                .toList();
        for (var sessionId : staleSessionIds) {
            var session = sessions.get(sessionId);
            if (session != null) {
                try {
                    // half-open 소켓의 close는 FIN만 보내고 즉시 반환하므로 블로킹되지 않는다.
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                } catch (Exception e) {
                    log.trace("stale websocket session close failed trace: ", e);
                }
            }
            cleanupSession(sessionId);
        }
        if (!staleSessionIds.isEmpty()) {
            log.info("reaped {} stale websocket session(s)", staleSessionIds.size());
        }
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
            // 어떤 메시지든 도착했다는 건 세션이 살아 있다는 뜻이므로 생존 시각을 갱신한다.
            sessionLastSeen.put(session.getId(), System.currentTimeMillis());
            var received = objectMapper.readValue(message.getPayload(), MessageDto.class);
            if (received.getType() == null) {
                return;
            }
            switch (received.getType()) {
                case VIEW_POST -> handleViewPost(session, received.getPayload());
                case WATCH_POSTS -> sendPostViewerCounts(session, asPostIdList(received.getPayload()));
                case AUTHENTICATE -> authenticate(session, received.getPayload());
                case CURSOR_MOVE -> handleCursorMove(session, received.getPayload());
                case CURSOR_CHAT -> handleCursorChat(session, received.getPayload());
                case CURSOR_LEAVE -> handleCursorLeave(session);
                case READ_POSITION -> handleReadPosition(session, received.getPayload());
                case REACTION_BURST -> handleReactionBurst(session, received.getPayload());
                case COMMENT_TYPING -> handleCommentTyping(session);
                case PING -> {
                    // 생존 신호. lastSeen은 위에서 이미 갱신했으므로 추가 처리는 없다.
                }
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
     * VIEW_POST 를 처리한다. payload 는 게시글 Id 문자열(예전 클라이언트)이거나 {postId, name, color} 다.
     */
    private void handleViewPost(WebSocketSession session, Object payload) {
        String postId;
        var name = DEFAULT_VIEWER_NAME;
        var color = DEFAULT_VIEWER_COLOR;
        if (payload instanceof Map<?, ?> map) {
            postId = asPostId(map.get("postId"));
            var requestedName = truncate(asString(map.get("name"), DEFAULT_VIEWER_NAME).strip(), CURSOR_NAME_MAX);
            name = requestedName.isEmpty() ? DEFAULT_VIEWER_NAME : requestedName;
            color = sanitizeColor(asString(map.get("color"), DEFAULT_VIEWER_COLOR));
        } else {
            postId = asPostId(payload);
        }
        if (postId != null && cursorSelfSent.add(session.getId())) {
            // 읽는 사람 목록에서 자기 자신을 알아볼 수 있도록 세션 Id 를 먼저 알려 준다.
            var serialized = serialize(MessageDto.builder().type(MessageType.CURSOR_SELF).payload(session.getId()).build());
            if (serialized != null) {
                enqueueSend(session, serialized);
            }
        }
        updateViewingPost(session.getId(), postId, name, color);
    }

    /**
     * 세션이 현재 보고 있는 게시글을 갱신하고, 영향을 받은 게시글들의 시청자 수(전체에게)와
     * 읽는 사람 목록(그 글을 읽는 세션들에게)을 알린다.
     */
    private void updateViewingPost(String sessionId, String newPostId, String name, String color) {
        List<String> affectedPostIds = new ArrayList<>();
        var identityChanged = false;
        synchronized (presenceLock) {
            var previousPostId = sessionViewingPost.get(sessionId);
            if (Objects.equals(previousPostId, newPostId)) {
                // 같은 글을 계속 보는 중: 이름·색이 바뀌었을 때만(예: 보던 중 로그인) 목록을 다시 알린다.
                var identity = sessionViewerIdentity.get(sessionId);
                if (newPostId == null || (identity != null && identity.name().equals(name) && identity.color().equals(color))) {
                    return;
                }
                var joinedAt = identity != null ? identity.joinedAt() : System.currentTimeMillis();
                sessionViewerIdentity.put(sessionId, new ViewerIdentity(name, color, joinedAt));
                identityChanged = true;
            } else {
                sessionReadPosition.remove(sessionId);
                if (previousPostId != null) {
                    removeViewer(previousPostId, sessionId);
                    affectedPostIds.add(previousPostId);
                }
                if (newPostId != null) {
                    // 새로 들어온 사람이 다른 사람들의 읽는 위치를 받도록 다음 flush 대상에 넣는다.
                    dirtyReadPosts.add(newPostId);
                    sessionViewingPost.put(sessionId, newPostId);
                    sessionViewerIdentity.put(sessionId, new ViewerIdentity(name, color, System.currentTimeMillis()));
                    postViewers.computeIfAbsent(newPostId, key -> new HashSet<>()).add(sessionId);
                    affectedPostIds.add(newPostId);
                } else {
                    sessionViewingPost.remove(sessionId);
                    sessionViewerIdentity.remove(sessionId);
                }
            }
        }
        if (identityChanged) {
            broadcastPostViewers(newPostId);
            return;
        }
        affectedPostIds.forEach(this::broadcastPostViewerCount);
        affectedPostIds.forEach(this::broadcastPostViewers);
    }

    /**
     * 한 글을 보고 있는 세션들에게 메시지를 보낸다. excludeSessionId 가 있으면 그 세션은 뺀다.
     */
    private void sendToPostViewers(String postId, MessageType type, Object payload, String excludeSessionId) {
        List<String> viewerIds;
        synchronized (presenceLock) {
            var viewers = postViewers.get(postId);
            if (viewers == null || viewers.isEmpty()) {
                return;
            }
            viewerIds = new ArrayList<>(viewers);
        }
        var serialized = serialize(MessageDto.builder().type(type).payload(payload).build());
        if (serialized == null) {
            return;
        }
        for (var viewerId : viewerIds) {
            if (viewerId.equals(excludeSessionId)) {
                continue;
            }
            var target = sessions.get(viewerId);
            if (target != null) {
                enqueueSend(target, serialized);
            }
        }
    }

    // 세션의 직전 전송 이후 최소 간격이 지났으면 true 를 돌려주고 시각을 갱신한다.
    private boolean passesInterval(Map<String, Long> lastAtBySession, String sessionId, long minIntervalMillis) {
        var now = System.currentTimeMillis();
        var last = lastAtBySession.get(sessionId);
        if (last != null && now - last < minIntervalMillis) {
            return false;
        }
        lastAtBySession.put(sessionId, now);
        return true;
    }

    /**
     * 글 반응 이모지를 방금 눌렀다는 신호를 같은 글을 읽는 모두에게(보낸 사람 포함) 전한다. 저장하지 않는다.
     */
    private void handleReactionBurst(WebSocketSession session, Object payload) {
        var sessionId = session.getId();
        var emoji = payload == null ? null : payload.toString();
        if (!com.hwans.apiserver.service.blog.BlogServiceImpl.isAllowedReactionEmoji(emoji)) {
            return;
        }
        var postId = sessionViewingPost.get(sessionId);
        if (postId == null || !passesInterval(reactionBurstLastAt, sessionId, REACTION_BURST_MIN_INTERVAL_MILLIS)) {
            return;
        }
        sendToPostViewers(postId, MessageType.REACTION_BURST, Map.of("postId", postId, "emoji", emoji, "id", sessionId), null);
    }

    /**
     * 댓글을 쓰는 중이라는 신호를 같은 글을 읽는 다른 사람들에게 전한다.
     */
    private void handleCommentTyping(WebSocketSession session) {
        var sessionId = session.getId();
        var postId = sessionViewingPost.get(sessionId);
        if (postId == null || !passesInterval(commentTypingLastAt, sessionId, COMMENT_TYPING_MIN_INTERVAL_MILLIS)) {
            return;
        }
        var identity = sessionViewerIdentity.get(sessionId);
        var name = identity != null ? identity.name() : DEFAULT_VIEWER_NAME;
        sendToPostViewers(postId, MessageType.COMMENT_TYPING, Map.of("postId", postId, "id", sessionId, "name", name), sessionId);
    }

    @Override
    public void notifyCommentsChanged(java.util.UUID postId) {
        if (postId == null) {
            return;
        }
        var id = postId.toString();
        sendToPostViewers(id, MessageType.COMMENTS_CHANGED, Map.of("postId", id), null);
    }

    // 읽는 사람 목록에 실리는 세션 Id (먼저 들어온 순, 최대 개수 제한). presenceLock 보유 상태에서만 호출한다.
    private List<String> listedViewerIds(Collection<String> viewerIds) {
        return viewerIds.stream()
                .filter(sessionViewerIdentity::containsKey)
                .sorted(Comparator.comparingLong((String id) -> sessionViewerIdentity.get(id).joinedAt())
                        .thenComparing(Comparator.naturalOrder()))
                .limit(MAX_LISTED_VIEWERS)
                .toList();
    }

    /**
     * 클라이언트가 보낸 읽는 위치를 보관한다. 실제 전송은 flushReadPositions 가 한다.
     */
    private void handleReadPosition(WebSocketSession session, Object payload) {
        var sessionId = session.getId();
        var now = System.currentTimeMillis();
        var last = readPositionLastAt.get(sessionId);
        if (last != null && now - last < READ_POSITION_MIN_INTERVAL_MILLIS) {
            return;
        }
        readPositionLastAt.put(sessionId, now);
        var postId = sessionViewingPost.get(sessionId);
        if (postId == null) {
            return;
        }
        sessionReadPosition.put(sessionId, clamp(asDouble(payload), 0, 1));
        dirtyReadPosts.add(postId);
    }

    @Override
    public void flushReadPositions() {
        if (dirtyReadPosts.isEmpty()) {
            return;
        }
        var now = System.currentTimeMillis();
        if (now - readPositionsFlushedAt < READ_POSITIONS_FLUSH_INTERVAL_MILLIS) {
            return;
        }
        readPositionsFlushedAt = now;
        for (var postId : new ArrayList<>(dirtyReadPosts)) {
            dirtyReadPosts.remove(postId);

            List<String> viewerIds;
            List<ReadPositionsDto.Position> positions;
            synchronized (presenceLock) {
                var viewers = postViewers.get(postId);
                // 혼자 읽는 중이면 알릴 사람이 없고, 사람이 너무 많은 글은 커서와 마찬가지로 보내지 않는다.
                if (viewers == null || viewers.size() < 2 || viewers.size() > MAX_CURSOR_VIEWERS) {
                    continue;
                }
                viewerIds = new ArrayList<>(viewers);
                positions = listedViewerIds(viewerIds).stream()
                        .filter(sessionReadPosition::containsKey)
                        .map(id -> new ReadPositionsDto.Position(id, sessionReadPosition.get(id)))
                        .toList();
            }
            if (positions.isEmpty()) {
                continue;
            }
            var serialized = serialize(MessageDto.builder().type(MessageType.READ_POSITIONS)
                    .payload(new ReadPositionsDto(postId, positions)).build());
            if (serialized == null) {
                continue;
            }
            for (var viewerId : viewerIds) {
                var target = sessions.get(viewerId);
                if (target != null) {
                    enqueueSend(target, serialized);
                }
            }
        }
    }

    /**
     * 한 글을 읽고 있는 세션들에게 지금 함께 읽는 사람 목록을 보낸다.
     */
    private void broadcastPostViewers(String postId) {
        List<String> viewerIds;
        List<PostViewerDto> listed;
        synchronized (presenceLock) {
            var viewers = postViewers.get(postId);
            if (viewers == null || viewers.isEmpty()) {
                return;
            }
            viewerIds = new ArrayList<>(viewers);
            listed = listedViewerIds(viewerIds).stream()
                    .map(id -> {
                        var identity = sessionViewerIdentity.get(id);
                        return new PostViewerDto(id, identity.name(), identity.color());
                    })
                    .toList();
        }
        var message = MessageDto.builder().type(MessageType.POST_VIEWERS)
                .payload(new PostViewersDto(postId, listed, viewerIds.size())).build();
        var serialized = serialize(message);
        if (serialized == null) {
            return;
        }
        for (var viewerId : viewerIds) {
            var target = sessions.get(viewerId);
            if (target != null) {
                enqueueSend(target, serialized);
            }
        }
    }

    /**
     * 세션이 연결 종료될 때 시청 중이던 게시글에서 제거한다.
     */
    private void clearViewingPost(String sessionId) {
        String previousPostId;
        synchronized (presenceLock) {
            previousPostId = sessionViewingPost.remove(sessionId);
            sessionViewerIdentity.remove(sessionId);
            sessionReadPosition.remove(sessionId);
            if (previousPostId != null) {
                removeViewer(previousPostId, sessionId);
            }
        }
        if (previousPostId != null) {
            broadcastPostViewerCount(previousPostId);
            broadcastPostViewers(previousPostId);
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
        dropCursor(postId, sessionId);
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

    // ───────────── 함께 읽는 사람 커서 ─────────────

    /**
     * 클라이언트가 보낸 커서 위치를 보관한다. 실제 전송은 flushCursors 가 한다.
     * 손님도 똑같이 참여한다 (이름·색은 클라이언트가 정해 보낸다).
     */
    private void handleCursorMove(WebSocketSession session, Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return;
        }
        var sessionId = session.getId();
        var now = System.currentTimeMillis();
        var last = cursorLastMoveAt.get(sessionId);
        if (last != null && now - last < CURSOR_MOVE_MIN_INTERVAL_MILLIS) {
            return;
        }
        cursorLastMoveAt.put(sessionId, now);

        var postId = sessionViewingPost.get(sessionId);
        if (postId == null) {
            return;
        }
        if (viewerCountOf(postId) > MAX_CURSOR_VIEWERS) {
            // 읽는 사람이 너무 많으면 커서는 받지 않고, 보낸 사람에게만 한 번 알린다 (방송하지 않는다).
            if (cursorDisabledNotified.add(sessionId)) {
                var serialized = serialize(MessageDto.builder().type(MessageType.CURSOR_BATCH)
                        .payload(new CursorBatchDto(postId, List.of(), List.of(), true)).build());
                if (serialized != null) {
                    enqueueSend(session, serialized);
                }
            }
            return;
        }
        cursorDisabledNotified.remove(sessionId);

        // 본문 영역 기준 비율. 본문 밖(좌우 여백·위아래)도 따라오도록 조금 넘치는 값을 허용한다.
        var x = clamp(asDouble(map.get("x")), -1.5, 2.5);
        var y = clamp(asDouble(map.get("y")), -0.5, 1.5);
        var name = truncate(asString(map.get("name"), "익명").strip(), CURSOR_NAME_MAX);
        var color = sanitizeColor(asString(map.get("color"), "#3b5bdb"));
        var cursors = postCursors.computeIfAbsent(postId, key -> new ConcurrentHashMap<>());
        var existing = cursors.get(sessionId);
        var message = existing != null ? existing.message() : null;
        var messageAt = existing != null ? existing.messageAt() : 0L;
        cursors.put(sessionId, new CursorStateDto(sessionId, x, y, name.isEmpty() ? "익명" : name, color, message, messageAt, now));
        dirtyCursorPosts.add(postId);

        if (cursorSelfSent.add(sessionId)) {
            var serialized = serialize(MessageDto.builder().type(MessageType.CURSOR_SELF).payload(sessionId).build());
            if (serialized != null) {
                enqueueSend(session, serialized);
            }
        }
    }

    /**
     * 커서 옆에 띄울 짧은 말을 보관한다. 빈 문자열이면 지운다.
     */
    private void handleCursorChat(WebSocketSession session, Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return;
        }
        var sessionId = session.getId();
        var now = System.currentTimeMillis();
        var last = cursorLastChatAt.get(sessionId);
        if (last != null && now - last < CURSOR_CHAT_MIN_INTERVAL_MILLIS) {
            return;
        }
        cursorLastChatAt.put(sessionId, now);

        var postId = sessionViewingPost.get(sessionId);
        if (postId == null) {
            return;
        }
        var cursors = postCursors.get(postId);
        var existing = cursors != null ? cursors.get(sessionId) : null;
        if (existing == null) {
            return;
        }
        var message = truncate(asString(map.get("message"), "").strip(), CURSOR_MESSAGE_MAX);
        cursors.put(sessionId, new CursorStateDto(existing.id(), existing.x(), existing.y(), existing.name(), existing.color(),
                message.isEmpty() ? null : message, message.isEmpty() ? 0L : now, now));
        dirtyCursorPosts.add(postId);
    }

    /**
     * 마우스가 창 밖으로 나갔거나 탭이 숨겨졌을 때: 커서만 지운다 (글은 계속 보는 중이므로 시청자 수는 그대로).
     */
    private void handleCursorLeave(WebSocketSession session) {
        var postId = sessionViewingPost.get(session.getId());
        if (postId != null) {
            dropCursor(postId, session.getId());
        }
    }

    /**
     * 세션이 글을 떠났을 때 커서를 지우고, 다음 flush 에서 "떠났다"고 알리도록 표시한다.
     */
    private void dropCursor(String postId, String sessionId) {
        var cursors = postCursors.get(postId);
        if (cursors == null || cursors.remove(sessionId) == null) {
            return;
        }
        if (cursors.isEmpty()) {
            postCursors.remove(postId);
        }
        leftCursors.computeIfAbsent(postId, key -> ConcurrentHashMap.newKeySet()).add(sessionId);
        dirtyCursorPosts.add(postId);
    }

    @Override
    public void flushCursors() {
        if (dirtyCursorPosts.isEmpty()) {
            return;
        }
        var now = System.currentTimeMillis();
        for (var postId : new ArrayList<>(dirtyCursorPosts)) {
            dirtyCursorPosts.remove(postId);

            Set<String> viewerIds;
            synchronized (presenceLock) {
                var viewers = postViewers.get(postId);
                viewerIds = viewers == null ? Set.of() : new HashSet<>(viewers);
            }
            var left = leftCursors.remove(postId);
            var leftIds = left == null ? new ArrayList<String>() : new ArrayList<>(left);
            var disabled = viewerIds.size() > MAX_CURSOR_VIEWERS;

            List<CursorStateDto> cursors = List.of();
            var cursorMap = postCursors.get(postId);
            if (cursorMap != null) {
                // 오래 안 움직인 커서는 떠난 것으로 본다.
                var stale = cursorMap.values().stream().filter(c -> now - c.updatedAt() > CURSOR_STALE_MILLIS).map(CursorStateDto::id).toList();
                stale.forEach(id -> {
                    cursorMap.remove(id);
                    leftIds.add(id);
                });
                if (cursorMap.isEmpty()) {
                    postCursors.remove(postId);
                } else if (!disabled) {
                    cursors = cursorMap.values().stream()
                            .sorted(Comparator.comparingLong(CursorStateDto::updatedAt).reversed())
                            .limit(MAX_CURSORS_PER_BATCH)
                            .toList();
                }
            }
            if (viewerIds.isEmpty() || (cursors.isEmpty() && leftIds.isEmpty())) {
                continue;
            }
            var batch = new CursorBatchDto(postId, cursors, leftIds, disabled);
            var serialized = serialize(MessageDto.builder().type(MessageType.CURSOR_BATCH).payload(batch).build());
            if (serialized == null) {
                continue;
            }
            for (var viewerId : viewerIds) {
                var target = sessions.get(viewerId);
                if (target != null) {
                    enqueueSend(target, serialized);
                }
            }
        }
    }

    private double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0 : Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double clamp(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private String sanitizeColor(String value) {
        return value != null && value.matches("#[0-9a-fA-F]{6}") ? value.toLowerCase() : "#3b5bdb";
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
