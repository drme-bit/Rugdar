package dev.drme.rugdar.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.drme.rugdar.dto.Ticker;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MarketDataHandler extends TopicWebSocketHandler {

    public MarketDataHandler(ObjectMapper mapper) {
        super(mapper);
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        if (ticker.id() == null) {
            return;
        }
        broadcastIf("ticker", ticker, (_, sub) -> matches(sub, ticker));
    }

    private static boolean matches(JsonNode sub, Ticker ticker) {
        if (sub == null) {
            return true;
        }
        JsonNode args = sub.path("args");
        String markets = args.path("markets").asText(null);
        String symbols = args.path("symbols").asText(null);
        if (markets != null && !markets.equalsIgnoreCase(ticker.exchange())) {
            return false;
        }
        return symbols == null || symbols.equalsIgnoreCase(ticker.symbol());
    }
}
