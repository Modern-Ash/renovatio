package org.shark.renovatio.cobol.recipes;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

final class JavaTemplateSupport {

    private JavaTemplateSupport() {
    }

    static J.MethodDeclaration replaceMethodBody(Cursor cursor, J.MethodDeclaration method, String templateSource) {
        JavaTemplate template = JavaTemplate.builder(templateSource)
                .build();
        return (J.MethodDeclaration) template.apply(cursor, method.getCoordinates().replaceBody());
    }
}
