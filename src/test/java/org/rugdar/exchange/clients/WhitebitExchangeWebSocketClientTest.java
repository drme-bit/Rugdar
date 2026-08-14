package org.rugdar.exchange.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.rugdar.dto.Ticker;
import org.springframework.context.ApplicationEventPublisher;

class WhitebitExchangeWebSocketClientTest {

    private final WhitebitExchangeWebSocketClient client = new WhitebitExchangeWebSocketClient(
            "wss://api.whitebit.com/ws",
            List.of("BTC_USDT"),
            new ObjectMapper(),
            mock(ApplicationEventPublisher.class));

    @Test
    void parsesMarketUpdateMessage() {
        String raw = """
                {"id":123,"method":"market_update",
                 "params":["BTC_USDT",{"last":"100.5","open":"90.0","high":"110.0","low":"85.0",
                 "volume":"10.5","deal":"1000000.0"}]}""";

        Optional<Ticker> parsed = client.parseTicker(raw);

        assertThat(parsed).isPresent();
        Ticker ticker = parsed.get();
        assertThat(ticker.exchange()).isEqualTo("whitebit");
        assertThat(ticker.symbol()).isEqualTo("BTCUSDT");
        assertThat(ticker.lastPrice()).isEqualByComparingTo("100.5");
        assertThat(ticker.open()).isEqualByComparingTo("90.0");
        assertThat(ticker.high()).isEqualByComparingTo("110.0");
        assertThat(ticker.low()).isEqualByComparingTo("85.0");
        assertThat(ticker.volume()).isEqualByComparingTo("10.5");
        assertThat(ticker.turnover()).isEqualByComparingTo("1000000.0");
    }

    @Test
    void ignoresNonMarketMessages() {
        assertThat(client.parseTicker("{\"id\":1,\"method\":\"ping\",\"result\":\"pong\"}")).isEmpty();
    }

    @Test
    void ignoresMalformedJson() {
        assertThat(client.parseTicker("not json")).isEmpty();
    }
}
