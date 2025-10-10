package org.shark.renovatio.cobol.recipes;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import java.util.Collections;

final class JavaTemplateSupport {

    private JavaTemplateSupport() {
    }

    static J.Block applyTemplate(Cursor cursor, J.Block body, String templateSource) {
        JavaTemplate template = JavaTemplate.builder(templateSource)
                .build();
        return (J.Block) template.apply(cursor, body.getCoordinates().replace(), new Object[0]);
    }
}
