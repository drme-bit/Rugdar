package dev.drme.rugdar.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

import dev.drme.rugdar.exchange.ExchangeGateway;
import dev.drme.rugdar.exchange.base.ExchangeStatusChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class MarketStatusHandler extends TopicWebSocketHandler {

    private final ExchangeGateway gateway;

    public MarketStatusHandler(ObjectMapper mapper, ExchangeGateway gateway) {
        super(mapper);
        this.gateway = gateway;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        super.afterConnectionEstablished(session);
        sendStatus(session);
    }

    @EventListener
    public void onStatusChanged(ExchangeStatusChangedEvent event) {
        broadcast("marketStatus", gateway.status());
    }

    private void sendStatus(WebSocketSession session) {
        send(session, Map.of(
                "v", 1,
                "type", "marketStatus",
                "seq", nextSeq(),
                "data", gateway.status()));
    }
}
