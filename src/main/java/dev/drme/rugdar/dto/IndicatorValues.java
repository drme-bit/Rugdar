package dev.drme.rugdar.dto;

import java.math.BigDecimal;

public record IndicatorValues(
        BigDecimal lastPrice,
        double sma,
        double ema
) {
}
