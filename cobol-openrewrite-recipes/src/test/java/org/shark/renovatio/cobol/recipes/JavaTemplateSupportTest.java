package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaTemplateSupportTest {

    @Test
    void replaceMethodBody_changes_body_using_template() {
        String src = "package p; public class T { public int f(){ return 0; } }";
        JavaParser parser = JavaParser.fromJavaVersion().build();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        List<org.openrewrite.SourceFile> sources = parser.parse(ctx, src).toList();

        JavaIsoVisitor<ExecutionContext> v = new JavaIsoVisitor<>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                if ("f".equals(method.getSimpleName())) {
                    return JavaTemplateSupport.replaceMethodBody(getCursor(), method, "{ return 42; }");
                }
                return super.visitMethodDeclaration(method, executionContext);
            }
        };

        // Apply the visitor to the only source file
        J.CompilationUnit cu = (J.CompilationUnit) sources.get(0);
        J.CompilationUnit after = (J.CompilationUnit) v.visit(cu, ctx);
        String updated = after.printAll();
        assertThat(updated).contains("return 42;");
        assertThat(updated).doesNotContain("return 0;");
    }
}
