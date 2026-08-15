package org.rugdar.exchange.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rugdar.dto.Ticker;
import org.springframework.context.ApplicationEventPublisher;

class BybitExchangeWebSocketClientTest {

    private final BybitExchangeWebSocketClient client = new BybitExchangeWebSocketClient(
            "wss://stream.bybit.com/v5/public/spot",
            List.of("BTCUSDT"),
            new ObjectMapper(),
            mock(ApplicationEventPublisher.class));

    @Test
    void parsesTickerMessage() {
        String raw = """
                {"topic":"tickers.BTCUSDT","ts":1720000000000,
                 "data":{"symbol":"BTCUSDT","lastPrice":"100.5","prevPrice24h":"90.0",
                 "highPrice24h":"110.0","lowPrice24h":"85.0","volume24h":"10.5","turnover24h":"1000000.0"}}""";

        Optional<Ticker> parsed = client.parseTicker(raw);

        assertThat(parsed).isPresent();
        Ticker ticker = parsed.get();
        assertThat(ticker.exchange()).isEqualTo("bybit");
        assertThat(ticker.symbol()).isEqualTo("BTCUSDT");
        assertThat(ticker.lastPrice()).isEqualByComparingTo("100.5");
        assertThat(ticker.open()).isEqualByComparingTo("90.0");
        assertThat(ticker.high()).isEqualByComparingTo("110.0");
        assertThat(ticker.low()).isEqualByComparingTo("85.0");
        assertThat(ticker.volume()).isEqualByComparingTo("10.5");
        assertThat(ticker.turnover()).isEqualByComparingTo("1000000.0");
    }

    @Test
    void ignoresNonTickerMessages() {
        assertThat(client.parseTicker("{\"op\":\"pong\",\"ts\":123}")).isEmpty();
    }

    @Test
    void ignoresMalformedJson() {
        assertThat(client.parseTicker("not json")).isEmpty();
    }
}
