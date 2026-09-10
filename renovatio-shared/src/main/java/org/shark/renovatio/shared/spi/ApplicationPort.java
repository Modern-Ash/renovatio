package org.shark.renovatio.shared.spi;

import java.util.List;
import java.util.Map;

/**
 * Interface for application-level orchestration and provider registration.
 * This port defines the contract for managing providers and executing generation requests.
 */
public interface ApplicationPort {

    /**
     * Register a language provider.
     *
     * @param provider the provider to register
     * @throws IllegalStateException if a provider for the same language is already registered
     */
    void registerProvider(LanguageProvider provider);

    /**
     * Register an emitter for a specific language.
     *
     * @param language the language identifier
     * @param emitter the emitter to register
     */
    void registerEmitter(String language, Emitter emitter);

    /**
     * Execute a generation request.
     *
     * @param request the generation request
     * @return the generation result
     */
    GenerationResult generate(GenerationRequest request);

    /**
     * Get all registered providers.
     *
     * @return list of registered providers
     */
    List<LanguageProvider> getRegisteredProviders();

    /**
     * Get a provider by language.
     *
     * @param language the language identifier
     * @return the provider, or null if not found
     */
    LanguageProvider getProvider(String language);

    /**
     * Check if a provider is registered for the given language.
     *
     * @param language the language identifier
     * @return true if a provider is registered
     */
    boolean hasProvider(String language);
}