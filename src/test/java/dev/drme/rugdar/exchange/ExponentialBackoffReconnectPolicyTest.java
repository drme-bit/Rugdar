package dev.drme.rugdar.exchange;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import dev.drme.rugdar.exchange.base.ExponentialBackoffReconnectPolicy;

class ExponentialBackoffReconnectPolicyTest {

    private final ExponentialBackoffReconnectPolicy policy = new ExponentialBackoffReconnectPolicy();

    @Test
    void delaysGrowExponentiallyThenCapAtMaxExponent() {
        assertThat(policy.nextDelaySeconds(1)).isEqualTo(5);
        assertThat(policy.nextDelaySeconds(2)).isEqualTo(10);
        assertThat(policy.nextDelaySeconds(3)).isEqualTo(20);
        assertThat(policy.nextDelaySeconds(4)).isEqualTo(40);
        assertThat(policy.nextDelaySeconds(5)).isEqualTo(60);
        assertThat(policy.nextDelaySeconds(100)).isEqualTo(60);
    }

    @Test
    void givesUpAfterMaxAttempts() {
        assertThat(policy.shouldGiveUp(10)).isFalse();
        assertThat(policy.shouldGiveUp(11)).isTrue();
        assertThat(policy.maxAttempts()).isEqualTo(10);
    }
}
