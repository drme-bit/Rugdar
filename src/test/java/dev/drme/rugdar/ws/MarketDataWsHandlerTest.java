package dev.drme.rugdar.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.drme.rugdar.ws.MarketDataWsHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import dev.drme.rugdar.dto.Ticker;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class MarketDataWsHandlerTest {

    private final MarketDataWsHandler handler =
            new MarketDataWsHandler(new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void broadcastsTickerAsJsonToSubscribedSessions() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        subscribe(session, "ticker");

        handler.onTicker(ticker());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"v\":1", "\"type\":\"ticker\"", "\"exchange\":\"bybit\"", "\"symbol\":\"BTCUSDT\""));
    }

    @Test
    void unsubscribedSessionReceivesNothing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.onTicker(ticker());

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcastWrapsDataInTypedEnvelope() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        subscribe(session, "analysis");

        handler.broadcast("analysis", "bullish");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"analysis\"", "\"data\":\"bullish\""));
    }

    @Test
    void subscriptionFiltersBroadcastsByTopic() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        subscribe(session, "analysis");

        handler.broadcast("ticker", ticker());
        handler.broadcast("analysis", "bearish");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> p.contains("\"type\":\"subscribed\""))
                .anyMatch(p -> p.contains("\"type\":\"analysis\""))
                .noneMatch(p -> p.contains("\"type\":\"ticker\""));
    }

    @Test
    void unsubscribeStopsDelivery() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        subscribe(session, "analysis");
        handler.handleTextMessage(session, new TextMessage("{\"action\":\"unsubscribe\",\"topic\":\"analysis\"}"));

        handler.broadcast("ticker", ticker());
        handler.broadcast("analysis", "bearish");

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> p.contains("\"type\":\"unsubscribed\""))
                .noneMatch(p -> p.contains("\"type\":\"analysis\""))
                .noneMatch(p -> p.contains("\"type\":\"ticker\""));
    }

    @Test
    void unknownTopicIsRejected() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"action\":\"subscribe\",\"topic\":\"gossip\"}"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .anyMatch(p -> containsAll(p, "\"type\":\"error\"", "\"code\":\"UNKNOWN_TOPIC\""));
    }

    @Test
    void malformedMessageIsRejected() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("not json"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload())
                .contains("\"type\":\"error\"", "\"code\":\"BAD_REQUEST\"");
    }

    @Test
    void closedSessionReceivesNothing() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        handler.afterConnectionEstablished(session);
        subscribe(session, "ticker");
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        handler.onTicker(ticker());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues().stream().map(TextMessage::getPayload).toList())
                .noneMatch(p -> p.contains("\"type\":\"ticker\""));
    }

    @Test
    void failedSendRemovesSession() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        doThrow(new RuntimeException("boom")).when(session).sendMessage(any(TextMessage.class));
        handler.afterConnectionEstablished(session);
        subscribe(session, "ticker");

        handler.onTicker(ticker());
        handler.onTicker(ticker());

        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    private void subscribe(WebSocketSession session, String topic) throws Exception {
        handler.handleTextMessage(session, new TextMessage(
                "{\"action\":\"subscribe\",\"topic\":\"" + topic + "\"}"));
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
