package org.shark.renovatio.shared.llm;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for an LLM provider.
 */
public record LLMConfig(
    String provider,
    String model,
    Duration timeout,
    int maxRetries,
    Budget budget,
    CircuitBreakerConfig circuitBreaker,
    boolean offlineDefault
) {

    public record Budget(
        int maxTokens,
        double maxCostUSD,
        int rateLimitPerMinute
    ) {}

    public record CircuitBreakerConfig(
        double failureThreshold,
        int minimumCalls,
        Duration openTimeout,
        int halfOpenRequests
    ) {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String provider = "fake";
        private String model = "gemini-2.0-flash";
        private Duration timeout = Duration.ofSeconds(5);
        private int maxRetries = 2;
        private Budget budget = new Budget(10000, 0.50, 10);
        private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig(0.5, 10, Duration.ofSeconds(30), 3);
        private boolean offlineDefault = true;

        public Builder provider(String p) { this.provider = p; return this; }
        public Builder model(String m) { this.model = m; return this; }
        public Builder timeout(Duration t) { this.timeout = t; return this; }
        public Builder maxRetries(int r) { this.maxRetries = r; return this; }
        public Builder budget(Budget b) { this.budget = b; return this; }
        public Builder circuitBreaker(CircuitBreakerConfig c) { this.circuitBreaker = c; return this; }
        public Builder offlineDefault(boolean o) { this.offlineDefault = o; return this; }

        public LLMConfig build() {
            return new LLMConfig(provider, model, timeout, maxRetries, budget, circuitBreaker, offlineDefault);
        }
    }
}