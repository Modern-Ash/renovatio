package org.shark.renovatio.llm.provider;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

/** Deterministic provider for offline tests and development. */
public final class OfflineFakeProvider implements LlmProvider {
    private final Deque<LlmResponse> responses;
    private int calls;

    public OfflineFakeProvider(Collection<LlmResponse> responses) {
        this.responses = new ArrayDeque<>(responses);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        calls++;
        LlmResponse response = responses.pollFirst();
        if (response == null) {
            throw new ProviderException(ProviderFailure.PROVIDER_UNAVAILABLE);
        }
        return response;
    }

    public int calls() {
        return calls;
    }
}
