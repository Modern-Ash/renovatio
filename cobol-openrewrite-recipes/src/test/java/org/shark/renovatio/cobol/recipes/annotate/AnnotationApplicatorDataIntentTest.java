package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorDataIntentTest {

    private static final String DTO = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
            }
            """;

    private J.CompilationUnit parse(ExecutionContext ctx) {
        return JavaParser.fromJavaVersion().build()
                .parse(ctx, DTO)
                .map(J.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void attachesCobolDataIntentAnnotationToField() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        J.CompilationUnit cu = parse(ctx);
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.redefinesDataIntent();

        AnnotationApplicationOutcome outcome =
                new AnnotationApplicator(f.model(), f.sidecar()).apply(cu, ctx);

        String printed = outcome.tree().printAll();
        assertThat(printed).contains("@CobolDataIntent(");
        assertThat(printed).contains("CobolDataIntent.Construction.REDEFINES");
        assertThat(printed).contains("private String customerName");
        assertThat(outcome.dropped()).isEmpty();
    }

    @Test
    void isDeterministicAcrossRuns() {
        ExecutionContext ctx1 = new InMemoryExecutionContext(Throwable::printStackTrace);
        ExecutionContext ctx2 = new InMemoryExecutionContext(Throwable::printStackTrace);
        AnnotatedFixtures.Fixture f = AnnotatedFixtures.redefinesDataIntent();

        String a = new AnnotationApplicator(f.model(), f.sidecar()).apply(parse(ctx1), ctx1).tree().printAll();
        String b = new AnnotationApplicator(f.model(), f.sidecar()).apply(parse(ctx2), ctx2).tree().printAll();

        assertThat(a).isEqualTo(b);
    }
}
