package org.shark.renovatio.cobol.ir.model;

import lombok.Getter;

import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a COBOL program that has been normalised into a
 * structure convenient for downstream translators.
 */
@Getter
public final class CobolIntermediateModel {

    private final String programId;
    private final Map<String, CobolParagraph> paragraphs;
    private final List<CobolDataItem> dataItems;
    private final ControlFlowGraph controlFlowGraph;
    private final CobolExecutionContext executionContext;

    private CobolIntermediateModel(Builder builder) {
        this.programId = builder.programId;
        this.paragraphs = Collections.unmodifiableMap(new LinkedHashMap<>(builder.paragraphs));
        this.dataItems = List.copyOf(builder.dataItems);
        this.controlFlowGraph = builder.controlFlowGraph;
        this.executionContext = builder.executionContext;
    }

    public Optional<CobolParagraph> findParagraph(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(paragraphs.get(name.toUpperCase()));
    }

    public CobolParagraph getEntryParagraph() {
        if (paragraphs.isEmpty()) {
            return CobolParagraph.empty("MAIN");
        }
        return paragraphs.values().iterator().next();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String programId;
        private final Map<String, CobolParagraph> paragraphs = new LinkedHashMap<>();
        private List<CobolDataItem> dataItems = List.of();
        private ControlFlowGraph controlFlowGraph = ControlFlowGraph.empty();
        private CobolExecutionContext executionContext = CobolExecutionContext.empty();

        private Builder() {
        }

        public Builder programId(String programId) {
            this.programId = Objects.requireNonNull(programId, "programId").toUpperCase();
            return this;
        }

        public Builder addParagraph(CobolParagraph paragraph) {
            Objects.requireNonNull(paragraph, "paragraph");
            this.paragraphs.put(paragraph.getName().toUpperCase(), paragraph);
            return this;
        }

        public Builder dataItems(List<CobolDataItem> dataItems) {
            this.dataItems = dataItems == null ? List.of() : List.copyOf(dataItems);
            return this;
        }

        public Builder controlFlowGraph(ControlFlowGraph controlFlowGraph) {
            this.controlFlowGraph = controlFlowGraph == null ? ControlFlowGraph.empty() : controlFlowGraph;
            return this;
        }

        public Builder executionContext(CobolExecutionContext executionContext) {
            this.executionContext = executionContext == null ? CobolExecutionContext.empty() : executionContext;
            return this;
        }

        public CobolIntermediateModel build() {
            Objects.requireNonNull(programId, "programId");
            return new CobolIntermediateModel(this);
        }
    }
}
