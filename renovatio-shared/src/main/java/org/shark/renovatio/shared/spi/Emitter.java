package org.shark.renovatio.shared.spi;

import java.util.Map;

/**
 * Interface for code emitters that transform semantic models to target code.
 */
public interface Emitter {

    /**
     * Emit code from a semantic model.
     *
     * @param model the semantic model
     * @param options emission options
     * @return the generated code
     */
    GeneratedCode emit(SemanticModel model, Map<String, Object> options);

    /**
     * Get the target language for this emitter.
     *
     * @return the target language identifier
     */
    String targetLanguage();

    /**
     * Get the source languages this emitter supports.
     *
     * @return list of supported source language identifiers
     */
    java.util.List<String> supportedSourceLanguages();

    /**
     * Initialize the emitter.
     */
    default void initialize() {
        // Default no-op implementation
    }

    /**
     * Shutdown the emitter.
     */
    default void shutdown() {
        // Default no-op implementation
    }
}