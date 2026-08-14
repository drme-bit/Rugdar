package org.rugdar.exchange;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.rugdar.exchange.base.ExchangeWebSocketClient;

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
