package org.rugdar.exchange.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.rugdar.dto.Ticker;
import org.rugdar.exchange.base.ExchangeWebSocketClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class BinanceExchangeWebSocketClient extends ExchangeWebSocketClient {

    private static final int SUBSCRIBE_ID = 1;

    private final List<String> symbols;
    private int nextId = 100;

    public BinanceExchangeWebSocketClient(
            @Value("${rugdar.exchanges.binance.url}") String url,
            @Value("${rugdar.exchanges.binance.symbols}") List<String> symbols,
            ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(url, mapper, publisher);
        this.symbols = symbols;
    }

    @Override
    protected void subscribe() {
        ArrayNode streams = mapper.createArrayNode();
        symbols.forEach(s -> streams.add(s.toLowerCase() + "@ticker"));
        ObjectNode message = mapper.createObjectNode();
        message.put("method", "SUBSCRIBE");
        message.set("params", streams);
        message.put("id", SUBSCRIBE_ID);
        sendJson(message);
    }

    @Override
    protected Optional<Ticker> parseTicker(String raw) {
        return readTree(raw)
                .filter(node -> "24hrTicker".equals(node.path("e").asText()))
                .map(node -> new Ticker(
                        "binance",
                        node.path("s").asText(),
                        price(node, "c"),
                        price(node, "o"),
                        price(node, "h"),
                        price(node, "l"),
                        price(node, "v"),
                        price(node, "q"),
                        Instant.now()));
    }

    private static BigDecimal price(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText("0"));
    }

    @Override
    protected void ping() {
        ObjectNode message = mapper.createObjectNode();
        message.put("method", "PING");
        message.put("id", nextRequestId());
        sendJson(message);
    }

    private int nextRequestId() {
        return nextId++;
    }
}
