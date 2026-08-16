package dev.drme.rugdar.ws;

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
        broadcast("ticker", ticker);
    }
}
