package org.shark.renovatio.llm.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.LLMProvider;
import org.shark.renovatio.llm.domain.LLMServiceUnavailableException;
import org.shark.renovatio.llm.domain.LLMTimeoutException;
import org.shark.renovatio.llm.domain.TypedProposal;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.LLMProviderException;
import org.shark.renovatio.llm.domain.ProposalMetadata;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Circuit breaker wrapper for LLM providers using Resilience4j.
 * Provides circuit breaker, retry, and rate limiting.
 */
public class CircuitBreakerLLMProvider implements LLMProvider {

    private final LLMProvider delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final RateLimiter rateLimiter;
    private final BudgetEnforcer budgetEnforcer;

    public CircuitBreakerLLMProvider(
            LLMProvider delegate,
            LLMConfig config,
            BudgetEnforcer budgetEnforcer) {
        this.delegate = delegate;
        this.budgetEnforcer = budgetEnforcer;

        // Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold((float) (config.circuitBreaker().failureThreshold() * 100))
            .minimumNumberOfCalls(config.circuitBreaker().minimumCalls())
            .waitDurationInOpenState(config.circuitBreaker().openTimeout())
            .permittedNumberOfCallsInHalfOpenState(config.circuitBreaker().halfOpenRequests())
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreakerRegistry.of(cbConfig).circuitBreaker("llm-provider");

        // Retry
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(LLMTimeoutException.class, LLMServiceUnavailableException.class)
            .build();
        this.retry = Retry.of("llm-provider", retryConfig);

        // Rate Limiter
        RateLimiterConfig rlConfig = RateLimiterConfig.custom()
            .limitForPeriod(config.budget().rateLimitPerMinute())
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        this.rateLimiter = RateLimiter.of("llm-provider", rlConfig);
    }

    @Override
    public TypedProposal propose(ProposalRequest request) {
        // Check budget before attempting - convert ProposalRequest.Budget to LLMConfig.Budget
        LLMConfig.Budget configBudget = new LLMConfig.Budget(
            request.budget().maxTokens(),
            0.50, // default cost budget
            10    // default rate limit
        );
        budgetEnforcer.checkBudget(configBudget, estimateTokens(request));

        // Decorate with resilience patterns
        Supplier<TypedProposal> decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter,
            Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker,
                    () -> delegate.propose(request)
                )
            )
        );

        try {
            TypedProposal proposal = decoratedSupplier.get();
            budgetEnforcer.recordUsage(configBudget, proposal.metadata().tokensUsed(), 0.0);
            return proposal;
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            throw new LLMServiceUnavailableException("Circuit breaker open for LLM provider", e);
        } catch (io.github.resilience4j.ratelimiter.RequestNotPermitted e) {
            throw new LLMServiceUnavailableException("Rate limiter rejected LLM request", e);
        } catch (Exception e) {
            if (e instanceof LLMProviderException) {
                throw (LLMProviderException) e;
            }
            throw new LLMProviderException("LLM provider error: " + e.getMessage(), "PROVIDER_ERROR", true, e);
        }
    }

    private int estimateTokens(ProposalRequest request) {
        // Rough estimation: input context size / 4 + budget max tokens / 2
        String contextStr = request.input().context().toString();
        return Math.min(contextStr.length() / 4 + request.budget().maxTokens() / 2, request.budget().maxTokens());
    }

    @Override
    public ProposalMetadata metadata() {
        return delegate.metadata();
    }

    @Override
    public void configure(LLMConfig config) {
        delegate.configure(config);
    }

    @Override
    public boolean isHealthy() {
        return circuitBreaker.getState() != CircuitBreaker.State.OPEN && delegate.isHealthy();
    }
}
