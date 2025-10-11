package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.Objects;

@Value
public final class PerformStatement implements CobolStatement {

    String paragraph;
    String throughParagraph;

    public PerformStatement(String paragraph, String throughParagraph) {
        this.paragraph = Objects.requireNonNull(paragraph, "paragraph").toUpperCase();
        this.throughParagraph = throughParagraph != null ? throughParagraph.toUpperCase() : null;
    }
}
