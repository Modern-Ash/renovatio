package org.modernash.renovatio.cobol.ir.model;

import lombok.Getter;

import org.modernash.renovatio.cobol.ir.context.CobolExecutionContext;
import org.modernash.renovatio.cobol.ir.flow.ControlFlowGraph;

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
    private final List<CobolDiagnostic> diagnostics;
    private final Map<String, ParagraphLineRange> paragraphLineRanges;
    private final Map<String, String> fileToRecordMapping;
    private final Map<String, List<String>> fileKeyFields;
    private final Map<String, String> fileAssignTarget;

    private CobolIntermediateModel(Builder builder) {
        this.programId = builder.programId;
        this.paragraphs = Collections.unmodifiableMap(new LinkedHashMap<>(builder.paragraphs));
        this.dataItems = List.copyOf(builder.dataItems);
        this.controlFlowGraph = builder.controlFlowGraph;
        this.executionContext = builder.executionContext;
        this.controlBreakPatterns = List.copyOf(builder.controlBreakPatterns);
        this.decomposedLogic = builder.decomposedLogic;
        this.diagnostics = builder.diagnostics.stream().sorted().toList();
        this.paragraphLineRanges = Collections.unmodifiableMap(new LinkedHashMap<>(builder.paragraphLineRanges));
        this.fileToRecordMapping = builder.fileToRecordMapping == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(builder.fileToRecordMapping));
        this.fileKeyFields = builder.fileKeyFields == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(builder.fileKeyFields));
        this.fileAssignTarget = builder.fileAssignTarget == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(builder.fileAssignTarget));
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

    /**
     * Returns the {@link ParagraphLineRange} for a named paragraph, if the parser
     * could determine its lexical span within the source file.
     */
    public Optional<ParagraphLineRange> findParagraphLineRange(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(paragraphLineRanges.get(name.toUpperCase()));
    }

    /**
     * Returns the mapping from FD (file definition) names to 01 level record names
     * in the FILE SECTION. This is used to correctly identify the record structure
     * associated with each file.
     */
    public Map<String, String> getFileToRecordMapping() {
        return fileToRecordMapping;
    }

    /**
     * Returns the mapping from FD (file definition) names to the field
     * name(s) declared as that file's RECORD KEY / ALTERNATE RECORD KEY in
     * FILE-CONTROL — the file's actual VSAM/indexed access key(s).
     */
    public Map<String, List<String>> getFileKeyFields() {
        return fileKeyFields;
    }

    /**
     * Returns the mapping from SELECT (file-control) name to the physical
     * dataset name it is {@code ASSIGN TO} — the same physical VSAM/indexed
     * file is often declared under a different local SELECT name in each
     * program (e.g. ACCT-FILE, ACCOUNT-FILE, ACCTFILE-FILE all {@code ASSIGN
     * TO ACCTFILE}). This is real structural evidence those declarations
     * describe one physical resource, independent of how similar their
     * local names happen to look.
     */
    public Map<String, String> getFileAssignTarget() {
        return fileAssignTarget;
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
        private List<CobolDiagnostic> diagnostics = new ArrayList<>();
        private final Map<String, ParagraphLineRange> paragraphLineRanges = new LinkedHashMap<>();
        private Map<String, String> fileToRecordMapping = new LinkedHashMap<>();
        private Map<String, List<String>> fileKeyFields = new LinkedHashMap<>();
        private Map<String, String> fileAssignTarget = new LinkedHashMap<>();

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

        public Builder diagnostics(List<CobolDiagnostic> diagnostics) {
            this.diagnostics = diagnostics == null ? new ArrayList<>() : new ArrayList<>(diagnostics);
            return this;
        }

        public Builder addDiagnostic(CobolDiagnostic diagnostic) {
            if (diagnostic != null) {
                this.diagnostics.add(diagnostic);
            }
            return this;
        }

        public Builder putParagraphLineRange(String paragraph, ParagraphLineRange range) {
            if (paragraph != null && range != null) {
                this.paragraphLineRanges.put(paragraph.toUpperCase(), range);
            }
            return this;
        }

        public Builder paragraphLineRanges(Map<String, ParagraphLineRange> ranges) {
            if (ranges != null) {
                ranges.forEach(this::putParagraphLineRange);
            }
            return this;
        }

        public Builder fileToRecordMapping(Map<String, String> mapping) {
            this.fileToRecordMapping = mapping == null ? new LinkedHashMap<>() : new LinkedHashMap<>(mapping);
            return this;
        }

        public Builder fileKeyFields(Map<String, List<String>> keyFields) {
            this.fileKeyFields = keyFields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(keyFields);
            return this;
        }

        public Builder fileAssignTarget(Map<String, String> assignTarget) {
            this.fileAssignTarget = assignTarget == null ? new LinkedHashMap<>() : new LinkedHashMap<>(assignTarget);
            return this;
        }

        public CobolIntermediateModel build() {
            Objects.requireNonNull(programId, "programId");
            return new CobolIntermediateModel(this);
        }
    }
}
