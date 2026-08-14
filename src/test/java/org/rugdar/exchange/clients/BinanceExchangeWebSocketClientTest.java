package org.rugdar.exchange.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rugdar.dto.Ticker;
import org.springframework.context.ApplicationEventPublisher;

class BinanceExchangeWebSocketClientTest {

    private final BinanceExchangeWebSocketClient client = new BinanceExchangeWebSocketClient(
            "wss://stream.binance.com:9443/ws",
            List.of("BTCUSDT"),
            new ObjectMapper(),
            mock(ApplicationEventPublisher.class));

    @Test
    void parsesTickerMessage() {
        String raw = """
                {"e":"24hrTicker","s":"BTCUSDT","c":"100.5","o":"90.0","h":"110.0","l":"85.0",
                 "v":"10.5","q":"1000000.0","E":1720000000000}""";

        Optional<Ticker> parsed = client.parseTicker(raw);

        assertThat(parsed).isPresent();
        Ticker ticker = parsed.get();
        assertThat(ticker.exchange()).isEqualTo("binance");
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
        assertThat(client.parseTicker("{\"result\":null,\"id\":1}")).isEmpty();
    }

    @Test
    void ignoresMalformedJson() {
        assertThat(client.parseTicker("not json")).isEmpty();
    }
}
