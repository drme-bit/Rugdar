package dev.drme.rugdar.service.MarketService;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import dev.drme.rugdar.dto.IndicatorValues;
import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.repository.market.MarketDataRepository;
import dev.drme.rugdar.service.IdService;
import dev.drme.rugdar.utils.Log;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private static final Logger log = Log.get(MarketDataService.class);

    private static final int HISTORY_LIMIT = 200;
    private static final int QUEUE_CAPACITY = 20_000;
    private static final int SMA_PERIOD = 20;
    private static final int EMA_PERIOD = 20;

    private final Map<String, Ticker> latest = new ConcurrentHashMap<>();
    private final Map<String, Deque<Ticker>> history = new ConcurrentHashMap<>();
    private final Map<String, IndicatorState> indicators = new ConcurrentHashMap<>();
    private final BlockingQueue<Ticker> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private final IdService ids;
    private final ApplicationEventPublisher publisher;
    private final MarketDataRepository repository;

    public int SMA;

    @Value("${rugdar.storage.retention-days:7}")
    private int retentionDays;

    public MarketDataService(IdService ids, ApplicationEventPublisher publisher, MarketDataRepository repository) {
        this.ids = ids;
        this.publisher = publisher;
        this.repository = repository;
    }

    @EventListener
    public void onTicker(Ticker ticker) {
        if (ticker.id() != null) {
            return;
        }
        Ticker enriched = new Ticker(
                ids.next(),
                ticker.exchange(), ticker.symbol(),
                ticker.lastPrice(), ticker.open(), ticker.high(), ticker.low(),
                ticker.volume(), ticker.turnover(), ticker.timestamp());
        store(enriched);
        publisher.publishEvent(enriched);
    }

    private void store(Ticker ticker) {
        String key = key(ticker.exchange(), ticker.symbol());
        latest.put(key, ticker);
        Deque<Ticker> deque = history.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(ticker);
            while (deque.size() > HISTORY_LIMIT) {
                deque.removeFirst();
            }
        }
        indicators.computeIfAbsent(key, k -> new IndicatorState(SMA_PERIOD, EMA_PERIOD))
                .update(ticker.lastPrice());
        if (!queue.offer(ticker)) {
            log.warn("Persist queue full, dropping ticker {}", key);
        }
    }

    @Scheduled(fixedDelayString = "${rugdar.storage.flush-interval-ms:10000}")
    public void flush() {
        List<Ticker> batch = new ArrayList<>();
        queue.drainTo(batch);
        if (batch.isEmpty()) {
            return;
        }
        try {
            repository.saveAll(batch);
        } catch (Exception e) {
            log.error("Failed to save {} tickers to DB", batch.size(), e);
            batch.forEach(queue::offer);
        }
    }

    @Scheduled(fixedDelayString = "${rugdar.storage.cleanup-interval-ms:3600000}")
    public void cleanOld() {
        try {
            int removed = repository.deleteOlderThan(retentionDays);
            if (removed > 0) {
                log.info("Removed {} tickers older than {} days", removed, retentionDays);
            }
        } catch (Exception e) {
            log.warn("Failed to clean old tickers", e);
        }
    }

    public Optional<Ticker> latest(String exchange, String symbol) {
        return Optional.ofNullable(latest.get(key(exchange, symbol)));
    }

    public List<Ticker> history(String exchange, String symbol) {
        Deque<Ticker> deque = history.get(key(exchange, symbol));
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

    public Optional<IndicatorValues> indicators(String exchange, String symbol) {
        IndicatorState state = indicators.get(key(exchange, symbol));
        return state == null ? Optional.empty() : Optional.ofNullable(state.snapshot());
    }

    private static String key(String exchange, String symbol) {
        return exchange + ":" + symbol;
    }
}
