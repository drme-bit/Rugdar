package dev.drme.rugdar.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.drme.rugdar.dto.Ticker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MarketDataHandlerTest {

    private final MarketDataHandler handler = new MarketDataHandler(
            new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void broadcastsTickerToConnectedSessions() throws Exception {
        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);

        handler.onTicker(ticker());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"v\":1", "\"type\":\"ticker\"", "\"exchange\":\"bybit\"", "\"symbol\":\"BTCUSDT\""));
    }

    @Test
    void disconnectedSessionReceivesNothing() throws Exception {
        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        handler.onTicker(ticker());

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void rawTickerWithoutIdIsNotBroadcast() throws Exception {
        WebSocketSession session = session();
        handler.afterConnectionEstablished(session);

        handler.onTicker(new Ticker(
                "bybit", "BTCUSDT",
                new BigDecimal("100.5"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now()));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    private static WebSocketSession session() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/market"));
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

    private static Ticker ticker() {
        return new Ticker(
                UUID.randomUUID(),
                "bybit", "BTCUSDT",
                new BigDecimal("100.5"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }
}
