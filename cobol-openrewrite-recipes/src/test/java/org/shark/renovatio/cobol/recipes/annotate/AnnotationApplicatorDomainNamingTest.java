package org.shark.renovatio.cobol.recipes.annotate;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationApplicatorDomainNamingTest {

    private static final String DTO = """
            package sample;
            public class SampleDto {
                private String customerName;
                public String getCustomerName() { return customerName; }
                public void setCustomerName(String v) { this.customerName = v; }
            }
            """;

    @Test
    void renamesFieldAndAccessors() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.domainNaming("clientFullName");

        String printed = new AnnotationApplicator(fixture.model(), fixture.sidecar())
                .apply(parse(ctx, DTO), ctx).tree().printAll();

        assertThat(printed).contains("private String clientFullName");
        assertThat(printed).contains("public String getClientFullName()");
        assertThat(printed).contains("public void setClientFullName(");
        assertThat(printed).doesNotContain("customerName");
        assertThat(printed).doesNotContain("CustomerName");
    }

    @Test
    void dropsRenameOnCollisionAndLeavesSourceUnchanged() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        String dtoWithClash = DTO.replace("private String customerName;",
                "private String customerName;\n    private String clientFullName;");
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.domainNaming("clientFullName");

        AnnotationApplicationOutcome outcome = new AnnotationApplicator(fixture.model(), fixture.sidecar())
                .apply(parse(ctx, dtoWithClash), ctx);

        assertThat(outcome.tree().printAll()).contains("private String customerName");
        assertThat(outcome.dropped()).anySatisfy(dropped ->
                assertThat(dropped.reason()).isEqualTo(DroppedAnnotation.DropReason.NAME_COLLISION));
    }

    @Test
    void dropsInvalidJavaIdentifier() {
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        AnnotatedFixtures.Fixture fixture = AnnotatedFixtures.domainNaming("not-valid");

        AnnotationApplicationOutcome outcome = new AnnotationApplicator(fixture.model(), fixture.sidecar())
                .apply(parse(ctx, DTO), ctx);

        assertThat(outcome.tree().printAll()).contains("private String customerName");
        assertThat(outcome.dropped()).singleElement().satisfies(dropped ->
                assertThat(dropped.reason()).isEqualTo(DroppedAnnotation.DropReason.NAME_COLLISION));
    }

    private J.CompilationUnit parse(ExecutionContext ctx, String source) {
        return JavaParser.fromJavaVersion().build()
                .parse(ctx, source)
                .map(J.CompilationUnit.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
