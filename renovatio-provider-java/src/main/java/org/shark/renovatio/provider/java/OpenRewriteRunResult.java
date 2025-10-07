package org.shark.renovatio.provider.java;

import org.openrewrite.Result;
import org.shark.renovatio.shared.dto.RecipeValidationError;

import java.util.List;

/**
 * Encapsulates the result of running an OpenRewrite recipe, including both the rewrite results and any validation errors.
 */
public class OpenRewriteRunResult {
    private final List<Result> results;
    private final List<RecipeValidationError> validationErrors;

    public OpenRewriteRunResult(List<Result> results, List<RecipeValidationError> validationErrors) {
        this.results = results;
        this.validationErrors = validationErrors;
    }

    public List<Result> getResults() {
        return results;
    }

    public List<RecipeValidationError> getValidationErrors() {
        return validationErrors;
    }
}

