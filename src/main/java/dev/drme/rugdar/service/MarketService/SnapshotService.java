package dev.drme.rugdar.service.MarketService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.utils.JsonUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class SnapshotService {

    private static final Duration TTL = Duration.ofSeconds(60);

    private final MarketDataService marketData;
    private final Map<String, Entry> perSymbol = new ConcurrentHashMap<>();

    private volatile boolean marketDirty = true;
    private volatile Entry marketSnapshot;

    public SnapshotService(MarketDataService marketData) {
        this.marketData = marketData;
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        String key = key(ticker.exchange(), ticker.symbol());
        perSymbol.put(key, new Entry(JsonUtils.serialize(ticker), Instant.now()));
        marketDirty = true;
    }

    public String currentJson() {
        Entry e = marketSnapshot;
        if (!marketDirty && e != null && e.isFresh()) return e.json;
        synchronized (this) {
            e = marketSnapshot;
            if (!marketDirty && e != null && e.isFresh()) return e.json;
            e = new Entry(JsonUtils.serialize(List.copyOf(marketData.allLatest().values())), Instant.now());
            marketSnapshot = e;
            marketDirty = false;
            return e.json;
        }
    }

    public String getSnapshot(String exchange, String symbol) {
        Entry e = perSymbol.get(key(exchange, symbol));
        if (e != null && e.isFresh()) return e.json;
        return marketData.latest(exchange, symbol)
                .map(JsonUtils::serialize)
                .orElse(null);
    }

    private static String key(String exchange, String symbol) {
        return exchange + ":" + symbol;
    }

    private record Entry(String json, Instant takenAt) {
        boolean isFresh() {
            return takenAt.plus(TTL).isAfter(Instant.now());
        }
    }
}