package org.modernash.renovatio.llm.provider;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/** Ollama provider orchestration with deterministic, injectable retry mechanics. */
public final class OllamaLlmProvider implements LlmProvider {
    private final OllamaConfiguration configuration;
    private final OllamaTransport transport;
    private final RetryPolicy retryPolicy;
    private final DoubleSupplier random;
    private final Consumer<Duration> sleeper;

    public OllamaLlmProvider(OllamaConfiguration configuration, OllamaTransport transport,
                             RetryPolicy retryPolicy, DoubleSupplier random,
                             Consumer<Duration> sleeper) {
        this.configuration = Objects.requireNonNull(configuration);
        this.transport = Objects.requireNonNull(transport);
        this.retryPolicy = Objects.requireNonNull(retryPolicy);
        this.random = Objects.requireNonNull(random);
        this.sleeper = Objects.requireNonNull(sleeper);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        for (int attempt = 1; attempt <= RetryPolicy.MAX_ATTEMPTS; attempt++) {
            try {
                return transport.send(request, configuration);
            } catch (ProviderException exception) {
                if (!exception.failure().retryable() || attempt == RetryPolicy.MAX_ATTEMPTS) {
                    throw exception;
                }
                sleeper.accept(retryPolicy.delayBeforeAttempt(attempt + 1, random));
            }
        }
        throw new IllegalStateException("Unreachable retry state");
    }
}
