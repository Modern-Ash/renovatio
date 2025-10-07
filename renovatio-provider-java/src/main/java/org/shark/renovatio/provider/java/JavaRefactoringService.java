package org.shark.renovatio.provider.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.shark.renovatio.shared.dto.RecipeValidationError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * @deprecated Preserved for backwards compatibility with legacy package names.
 * Prefer {@link org.shark.renovatio.provider.java.OpenRewriteRunner} and
 * associated services from the {@code org.shark.renovatio} namespace.
 */
@Deprecated(forRemoval = false)
public class JavaRefactoringService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaRefactoringService.class);

    private final org.shark.renovatio.provider.java.OpenRewriteRunner delegate;

    public JavaRefactoringService() {
        this(new org.shark.renovatio.provider.java.OpenRewriteRunner());
    }

    JavaRefactoringService(org.shark.renovatio.provider.java.OpenRewriteRunner delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    public List<Result> runRecipe(Recipe recipe, List<SourceFile> sourceFiles, ExecutionContext ctx) {
        OpenRewriteRunResult result = delegate.runRecipe(recipe, ctx, sourceFiles);
        if (!result.getValidationErrors().isEmpty()) {
            StringBuilder sb = new StringBuilder("Recipe validation error(s): ");
            for (RecipeValidationError err : result.getValidationErrors()) {
                sb.append("[Recipe: ").append(err.getRecipeName()).append(", Message: ").append(err.getMessage()).append("] ");
            }
            LOGGER.warn(sb.toString());
            return List.of(); // Devuelve lista vacía en vez de lanzar excepción
        }
        return result.getResults();
    }
}
