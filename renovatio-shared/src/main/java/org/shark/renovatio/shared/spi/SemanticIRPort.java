package org.shark.renovatio.shared.spi;

import java.util.List;

/**
 * Interface for transforming source code through the semantic IR pipeline.
 * This port defines the contract for parse → plan → render operations.
 */
public interface SemanticIRPort {

    /**
     * Transform source code into a semantic model.
     *
     * @param language the source language
     * @param code the source code to transform
     * @return the semantic model
     * @throws ParseException if parsing fails
     */
    SemanticModel transform(String language, SourceCode code) throws ParseException;

    /**
     * Create a generation plan from a semantic model.
     *
     * @param model the semantic model
     * @return the generation plan
     */
    GenerationPlan plan(SemanticModel model);

    /**
     * Render code from a generation plan.
     *
     * @param plan the generation plan
     * @return the generated code
     */
    GeneratedCode render(GenerationPlan plan);

    /**
     * Get the supported languages.
     *
     * @return list of supported language identifiers
     */
    List<String> supportedLanguages();
}