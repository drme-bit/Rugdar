package org.rugdar.service.MarketService;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.rugdar.dto.Ticker;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private static final int HISTORY_LIMIT = 200;

    private final Map<String, Ticker> latest = new ConcurrentHashMap<>();
    private final Map<String, Deque<Ticker>> history = new ConcurrentHashMap<>();

    @EventListener
    public void onTicker(Ticker ticker) {
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
