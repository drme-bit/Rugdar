package dev.drme.rugdar.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Ticker(
        UUID id,
        String exchange,
        String symbol,
        BigDecimal lastPrice,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal volume,
        BigDecimal turnover,
        Instant timestamp
) {
    public Ticker(
            String exchange,
            String symbol,
            BigDecimal lastPrice,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal volume,
            BigDecimal turnover,
            Instant timestamp) {
        this(null, exchange, symbol, lastPrice, open, high, low, volume, turnover, timestamp);
    }
}
