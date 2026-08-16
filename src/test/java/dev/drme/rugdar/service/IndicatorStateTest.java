package dev.drme.rugdar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import dev.drme.rugdar.dto.IndicatorValues;
import dev.drme.rugdar.service.MarketService.IndicatorState;
import org.junit.jupiter.api.Test;

class IndicatorStateTest {

    @Test
    void smaIsAverageOfLastPrices() {
        IndicatorState state = new IndicatorState(5, 5);
        for (int i = 1; i <= 5; i++) {
            state.update(price(i));
        }
        IndicatorValues v = state.snapshot();
        assertThat(v).isNotNull();
        assertThat(v.sma()).isEqualTo(3.0);

        for (int i = 6; i <= 10; i++) {
            state.update(price(i));
        }
        assertThat(state.snapshot().sma()).isEqualTo(8.0);
    }

    @Test
    void emaConvergesPerFormula() {
        IndicatorState state = new IndicatorState(3, 3);
        state.update(price(1));
        state.update(price(2));
        state.update(price(3));

        IndicatorValues v = state.snapshot();
        assertThat(v).isNotNull();
        assertThat(v.ema()).isEqualTo(2.25);
    }

    @Test
    void snapshotIsNullBeforeSmaWarmup() {
        IndicatorState state = new IndicatorState(5, 5);
        state.update(price(1));
        state.update(price(2));

        assertThat(state.snapshot()).isNull();
    }

    private static BigDecimal price(int value) {
        return new BigDecimal(value);
    }
}
