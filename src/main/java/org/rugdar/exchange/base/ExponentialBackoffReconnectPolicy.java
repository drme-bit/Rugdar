package org.rugdar.exchange.base;

public class ExponentialBackoffReconnectPolicy {

    private static final int INITIAL_DELAY_SECONDS = 5;
    private static final int MAX_DELAY_SECONDS = 60;
    private static final int MAX_EXPONENT = 4;
    private static final int MAX_ATTEMPTS = 10;

    public long nextDelaySeconds(int attempt) {
        int exponent = Math.min(attempt - 1, MAX_EXPONENT);
        return Math.min(INITIAL_DELAY_SECONDS * (1 << exponent), MAX_DELAY_SECONDS);
    }

    public boolean shouldGiveUp(int attempt) {
        return attempt > MAX_ATTEMPTS;
    }

    public int maxAttempts() {
        return MAX_ATTEMPTS;
    }
}
