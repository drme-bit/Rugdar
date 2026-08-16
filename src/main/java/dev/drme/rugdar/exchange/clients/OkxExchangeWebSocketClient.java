package dev.drme.rugdar.exchange.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.exchange.base.ExchangeWebSocketClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class OkxExchangeWebSocketClient extends ExchangeWebSocketClient {

    private final List<String> symbols;

    public OkxExchangeWebSocketClient(
            @Value("${rugdar.exchanges.okx.url}") String url,
            @Value("${rugdar.exchanges.okx.symbols}") List<String> symbols,
            ObjectMapper mapper,
            ApplicationEventPublisher publisher
    ) {
        super(url, mapper, publisher);
        this.symbols = symbols;
    }

    @Override
    protected Optional<Ticker> parseTicker(String raw) {
        return readTree(raw)
                .filter(node -> "tickers".equals(node.path("arg").path("channel").asText()))
                .filter(node -> node.path("data").isArray() && !node.path("data").isEmpty())
                .map(node -> {
                    JsonNode t = node.path("data").get(0);
                    return new Ticker(
                            exchangeName(),
                            t.path("instId").asText().replace("-", ""),
                            price(t, "last"),
                            price(t, "open24h"),
                            price(t, "high24h"),
                            price(t, "low24h"),
                            price(t, "vol24h"),
                            price(t, "volCcy24h"),
                            Instant.ofEpochMilli(t.path("ts").asLong()));
                });
    }

    @Override
    protected void subscribe() {
        ArrayNode args = mapper.createArrayNode();
        for (String symbol : symbols) {
            ObjectNode arg = mapper.createObjectNode();
            arg.put("channel", "tickers");
            arg.put("instId", symbol);
            args.add(arg);
        }
        ObjectNode message = mapper.createObjectNode();
        message.put("op", "subscribe");
        message.set("args", args);
        sendJson(message);
    }

    @Override
    protected void ping() {
        ObjectNode message = mapper.createObjectNode();
        message.put("op", "ping");
        sendJson(message);
    }

    @Override
    public String exchangeName() {
        return "okx";
    }

    private static BigDecimal price(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText("0"));
    }
}
