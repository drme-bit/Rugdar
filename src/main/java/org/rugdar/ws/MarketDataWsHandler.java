package org.rugdar.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import org.rugdar.dto.Ticker;
import org.rugdar.utils.Log;
import org.slf4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MarketDataWsHandler extends TextWebSocketHandler {

    private static final Logger log = Log.get(MarketDataWsHandler.class);

    private static final int PROTOCOL_VERSION = 1;
    private static final Set<String> KNOWN_TOPICS = Set.of("ticker", "analysis");

    private final ObjectMapper mapper;
    private final AtomicLong seq = new AtomicLong();
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    private final Map<WebSocketSession, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();

    public MarketDataWsHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        if (ticker.id() == null) {
            return;
        }
        broadcast("ticker", ticker);
    }

    public void broadcast(String type, Object data) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                    "v", PROTOCOL_VERSION,
                    "type", type,
                    "seq", seq.incrementAndGet(),
                    "data", data));
            for (WebSocketSession session : sessions) {
                try {
                    if (session.isOpen() && wants(session, type)) {
                        synchronized (lock(session)) {
                            session.sendMessage(new TextMessage(json));
                        }
                    }
                } catch (Exception e) {
                    sessions.remove(session);
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("WS serialize error", e);
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        subscriptions.remove(session);
        sessionLocks.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = mapper.readTree(message.getPayload());
            String action = node.path("action").asText();
            String topic = node.path("topic").asText();

            switch (action) {
                case "subscribe" -> {
                    if (topic.isBlank()) {
                        error(session, "BAD_REQUEST", "missing 'topic'");
                    } else if (!KNOWN_TOPICS.contains(topic)) {
                        error(session, "UNKNOWN_TOPIC", "topic '" + topic + "' is not supported");
                    } else {
                        subscriptions.computeIfAbsent(session, s -> ConcurrentHashMap.newKeySet()).add(topic);
                        ack(session, "subscribed", topic);
                    }
                }
                case "unsubscribe" -> {
                    Set<String> topics = subscriptions.get(session);
                    if (topics != null) {
                        topics.remove(topic);
                    }
                    ack(session, "unsubscribed", topic);
                }
                default -> error(session, "UNKNOWN_ACTION", "action '" + action + "' is not supported");
            }
        } catch (JsonProcessingException e) {
            error(session, "BAD_REQUEST", "malformed JSON message");
        }
    }

    private void ack(WebSocketSession session, String type, String topic) {
        send(session, Map.of(
                "v", PROTOCOL_VERSION,
                "type", type,
                "seq", seq.incrementAndGet(),
                "topic", topic));
    }

    private void error(WebSocketSession session, String code, String message) {
        send(session, Map.of(
                "v", PROTOCOL_VERSION,
                "type", "error",
                "seq", seq.incrementAndGet(),
                "code", code,
                "message", message));
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            synchronized (lock(session)) {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
            }
        } catch (Exception e) {
            log.warn("WS send error", e);
        }
    }

    private Object lock(WebSocketSession session) {
        return sessionLocks.computeIfAbsent(session, _ -> new Object());
    }

    private boolean wants(WebSocketSession session, String type) {
        Set<String> topics = subscriptions.get(session);
        return topics != null && topics.contains(type);
    }
}
