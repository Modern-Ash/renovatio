package org.modernash.renovatio.llm.provider;

/** Injectable Ollama transport; production uses HTTP and tests use an offline fake. */
@FunctionalInterface
public interface OllamaTransport {
    LlmResponse send(LlmRequest request, OllamaConfiguration configuration);
}
