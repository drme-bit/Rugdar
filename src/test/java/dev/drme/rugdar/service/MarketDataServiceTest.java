package dev.drme.rugdar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.drme.rugdar.service.IdService;
import org.junit.jupiter.api.Test;
import dev.drme.rugdar.dto.Ticker;
import dev.drme.rugdar.service.MarketService.MarketDataService;

class MarketDataServiceTest {

    private final List<Object> published = new ArrayList<>();
    private final MarketDataService service =
            new MarketDataService(new IdService(0), published::add);

    @Test
    void storesLatestTickerPerExchangeSymbol() {
        service.onTicker(ticker("bybit", "BTCUSDT", "100"));
        service.onTicker(ticker("bybit", "BTCUSDT", "101"));
        service.onTicker(ticker("binance", "BTCUSDT", "102"));

        assertThat(service.latest("bybit", "BTCUSDT"))
                .get()
                .extracting(Ticker::lastPrice)
                .satisfies(price -> assertThat(price).isEqualByComparingTo("101"));
        assertThat(service.latest("binance", "BTCUSDT"))
                .get()
                .extracting(Ticker::lastPrice)
                .satisfies(price -> assertThat(price).isEqualByComparingTo("102"));
    }

    @Test
    void latestReturnsEmptyWhenNothingReceived() {
        assertThat(service.latest("bybit", "BTCUSDT")).isEmpty();
    }

    @Test
    void historyKeepsArrivalOrder() {
        service.onTicker(ticker("bybit", "BTCUSDT", "1"));
        service.onTicker(ticker("bybit", "BTCUSDT", "2"));
        service.onTicker(ticker("bybit", "BTCUSDT", "3"));

        assertThat(service.history("bybit", "BTCUSDT"))
                .extracting(t -> t.lastPrice().toString())
                .containsExactly("1", "2", "3");
    }

    @Test
    void historyIsBoundedToLimit() {
        for (int i = 1; i <= 250; i++) {
            service.onTicker(ticker("bybit", "BTCUSDT", String.valueOf(i)));
        }

        List<Ticker> history = service.history("bybit", "BTCUSDT");
        assertThat(history).hasSize(200);
        assertThat(history.get(0).lastPrice()).isEqualByComparingTo("51");
        assertThat(history.get(199).lastPrice()).isEqualByComparingTo("250");
    }

    @Test
    void historyReturnsEmptyForUnknownKey() {
        assertThat(service.history("bybit", "DOGEUSDT")).isEmpty();
    }

    @Test
    void allLatestReturnsEveryKey() {
        service.onTicker(ticker("bybit", "BTCUSDT", "100"));
        service.onTicker(ticker("whitebit", "ETHUSDT", "2000"));

        assertThat(service.allLatest()).hasSize(2);
    }

    @Test
    void enrichesRawTickerWithIdAndRepublishes() {
        service.onTicker(ticker("bybit", "BTCUSDT", "100"));

        Ticker stored = service.latest("bybit", "BTCUSDT").orElseThrow();
        assertThat(stored.id()).isNotNull();
        assertThat(published).hasSize(1);
        assertThat(((Ticker) published.get(0)).id()).isEqualTo(stored.id());
    }

    @Test
    void alreadyEnrichedTickerIsNotRepublished() {
        Ticker raw = ticker("bybit", "BTCUSDT", "100");
        Ticker enriched = new Ticker(
                UUID.randomUUID(),
                raw.exchange(), raw.symbol(),
                raw.lastPrice(), raw.open(), raw.high(), raw.low(),
                raw.volume(), raw.turnover(), raw.timestamp());

        service.onTicker(enriched);

        assertThat(published).isEmpty();
    }

    private static Ticker ticker(String exchange, String symbol, String last) {
        return new Ticker(
                exchange, symbol,
                new BigDecimal(last), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }
}
