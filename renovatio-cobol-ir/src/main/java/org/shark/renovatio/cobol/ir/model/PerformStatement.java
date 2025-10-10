package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public final class PerformStatement implements CobolStatement {

    private final String paragraph;
    private final String throughParagraph;

    public PerformStatement(String paragraph, String throughParagraph) {
        this.paragraph = Objects.requireNonNull(paragraph, "paragraph").toUpperCase();
        this.throughParagraph = throughParagraph != null ? throughParagraph.toUpperCase() : null;
    }

    public String getParagraph() {
        return paragraph;
    }

    public String getThroughParagraph() {
        return throughParagraph;
    }
}
