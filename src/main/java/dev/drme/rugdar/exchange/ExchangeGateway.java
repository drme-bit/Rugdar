package dev.drme.rugdar.exchange;

import java.util.List;

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
}
