package dev.drme.rugdar.dto;

import java.math.BigDecimal;

public record MarketSummary(
        String exchange,
        String symbol,
        BigDecimal lastPrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal avgPrice,
        BigDecimal totalVolume,
        BigDecimal totalTurnover,
        int samples
) {
}
