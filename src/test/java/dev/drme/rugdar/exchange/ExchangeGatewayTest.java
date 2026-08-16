package dev.drme.rugdar.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import dev.drme.rugdar.exchange.ExchangeGateway;
import dev.drme.rugdar.exchange.base.ConnectionState;
import dev.drme.rugdar.exchange.base.ExchangeWebSocketClient;
import org.junit.jupiter.api.Test;

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

    @Test
    void statusReturnsStatePerClient() {
        ExchangeWebSocketClient bybit = mock(ExchangeWebSocketClient.class);
        when(bybit.exchangeName()).thenReturn("bybit");
        when(bybit.state()).thenReturn(ConnectionState.CONNECTED);
        ExchangeWebSocketClient binance = mock(ExchangeWebSocketClient.class);
        when(binance.exchangeName()).thenReturn("binance");
        when(binance.state()).thenReturn(ConnectionState.RECONNECTING);
        ExchangeGateway gateway = new ExchangeGateway(List.of(bybit, binance));

        Map<String, ConnectionState> status = gateway.status();

        assertThat(status).containsEntry("bybit", ConnectionState.CONNECTED);
        assertThat(status).containsEntry("binance", ConnectionState.RECONNECTING);
    }
}
