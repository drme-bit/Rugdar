package dev.drme.rugdar.exchange.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.exchange.base.ExchangeWebSocketClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BybitExchangeWebSocketClient extends ExchangeWebSocketClient {

    private final List<String> symbols;

    public BybitExchangeWebSocketClient(
            @Value("${rugdar.exchanges.bybit.url}") String url,
            @Value("${rugdar.exchanges.bybit.symbols}") List<String> symbols,
            ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(url, mapper, publisher);
        this.symbols = symbols;
    }

    @Override
    protected void subscribe() {
        ArrayNode topics = mapper.createArrayNode();
        symbols.forEach(s -> topics.add("tickers." + s));
        ObjectNode message = mapper.createObjectNode();
        message.put("op", "subscribe");
        message.set("args", topics);
        sendJson(message);
    }

    @Override
    protected Optional<Ticker> parseTicker(String raw) {
        return readTree(raw)
                .filter(node -> node.has("topic"))
                .map(node -> {
                    JsonNode data = node.path("data");
                    return new Ticker(
                            "bybit",
                            data.path("symbol").asText(),
                            price(data, "lastPrice"),
                            price(data, "prevPrice24h"),
                            price(data, "highPrice24h"),
                            price(data, "lowPrice24h"),
                            price(data, "volume24h"),
                            price(data, "turnover24h"),
                            Instant.now());
                });
    }

    private static BigDecimal price(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText("0"));
    }

    @Override
    protected void ping() {
        ObjectNode message = mapper.createObjectNode();
        message.put("op", "ping");
        sendJson(message);
    }
}
