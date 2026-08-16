package dev.drme.rugdar.exchange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.drme.rugdar.exchange.base.ConnectionState;
import dev.drme.rugdar.exchange.base.ExchangeWebSocketClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExchangeGateway {

    private final List<ExchangeWebSocketClient> clients;

    public ExchangeGateway(List<ExchangeWebSocketClient> clients) {
        this.clients = clients;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void connect() {
        clients.forEach(ExchangeWebSocketClient::connect);
    }

    public Map<String, ConnectionState> status() {
        Map<String, ConnectionState> status = new LinkedHashMap<>();
        for (ExchangeWebSocketClient client : clients) {
            status.put(client.exchangeName(), client.state());
        }
        return status;
    }
}
