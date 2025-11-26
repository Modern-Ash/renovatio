package org.shark.renovatio.cobol.ir.model;

import lombok.Getter;

import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a COBOL program that has been normalised into a
 * structure convenient for downstream translators.
 *
 * <p>This model now includes support for detecting and representing control break patterns,
 * which are common in COBOL batch programs that process ISAM/sequential files with
 * grouping and aggregation logic.
 *
 * <p>The {@link #controlBreakPatterns} field contains detected patterns that can be
 * decomposed into modern architectural components (repositories, services, aggregators).
 *
 * @see ControlBreakPattern
 * @see DecomposedBusinessLogic
 */
@Getter
public final class CobolIntermediateModel {

    private final String programId;
    private final Map<String, CobolParagraph> paragraphs;
    private final List<CobolDataItem> dataItems;
    private final ControlFlowGraph controlFlowGraph;
    private final CobolExecutionContext executionContext;
    private final List<ControlBreakPattern> controlBreakPatterns;
    private final DecomposedBusinessLogic decomposedLogic;

    private CobolIntermediateModel(Builder builder) {
        this.programId = builder.programId;
        this.paragraphs = Collections.unmodifiableMap(new LinkedHashMap<>(builder.paragraphs));
        this.dataItems = List.copyOf(builder.dataItems);
        this.controlFlowGraph = builder.controlFlowGraph;
        this.executionContext = builder.executionContext;
        this.controlBreakPatterns = List.copyOf(builder.controlBreakPatterns);
        this.decomposedLogic = builder.decomposedLogic;
    }

    /**
     * Returns true if this program contains control break patterns.
     */
    public boolean hasControlBreakPatterns() {
        return !controlBreakPatterns.isEmpty();
    }

    /**
     * Returns true if this program's logic has been decomposed into reusable components.
     */
    public boolean hasDecomposedLogic() {
        return decomposedLogic != null;
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
        private List<ControlBreakPattern> controlBreakPatterns = new ArrayList<>();
        private DecomposedBusinessLogic decomposedLogic;

        private Builder() {
        }

        public Builder programId(String programId) {
            this.programId = Objects.requireNonNull(programId, "programId").toUpperCase();
            return this;
        }

        public Builder addParagraph(CobolParagraph paragraph) {
            Objects.requireNonNull(paragraph, "paragraph");
            this.paragraphs.put(paragraph.name().toUpperCase(), paragraph);
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

        public Builder controlBreakPatterns(List<ControlBreakPattern> patterns) {
            this.controlBreakPatterns = patterns == null ? new ArrayList<>() : new ArrayList<>(patterns);
            return this;
        }

        public Builder addControlBreakPattern(ControlBreakPattern pattern) {
            if (pattern != null) {
                this.controlBreakPatterns.add(pattern);
            }
            return this;
        }

        public Builder decomposedLogic(DecomposedBusinessLogic decomposedLogic) {
            this.decomposedLogic = decomposedLogic;
            return this;
        }

        public CobolIntermediateModel build() {
            Objects.requireNonNull(programId, "programId");
            return new CobolIntermediateModel(this);
        }
    }
}
