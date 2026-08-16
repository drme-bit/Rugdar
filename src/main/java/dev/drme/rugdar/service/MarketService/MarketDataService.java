package dev.drme.rugdar.service.MarketService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.service.IdService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private static final int HISTORY_LIMIT = 200;

    private final Map<String, Ticker> latest = new ConcurrentHashMap<>();
    private final Map<String, Deque<Ticker>> history = new ConcurrentHashMap<>();

    private final IdService ids;
    private final ApplicationEventPublisher publisher;

    public MarketDataService(IdService ids, ApplicationEventPublisher publisher) {
        this.ids = ids;
        this.publisher = publisher;
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        if (ticker.id() == null) {
            Ticker enriched = new Ticker(
                    ids.next(),
                    ticker.exchange(), ticker.symbol(),
                    ticker.lastPrice(), ticker.open(), ticker.high(), ticker.low(),
                    ticker.volume(), ticker.turnover(), ticker.timestamp());
            store(enriched);
            publisher.publishEvent(enriched);
        } else {
            store(ticker);
        }
    }

    private void store(Ticker ticker) {
        String key = ticker.exchange() + ":" + ticker.symbol();
        latest.put(key, ticker);
        Deque<Ticker> deque = history.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(ticker);
            while (deque.size() > HISTORY_LIMIT) {
                deque.removeFirst();
            }
        }
    }

    public Optional<Ticker> latest(String exchange, String symbol) {
        return Optional.ofNullable(latest.get(exchange + ":" + symbol));
    }

    public List<Ticker> history(String exchange, String symbol) {
        Deque<Ticker> deque = history.get(exchange + ":" + symbol);
        if (deque == null) {
            return List.of();
        }
        synchronized (deque) {
            return List.copyOf(deque);
        }
    }

    public Map<String, Ticker> allLatest() {
        return Map.copyOf(latest);
    }
}
