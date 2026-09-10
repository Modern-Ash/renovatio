package org.shark.renovatio.llm.provider;

import java.time.Duration;
import java.util.function.DoubleSupplier;

/** Three-attempt capped exponential backoff with full jitter. */
public final class RetryPolicy {
    public static final int MAX_ATTEMPTS = 3;
    private static final long BASE_MILLIS = 500;
    private static final long MAX_MILLIS = 5_000;

    public Duration delayBeforeAttempt(int nextAttempt, DoubleSupplier random) {
        if (nextAttempt < 2 || nextAttempt > MAX_ATTEMPTS) {
            throw new IllegalArgumentException("nextAttempt must be 2 or 3");
        }
        long upperBound = Math.min(MAX_MILLIS, BASE_MILLIS << (nextAttempt - 2));
        double sample = random.getAsDouble();
        if (sample < 0.0 || sample >= 1.0) {
            throw new IllegalArgumentException("random sample must be in [0, 1)");
        }
        return Duration.ofMillis((long) Math.floor(sample * (upperBound + 1)));
    }
}
