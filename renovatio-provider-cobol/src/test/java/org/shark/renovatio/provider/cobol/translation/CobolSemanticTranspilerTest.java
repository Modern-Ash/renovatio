package org.shark.renovatio.provider.cobol.translation;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.CobolIrIdentityProjector;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.recipes.PopulateCobolProcessRecipe;
import org.shark.renovatio.provider.java.OpenRewriteRunResult;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CobolSemanticTranspilerTest {

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
            END-PARA.
            """;

    private static final String JAVA_STUB = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String customerName) { this.customerName = customerName; }
                public Integer getCustomerRating() { return 0; }
            }
            public class SampleService {
                public SampleDto process(SampleDto input) {
                    // TODO: Implement COBOL business logic
                    SampleDto output = new SampleDto();
                    return output;
                }
            }
            """;

    @Test
    void shouldEnrichServiceImplementation() {
        CobolIntermediateModelService modelService = new CobolIntermediateModelService();
        CobolIntermediateModel model = modelService.parse(COBOL_SAMPLE);
        CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

        String enriched = transpiler.enrichServiceImplementation(JAVA_STUB, model);
        assertThat(enriched).contains("output.setCustomerName(\"JOHN\");");
    }

    @Test
    void injectsValidatedAnnotatedContextWhilePreservingLegacyModelReference() {
        CobolIntermediateModel model = new CobolIntermediateModelService().parse(COBOL_SAMPLE);
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                "cobol-ir.v1", new CobolIrIdentityProjector().baseIrHash(model), List.of());
        AnnotatedCobolContext annotated = new AnnotatedCobolContext(model, sidecar);
        CapturingRunner runner = new CapturingRunner();

        new CobolSemanticTranspiler(runner).enrichServiceImplementation(JAVA_STUB, annotated);

        CobolIntermediateModel legacyValue = runner.context.getMessage(PopulateCobolProcessRecipe.CONTEXT_KEY);
        AnnotatedCobolContext annotatedValue = runner.context.getMessage(PopulateCobolProcessRecipe.ANNOTATED_CONTEXT_KEY);
        assertThat(legacyValue).isSameAs(model);
        assertThat(annotatedValue).isSameAs(annotated);
    }

    @Test
    void omitsStaleAnnotatedContextWhilePreservingLegacyModel() {
        CobolIntermediateModel model = new CobolIntermediateModelService().parse(COBOL_SAMPLE);
        AnnotatedCobolContext stale = new AnnotatedCobolContext(model,
                new AnnotatedCobolModel(AnnotatedCobolModel.SCHEMA_VERSION,
                        "cobol-ir.v1", "a".repeat(64), List.of()));
        CapturingRunner runner = new CapturingRunner();

        new CobolSemanticTranspiler(runner).enrichServiceImplementation(JAVA_STUB, stale);

        CobolIntermediateModel legacyValue = runner.context.getMessage(PopulateCobolProcessRecipe.CONTEXT_KEY);
        AnnotatedCobolContext annotatedValue = runner.context.getMessage(PopulateCobolProcessRecipe.ANNOTATED_CONTEXT_KEY);
        assertThat(legacyValue).isSameAs(model);
        assertThat(annotatedValue).isNull();
    }

    private static final class CapturingRunner extends OpenRewriteRunner {
        private ExecutionContext context;

        @Override
        public OpenRewriteRunResult runRecipe(Recipe recipe, ExecutionContext ctx, List<SourceFile> sourceFiles) {
            context = ctx;
            return super.runRecipe(recipe, ctx, sourceFiles);
        }
    }
}
