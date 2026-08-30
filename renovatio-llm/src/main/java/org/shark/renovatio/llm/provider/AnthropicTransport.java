package org.shark.renovatio.llm.provider;

/** Injectable transport seam; production uses HTTP and tests use an offline fake. */
@FunctionalInterface
public interface AnthropicTransport {
    LlmResponse send(LlmRequest request, AnthropicConfiguration configuration);
}
