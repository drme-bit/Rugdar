package dev.drme.rugdar.exchange.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.drme.rugdar.dto.Ticker;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

class OkxExchangeWebSocketClientTest {

    private final OkxExchangeWebSocketClient client = new OkxExchangeWebSocketClient(
            "wss://ws.okx.com:8443/ws/v5/public",
            List.of("BTC-USDT", "ETH-USDT"),
            new ObjectMapper(),
            mock(ApplicationEventPublisher.class));

    @Test
    void parsesTickerMessage() {
        String raw = """
                {"arg":{"channel":"tickers","instId":"BTC-USDT"},"data":[{
                  "instType":"SPOT","instId":"BTC-USDT","last":"43250.1","lastSz":"0.21671",
                  "askPx":"43255.4","askSz":"0.00015","bidPx":"43250.1","bidSz":"0.00118",
                  "open24h":"42425.1","high24h":"43630.1","low24h":"42137.6",
                  "volCcy24h":"1805.9152318","vol24h":"41893.8","ts":"1720000000000",
                  "sodUtc0":"42425.1","sodUtc8":"42425.1"}],"ts":"1720000000000"}""";

        Optional<Ticker> parsed = client.parseTicker(raw);

        assertThat(parsed).isPresent();
        Ticker ticker = parsed.get();
        assertThat(ticker.exchange()).isEqualTo("okx");
        assertThat(ticker.symbol()).isEqualTo("BTCUSDT");
        assertThat(ticker.lastPrice()).isEqualByComparingTo("43250.1");
        assertThat(ticker.open()).isEqualByComparingTo("42425.1");
        assertThat(ticker.high()).isEqualByComparingTo("43630.1");
        assertThat(ticker.low()).isEqualByComparingTo("42137.6");
        assertThat(ticker.volume()).isEqualByComparingTo("41893.8");
        assertThat(ticker.turnover()).isEqualByComparingTo("1805.9152318");
    }

    @Test
    void ignoresNonTickerMessages() {
        assertThat(client.parseTicker("""
                {"event":"subscribe","arg":{"channel":"tickers","instId":"BTC-USDT"}}"""))
                .isEmpty();
        assertThat(client.parseTicker("""
                {"event":"error","code":60012,"msg":"Channel does not exist"}"""))
                .isEmpty();
        assertThat(client.parseTicker("""
                {"event":"pong","code":"0","msg":""}"""))
                .isEmpty();
    }

    @Test
    void ignoresEmptyDataArray() {
        assertThat(client.parseTicker("""
                {"arg":{"channel":"tickers","instId":"BTC-USDT"},"data":[]}"""))
                .isEmpty();
    }

    @Test
    void ignoresMalformedJson() {
        assertThat(client.parseTicker("not json")).isEmpty();
    }

    @Test
    void exchangeNameIsOkx() {
        assertThat(client.exchangeName()).isEqualTo("okx");
    }

    @Test
    void subscribesWithOneArgPerSymbol() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
        ReflectionTestUtils.setField(client, "session", session);

        client.subscribe();

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertThat(payload)
                .contains("\"op\":\"subscribe\"")
                .contains("\"channel\":\"tickers\"")
                .contains("\"instId\":\"BTC-USDT\"")
                .contains("\"instId\":\"ETH-USDT\"");
    }
}
