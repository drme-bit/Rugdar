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
public class WhitebitExchangeWebSocketClient extends ExchangeWebSocketClient {

    private static final int PING_ID = 1;

    private final List<String> symbols;
    private int nextId = 100;

    public WhitebitExchangeWebSocketClient(
            @Value("${rugdar.exchanges.whitebit.url}") String url,
            @Value("${rugdar.exchanges.whitebit.symbols}") List<String> symbols,
            ObjectMapper mapper,
            ApplicationEventPublisher publisher) {
        super(url, mapper, publisher);
        this.symbols = symbols;
    }

    @Override
    public String exchangeName() {
        return "whitebit";
    }

    @Override
    protected void subscribe() {
        ArrayNode markets = mapper.createArrayNode();
        symbols.forEach(markets::add);
        ObjectNode message = mapper.createObjectNode();
        message.put("id", nextRequestId());
        message.put("method", "market_subscribe");
        message.set("params", markets);
        sendJson(message);
    }

    @Override
    protected Optional<Ticker> parseTicker(String raw) {
        return readTree(raw)
                .filter(node -> node.has("method"))
                .map(node -> {
                    JsonNode params = node.path("params");
                    JsonNode stats = params.size() > 1 ? params.get(1) : params.get(0);
                    String symbol = !params.isEmpty() ? params.get(0).asText().replace("_", "") : null;
                    if (symbol == null || stats.path("last").isMissingNode()) {
                        return null;
                    }
                    return new Ticker(
                            "whitebit",
                            symbol,
                            price(stats, "last"),
                            price(stats, "open"),
                            price(stats, "high"),
                            price(stats, "low"),
                            price(stats, "volume"),
                            price(stats, "deal"),
                            Instant.now());
                });
    }

    private static BigDecimal price(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText("0"));
    }

    @Override
    protected void ping() {
        ObjectNode message = mapper.createObjectNode();
        message.put("id", PING_ID);
        message.put("method", "ping");
        message.set("params", mapper.createArrayNode());
        sendJson(message);
    }

    private int nextRequestId() {
        return nextId++;
    }
}
