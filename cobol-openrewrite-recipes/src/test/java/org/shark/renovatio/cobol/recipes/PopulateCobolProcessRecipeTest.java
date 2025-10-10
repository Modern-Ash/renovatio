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
                MOVE 'JOHN' TO CUSTOMER-NAME.
                IF CUSTOMER-RATING > 80
                    MOVE 'VIP' TO CUSTOMER-NAME
                ELSE
                    MOVE 'STANDARD' TO CUSTOMER-NAME
                END-IF.
                GOBACK.
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

        List<J.CompilationUnit> cus = javaParser.parse(ctx, javaSource);
        PopulateCobolProcessRecipe recipe = new PopulateCobolProcessRecipe();
        List<Result> results = recipe.run(cus, ctx).getChangeset().getAllResults();

        assertThat(results).hasSize(1);
        String updated = results.get(0).getAfter().printAll();
        assertThat(updated).contains("output.setCustomerName('JOHN');");
        assertThat(updated).contains("if (input.getCustomerRating() > 80)");
        assertThat(updated).doesNotContain("TODO");
    }
}
