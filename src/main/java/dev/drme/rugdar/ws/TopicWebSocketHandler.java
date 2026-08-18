package dev.drme.rugdar.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;

import dev.drme.rugdar.utils.Log;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public abstract class TopicWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = Log.get(TopicWebSocketHandler.class);

    private static final int PROTOCOL_VERSION = 1;

    protected final ObjectMapper mapper;

    private final AtomicLong seq = new AtomicLong();
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, JsonNode> subscriptions = new ConcurrentHashMap<>();

    protected TopicWebSocketHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void broadcast(String type, Object data) {
        broadcastIf(type, data, (session, sub) -> true);
    }

    public void broadcastIf(String type, Object data, BiPredicate<WebSocketSession, JsonNode> filter) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "v", PROTOCOL_VERSION,
                    "type", type,
                    "seq", seq.incrementAndGet(),
                    "data", data));
            for (WebSocketSession session : sessions) {
                try {
                    JsonNode sub = subscriptions.getOrDefault(session, null);
                    if (!filter.test(session, sub)) {
                        continue;
                    }
                    if (session.isOpen()) {
                        synchronized (lock(session)) {
                            session.sendMessage(new TextMessage(json));
                        }
                    }
                } catch (Exception e) {
                    sessions.remove(session);
                    sessionLocks.remove(session);
                    subscriptions.remove(session);
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("WS serialize error", e);
        }
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());

            // op/option can be sub or unsub
            String type = node.path("op").asText("");
            switch (type) {
                case "sub", "subscribe" -> subscriptions.put(session, node);
                case "unsub", "unsubscribe" -> subscriptions.remove(session);
            }
        } catch (Exception e) {
            log.warn("WS parse error from {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
        sessionLocks.remove(session);
        subscriptions.remove(session);
    }

    protected void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            synchronized (lock(session)) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.warn("WS send error", e);
        }
    }

    protected long nextSeq() {
        return seq.incrementAndGet();
    }

    private Object lock(WebSocketSession session) {
        return sessionLocks.computeIfAbsent(session, _ -> new Object());
    }
}
