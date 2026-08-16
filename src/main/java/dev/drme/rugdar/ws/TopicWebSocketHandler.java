package dev.drme.rugdar.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

import dev.drme.rugdar.utils.Log;
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

    protected TopicWebSocketHandler(ObjectMapper mapper) {
        this.mapper = mapper;
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
                    if (session.isOpen()) {
                        synchronized (lock(session)) {
                            session.sendMessage(new TextMessage(json));
                        }
                    }
                } catch (Exception e) {
                    sessions.remove(session);
                    sessionLocks.remove(session);
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
        sessionLocks.remove(session);
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
