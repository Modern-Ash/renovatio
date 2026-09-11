package org.shark.renovatio.llm.runtime.config;

import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.LLMProvider;
import org.shark.renovatio.llm.provider.FakeLLMProvider;
import org.shark.renovatio.llm.provider.GeminiLLMProvider;
import org.shark.renovatio.llm.resilience.BudgetEnforcer;
import org.shark.renovatio.llm.resilience.CircuitBreakerLLMProvider;
import org.shark.renovatio.llm.security.PromptSanitizer;
import org.shark.renovatio.llm.security.DataMinimizer;
import org.shark.renovatio.llm.security.GovernanceRedactor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;

@Configuration
@ConfigurationProperties(prefix = "renovatio.llm")
public class LLMConfigLoader {

    private String provider = "fake";
    private String model = "gemini-2.0-flash";
    private long timeout = 5000;
    private int maxRetries = 2;
    private Budget budget = new Budget();
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();
    private boolean offlineDefault = true;

    public static class Budget {
        private int maxTokens = 10000;
        private double maxCostUSD = 0.50;
        private int rateLimitPerMinute = 10;

        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public double getMaxCostUSD() { return maxCostUSD; }
        public void setMaxCostUSD(double maxCostUSD) { this.maxCostUSD = maxCostUSD; }
        public int getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
    }

    public static class CircuitBreakerConfig {
        private double failureThreshold = 0.5;
        private int minimumCalls = 10;
        private long openTimeout = 30000;
        private int halfOpenRequests = 3;

        public double getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(double failureThreshold) { this.failureThreshold = failureThreshold; }
        public int getMinimumCalls() { return minimumCalls; }
        public void setMinimumCalls(int minimumCalls) { this.minimumCalls = minimumCalls; }
        public long getOpenTimeout() { return openTimeout; }
        public void setOpenTimeout(long openTimeout) { this.openTimeout = openTimeout; }
        public int getHalfOpenRequests() { return halfOpenRequests; }
        public void setHalfOpenRequests(int halfOpenRequests) { this.halfOpenRequests = halfOpenRequests; }
    }

    // Getters and setters
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Budget getBudget() { return budget; }
    public void setBudget(Budget budget) { this.budget = budget; }
    public CircuitBreakerConfig getCircuitBreaker() { return circuitBreaker; }
    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) { this.circuitBreaker = circuitBreaker; }
    public boolean isOfflineDefault() { return offlineDefault; }
    public void setOfflineDefault(boolean offlineDefault) { this.offlineDefault = offlineDefault; }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "renovatio.llm.provider", havingValue = "fake", matchIfMissing = true)
    public LLMProvider fakeProvider() {
        FakeLLMProvider provider = new FakeLLMProvider();
        provider.configure(toLLMConfig());
        return provider;
    }

    @Bean
    @ConditionalOnProperty(name = "renovatio.llm.provider", havingValue = "gemini")
    public LLMProvider geminiProvider(
            PromptSanitizer promptSanitizer,
            DataMinimizer dataMinimizer,
            GovernanceRedactor governanceRedactor,
            BudgetEnforcer budgetEnforcer) {
        GeminiLLMProvider provider = new GeminiLLMProvider(
            toLLMConfig(), promptSanitizer, dataMinimizer, governanceRedactor);
        return new CircuitBreakerLLMProvider(provider, toLLMConfig(), budgetEnforcer);
    }

    public LLMConfig toLLMConfig() {
        LLMConfig.Budget budgetConfig = new LLMConfig.Budget(
            budget.getMaxTokens(),
            budget.getMaxCostUSD(),
            budget.getRateLimitPerMinute()
        );
        LLMConfig.CircuitBreakerConfig cbConfig = new LLMConfig.CircuitBreakerConfig(
            circuitBreaker.getFailureThreshold(),
            circuitBreaker.getMinimumCalls(),
            java.time.Duration.ofMillis(circuitBreaker.getOpenTimeout()),
            circuitBreaker.getHalfOpenRequests()
        );
        return new LLMConfig(
            provider,
            model,
            java.time.Duration.ofMillis(timeout),
            maxRetries,
            budgetConfig,
            cbConfig,
            offlineDefault
        );
    }
}
