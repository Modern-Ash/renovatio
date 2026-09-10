package org.shark.renovatio.llm.resilience;

import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.LLMBudgetExceededException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enforces budget limits (tokens, cost, rate) for LLM calls.
 */
@Component
public class BudgetEnforcer {

    private final ConcurrentHashMap<String, RateLimitBucket> rateLimitBuckets = new ConcurrentHashMap<>();

    public void checkBudget(LLMConfig.Budget budget, int estimatedTokens) {
        // Check token budget
        if (estimatedTokens > budget.maxTokens()) {
            throw new LLMBudgetExceededException(
                String.format("Estimated tokens %d exceeds budget %d", estimatedTokens, budget.maxTokens())
            );
        }

        // Check rate limit
        RateLimitBucket bucket = rateLimitBuckets.computeIfAbsent("global", k -> new RateLimitBucket(budget.rateLimitPerMinute()));
        if (!bucket.tryConsume()) {
            throw new LLMBudgetExceededException(
                String.format("Rate limit exceeded: %d requests per minute", budget.rateLimitPerMinute())
            );
        }
    }

    public void recordUsage(LLMConfig.Budget budget, int tokensUsed, double costUSD) {
        // In a real implementation, this would track cumulative usage
        // For now, just validate against per-call limits
        if (tokensUsed > budget.maxTokens()) {
            throw new LLMBudgetExceededException(
                String.format("Actual tokens used %d exceeds budget %d", tokensUsed, budget.maxTokens())
            );
        }
        if (costUSD > budget.maxCostUSD()) {
            throw new LLMBudgetExceededException(
                String.format("Actual cost $%.4f exceeds budget $%.4f", costUSD, budget.maxCostUSD())
            );
        }
    }

    private static class RateLimitBucket {
        private final int maxPermits;
        private final AtomicInteger availablePermits;
        private final AtomicLong lastRefill;

        RateLimitBucket(int maxPermits) {
            this.maxPermits = maxPermits;
            this.availablePermits = new AtomicInteger(maxPermits);
            this.lastRefill = new AtomicLong(Instant.now().toEpochMilli());
        }

        boolean tryConsume() {
            refillIfNeeded();
            int current = availablePermits.get();
            while (current > 0) {
                if (availablePermits.compareAndSet(current, current - 1)) {
                    return true;
                }
                current = availablePermits.get();
            }
            return false;
        }

        private void refillIfNeeded() {
            long now = Instant.now().toEpochMilli();
            long last = lastRefill.get();
            if (now - last >= 60_000) { // 1 minute
                if (lastRefill.compareAndSet(last, now)) {
                    availablePermits.set(maxPermits);
                }
            }
        }
    }
}