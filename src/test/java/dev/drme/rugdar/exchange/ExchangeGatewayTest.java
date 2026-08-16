package dev.drme.rugdar.exchange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import dev.drme.rugdar.exchange.ExchangeGateway;
import org.junit.jupiter.api.Test;
import dev.drme.rugdar.exchange.base.ExchangeWebSocketClient;

class ExchangeGatewayTest {

    @Test
    void connectConnectsAllClients() {
        ExchangeWebSocketClient bybit = mock(ExchangeWebSocketClient.class);
        ExchangeWebSocketClient binance = mock(ExchangeWebSocketClient.class);
        ExchangeGateway gateway = new ExchangeGateway(List.of(bybit, binance));

        gateway.connect();

        verify(bybit).connect();
        verify(binance).connect();
    }
}
