package org.shark.renovatio.shared.spi;

import java.util.List;

/**
 * Interface for parsing source code into a semantic intermediate representation.
 * This is the first stage of the compilation/generation pipeline.
 */
public interface SemanticIRParser {

    /**
     * Parse source code into a semantic model.
     *
     * @param sourceCode the source code to parse
     * @return the parsed semantic model
     * @throws ParseException if parsing fails
     */
    SemanticModel parse(SourceCode sourceCode) throws ParseException;

    /**
     * Validate a semantic model for correctness.
     *
     * @param model the semantic model to validate
     * @return list of validation errors (empty if valid)
     */
    List<SemanticError> validate(SemanticModel model);

    /**
     * Get the supported source language.
     *
     * @return the language identifier (e.g., "java", "cobol")
     */
    String language();

    /**
     * Check if this parser can handle the given source code.
     *
     * @param sourceCode the source code to check
     * @return true if this parser can handle the source
     */
    boolean canParse(SourceCode sourceCode);
}