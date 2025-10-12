package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record PerformStatement(String paragraph, String throughParagraph) implements CobolStatement {

    public PerformStatement(String paragraph, String throughParagraph) {
        this.paragraph = Objects.requireNonNull(paragraph, "paragraph").toUpperCase();
        this.throughParagraph = throughParagraph != null ? throughParagraph.toUpperCase() : null;
    }
}
