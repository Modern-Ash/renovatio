package org.shark.renovatio.provider.cobol.translation;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.recipes.PopulateCobolProcessRecipe;
import org.shark.renovatio.provider.java.OpenRewriteRunResult;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CobolSemanticTranspiler {

    private final OpenRewriteRunner runner;

    public CobolSemanticTranspiler(OpenRewriteRunner runner) {
        this.runner = runner;
    }

    public String enrichServiceImplementation(String javaSource, CobolIntermediateModel model) {
        if (javaSource == null || javaSource.isBlank() || model == null) {
            return javaSource;
        }
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);

        JavaParser javaParser = JavaParser.fromJavaVersion()
                .logCompilationWarningsAndErrors(false)
                .build();
        List<SourceFile> sources = javaParser.parse(ctx, javaSource).collect(java.util.stream.Collectors.toList());

        OpenRewriteRunResult runResult = runner.runRecipe(new PopulateCobolProcessRecipe(), ctx, sources);
        if (!runResult.getValidationErrors().isEmpty() || runResult.getResults().isEmpty()) {
            return javaSource;
        }
        Result first = runResult.getResults().get(0);
        return first.getAfter() != null ? first.getAfter().printAll() : javaSource;
    }
}
