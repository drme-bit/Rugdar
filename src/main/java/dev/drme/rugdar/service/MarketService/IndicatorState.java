package dev.drme.rugdar.service.MarketService;

import dev.drme.rugdar.dto.IndicatorValues;
import java.math.BigDecimal;

public class IndicatorState {

    private final double[] window;
    private final int emaPeriod;
    private int idx;
    private int count;
    private double sum;
    private boolean emaInitialized;
    private double prevEma;
    private BigDecimal lastPrice;

    public IndicatorState(int smaPeriod, int emaPeriod) {
        if (smaPeriod <= 0 || emaPeriod <= 0) {
            throw new IllegalArgumentException("Periods must be positive");
        }
        this.window = new double[smaPeriod];
        this.emaPeriod = emaPeriod;
    }

    public synchronized void update(BigDecimal price) {
        double value = price.doubleValue();
        if (count < window.length) {
            sum += value;
        } else {
            sum += value - window[idx];
        }
        window[idx] = value;
        idx = (idx + 1) % window.length;
        count++;

        double k = 2.0 / (emaPeriod + 1);
        if (!emaInitialized) {
            prevEma = value;
            emaInitialized = true;
        } else {
            prevEma = value * k + prevEma * (1 - k);
        }
        lastPrice = price;
    }

    public synchronized IndicatorValues snapshot() {
        if (count < window.length) {
            return null;
        }
        return new IndicatorValues(lastPrice, sum / window.length, prevEma);
    }
}
