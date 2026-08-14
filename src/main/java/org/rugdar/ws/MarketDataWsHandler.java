package org.rugdar.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CopyOnWriteArraySet;

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

    private final ObjectMapper mapper;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    public MarketDataWsHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        try {
            broadcast(mapper.writeValueAsString(ticker));
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
    }

    private void broadcast(String json) {
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            } catch (Exception e) {
                sessions.remove(session);
            }
        }
    }
}
