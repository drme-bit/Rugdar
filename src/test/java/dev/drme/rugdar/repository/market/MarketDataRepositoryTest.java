package dev.drme.rugdar.repository.market;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import dev.drme.rugdar.dto.MarketSummary;
import dev.drme.rugdar.dto.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class MarketDataRepositoryTest {

    private JdbcTemplate jdbc;
    private MarketDataRepository repository;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        repository = new MarketDataRepository(jdbc);
    }

    @Test
    void saveAllDelegatesToBatchUpdate() {
        Ticker t1 = ticker("bybit", "BTCUSDT", "100000");
        Ticker t2 = ticker("binance", "ETHUSDT", "3000");

        repository.saveAll(List.of(t1, t2));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> captor = ArgumentCaptor.forClass(List.class);
        verify(jdbc).batchUpdate(anyString(), captor.capture());

        List<Object[]> rows = captor.getValue();
        assertThat(rows).hasSize(2);

        Object[] first = rows.getFirst();
        assertThat(first[0]).isEqualTo(t1.id());
        assertThat(first[1]).isEqualTo("bybit");
        assertThat(first[2]).isEqualTo("BTCUSDT");
        assertThat(first[6]).isEqualTo(Timestamp.from(t1.timestamp()));
    }

    @Test
    void deleteOlderThanReturnsAffectedRows() {
        when(jdbc.update(anyString(), eq(7))).thenReturn(42);

        int removed = repository.deleteOlderThan(7);

        assertThat(removed).isEqualTo(42);
        verify(jdbc).update(
                "",
                7);
    }

    @Test
    void summarizeMapsResultCorrectly() {
        MarketSummary expected = new MarketSummary(
                "bybit", "BTCUSDT",
                new BigDecimal("63000"), new BigDecimal("62000"),
                new BigDecimal("64000"), new BigDecimal("63000"),
                new BigDecimal("1000"), new BigDecimal("63000000"),
                50);

        when(jdbc.query(anyString(), anyRowMapper(), eq(7)))
                .thenReturn(List.of(expected));

        List<MarketSummary> result = repository.summarize(7);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().exchange()).isEqualTo("bybit");
        assertThat(result.getFirst().symbol()).isEqualTo("BTCUSDT");
        assertThat(result.getFirst().samples()).isEqualTo(50);
    }

    private static Ticker ticker(String exchange, String symbol, String price) {
        return new Ticker(
                UUID.randomUUID(), exchange, symbol,
                new BigDecimal(price), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }

    @SuppressWarnings("unchecked")
    private static <T> RowMapper<T> anyRowMapper() {
        return any(RowMapper.class);
    }
}
