package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateCobolProcessRecipeTest {

    private static final String COBOL_SAMPLE = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE1.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            01 CUSTOMER-RATING PIC 9(2).
            PROCEDURE DIVISION.
            MAIN-PARA.
                PERFORM PREP-PARA.
                MOVE 'JOHN' TO CUSTOMER-NAME.
                IF CUSTOMER-RATING > 80
                    MOVE 'VIP' TO CUSTOMER-NAME
                ELSE
                    MOVE 'STANDARD' TO CUSTOMER-NAME
                END-IF.
                EVALUATE CUSTOMER-RATING
                    WHEN 1
                        MOVE 'BRONZE' TO CUSTOMER-NAME
                    WHEN OTHER
                        MOVE 'PLATINUM' TO CUSTOMER-NAME
                END-EVALUATE.
                GOBACK.
            END-PARA.
            PREP-PARA.
                MOVE 'INIT' TO CUSTOMER-NAME.
            END-PARA.
            """;

    @Test
    void shouldPopulateProcessMethodWithCobolLogic() {
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(COBOL_SAMPLE);

        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;

        JavaParser javaParser = JavaParser.fromJavaVersion().build();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);

        List<org.openrewrite.SourceFile> sources = javaParser.parse(ctx, javaSource)
                .collect(java.util.stream.Collectors.toList());
        PopulateCobolProcessRecipe recipe = new PopulateCobolProcessRecipe();

        // Adapt to OpenRewrite LargeSourceSet API
        org.openrewrite.LargeSourceSet lss = new org.openrewrite.internal.InMemoryLargeSourceSet(sources);
        var run = recipe.run(lss, ctx);
        List<Result> results = run.getChangeset().getAllResults();

        assertThat(results).hasSize(1);
        String updated = results.get(0).getAfter().printAll();
        assertThat(updated).contains("output.setCustomerName(\"JOHN\");");
        assertThat(updated).contains("if (input.getCustomerRating() > 80)");
        assertThat(updated).contains("switch (input.getCustomerRating()) {");
        assertThat(updated).contains("case 1 -> {");
        assertThat(updated).contains("output.setCustomerName(\"BRONZE\");");
        assertThat(updated).contains("output.setCustomerName(\"PLATINUM\");");
        assertThat(updated).doesNotContain("TODO");
    }

    @Test
    void shouldInlinePerformParagraphs() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE2.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM PREP-PARA.
                    MOVE 'READY' TO CUSTOMER-NAME.
                    GOBACK.
                PREP-PARA.
                    MOVE 'INIT' TO CUSTOMER-NAME.
                    GOBACK.
                """;

        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);

        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;

        JavaParser javaParser = JavaParser.fromJavaVersion().build();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);

        List<org.openrewrite.SourceFile> sources = javaParser.parse(ctx, javaSource)
                .collect(java.util.stream.Collectors.toList());
        PopulateCobolProcessRecipe recipe = new PopulateCobolProcessRecipe();

        org.openrewrite.LargeSourceSet lss = new org.openrewrite.internal.InMemoryLargeSourceSet(sources);
        var run = recipe.run(lss, ctx);
        List<Result> results = run.getChangeset().getAllResults();

        assertThat(results).hasSize(1);
        String updated = results.get(0).getAfter().printAll();
        assertThat(updated).contains("output.setCustomerName(\"INIT\");");
        assertThat(updated).contains("output.setCustomerName(\"READY\");");
        assertThat(updated).doesNotContain("PERFORM");
    }
}
