package dev.drme.rugdar.exchange.base;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.drme.rugdar.dto.Ticker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketSession;

class ExchangeWebSocketClientStateTest {

    private final List<Object> published = new ArrayList<>();
    private final TestClient client = new TestClient(
            "wss://test.example", new ObjectMapper(), published::add);

    @AfterEach
    void tearDown() {
        client.destroy();
    }

    @Test
    void initialStateIsDisconnected() {
        assertThat(client.state()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    @Test
    void onConnectedSetsConnectedAndPublishesEvent() {
        client.onConnected(null);

        assertThat(client.state()).isEqualTo(ConnectionState.CONNECTED);
        assertThat(statusEvents()).contains(
                new ExchangeStatusChangedEvent("test", ConnectionState.CONNECTED));
    }

    @Test
    void repeatedConnectedDoesNotRepublishEvent() {
        client.onConnected(null);
        client.onConnected(null);

        assertThat(statusEvents()).hasSize(1);
    }

    @Test
    void onDisconnectedEntersReconnectingState() {
        client.onConnected(null);
        client.onDisconnected();

        assertThat(client.state()).isEqualTo(ConnectionState.RECONNECTING);
        assertThat(statusEvents()).contains(
                new ExchangeStatusChangedEvent("test", ConnectionState.CONNECTED),
                new ExchangeStatusChangedEvent("test", ConnectionState.DISCONNECTED),
                new ExchangeStatusChangedEvent("test", ConnectionState.RECONNECTING));
    }

    @Test
    void giveUpAfterMaxAttemptsSetsFailed() {
        for (int i = 0; i < 20; i++) {
            client.onDisconnected();
        }

        assertThat(client.state()).isEqualTo(ConnectionState.FAILED);
        assertThat(statusEvents().getLast())
                .isEqualTo(new ExchangeStatusChangedEvent("test", ConnectionState.FAILED));
    }

    private List<ExchangeStatusChangedEvent> statusEvents() {
        return published.stream()
                .filter(ExchangeStatusChangedEvent.class::isInstance)
                .map(ExchangeStatusChangedEvent.class::cast)
                .toList();
    }

    private static class TestClient extends ExchangeWebSocketClient {
        TestClient(String url, ObjectMapper mapper, ApplicationEventPublisher publisher) {
            super(url, mapper, publisher);
        }

        @Override
        public String exchangeName() {
            return "test";
        }

        @Override
        protected Optional<Ticker> parseTicker(String raw) {
            return Optional.empty();
        }

        @Override
        protected void subscribe() {
        }

        @Override
        protected void ping() {
        }
    }
}
