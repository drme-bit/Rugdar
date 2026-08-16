package dev.drme.rugdar.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.drme.rugdar.exchange.ExchangeGateway;
import dev.drme.rugdar.exchange.base.ConnectionState;
import dev.drme.rugdar.exchange.base.ExchangeStatusChangedEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MarketStatusHandlerTest {

    private final ExchangeGateway gateway = mock(ExchangeGateway.class);
    private final MarketStatusHandler handler = new MarketStatusHandler(new ObjectMapper(), gateway);

    @Test
    void connectionSendsStatusSnapshot() throws Exception {
        when(gateway.status()).thenReturn(Map.of(
                "binance", ConnectionState.CONNECTED,
                "bybit", ConnectionState.RECONNECTING));

        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"marketStatus\"", "\"binance\":\"CONNECTED\"", "\"bybit\":\"RECONNECTING\""));
    }

    @Test
    void statusChangeBroadcastsSnapshot() throws Exception {
        when(gateway.status()).thenReturn(Map.of("binance", ConnectionState.CONNECTED));

        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);
        handler.onStatusChanged(new ExchangeStatusChangedEvent("binance", ConnectionState.CONNECTED));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"marketStatus\"", "\"binance\":\"CONNECTED\""));
    }

    @Test
    void disconnectedSessionReceivesNothing() throws Exception {
        when(gateway.status()).thenReturn(Map.of("binance", ConnectionState.CONNECTED));

        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);

        when(gateway.status()).thenReturn(Map.of("binance", ConnectionState.RECONNECTING));
        handler.onStatusChanged(new ExchangeStatusChangedEvent("binance", ConnectionState.RECONNECTING));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .noneMatch(p -> p.contains("\"binance\":\"RECONNECTING\""));
    }

    private static WebSocketSession session() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/status"));
        return session;
    }

    private static boolean containsAll(String text, String... parts) {
        for (String part : parts) {
            if (!text.contains(part)) {
                return false;
            }
        }
        return true;
    }
}
