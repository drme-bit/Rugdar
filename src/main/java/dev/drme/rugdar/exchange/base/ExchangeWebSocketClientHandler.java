package dev.drme.rugdar.exchange.base;

import org.jspecify.annotations.NonNull;
import dev.drme.rugdar.utils.Log;
import org.slf4j.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

public class ExchangeWebSocketClientHandler implements WebSocketHandler {

    private static final Logger log = Log.get(ExchangeWebSocketClientHandler.class);

    private final ExchangeWebSocketClient client;

    public ExchangeWebSocketClientHandler(ExchangeWebSocketClient client) {
        this.client = client;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        client.onConnected(session);
    }

    @Override
    public void handleMessage(@NonNull WebSocketSession session, WebSocketMessage<?> message) {
        if (message instanceof TextMessage text) {
            client.onMessage(text.getPayload());
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, Throwable exception) {
        log.warn("WS transport error", exception);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, CloseStatus closeStatus) {
        client.onDisconnected();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
