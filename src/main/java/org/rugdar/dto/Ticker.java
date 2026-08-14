package org.rugdar.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record Ticker(
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
}
