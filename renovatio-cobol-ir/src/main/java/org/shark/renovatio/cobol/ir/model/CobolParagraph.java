package org.shark.renovatio.cobol.ir.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class CobolParagraph {

    private final String name;
    private final List<CobolStatement> statements;

    public CobolParagraph(String name, List<CobolStatement> statements) {
        this.name = Objects.requireNonNull(name, "name").toUpperCase();
        this.statements = Collections.unmodifiableList(new ArrayList<>(statements == null ? List.of() : statements));
    }

    public String getName() {
        return name;
    }

    public List<CobolStatement> getStatements() {
        return statements;
    }

    public static CobolParagraph empty(String name) {
        return new CobolParagraph(name, List.of());
    }
}
