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
    private final Map<String, CobolRecordLayout> recordLayouts;
    private final Map<String, CobolFileDescriptor> fileDescriptors;
    private final Map<String, String> initialValues;
    private final Map<String, Integer> tableDimensions;
    private final Map<String, CobolRedefinesView> redefinesViews;
    private final java.util.Set<String> ambiguousDataNames;

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
        this.recordLayouts = Collections.unmodifiableMap(new LinkedHashMap<>(builder.recordLayouts));
        this.fileDescriptors = Collections.unmodifiableMap(new LinkedHashMap<>(builder.fileDescriptors));
        this.initialValues = Collections.unmodifiableMap(new LinkedHashMap<>(builder.initialValues));
        this.tableDimensions = Collections.unmodifiableMap(new LinkedHashMap<>(builder.tableDimensions));
        this.redefinesViews = Collections.unmodifiableMap(new LinkedHashMap<>(builder.redefinesViews));
        this.ambiguousDataNames = java.util.Set.copyOf(builder.ambiguousDataNames);
    }

    /** Upper-case data names declared more than once (a qualified OF/IN reference is needed). */
    public java.util.Set<String> getAmbiguousDataNames() {
        return ambiguousDataNames;
    }

    /** REDEFINES views keyed by upper-case item name (see {@link CobolRedefinesView}). */
    public Map<String, CobolRedefinesView> getRedefinesViews() {
        return redefinesViews;
    }

    /**
     * Occurrences of each elementary item declared inside a fixed one-dimensional OCCURS, keyed
     * by upper-case name; 0 marks a table generation does not support (OCCURS DEPENDING ON,
     * variable or multi-dimensional). Scalars are absent.
     */
    public Map<String, Integer> getTableDimensions() {
        return tableDimensions;
    }

    /**
     * VALUE clause of each uniquely named elementary data item, keyed by upper-case name, as the
     * raw COBOL literal (quoted, numeric or figurative).
     */
    public Map<String, String> getInitialValues() {
        return initialValues;
    }

    /** Files declared in FILE-CONTROL, keyed by upper-case SELECT name, in declaration order. */
    public Map<String, CobolFileDescriptor> getFileDescriptors() {
        return fileDescriptors;
    }

    /**
     * Offset and length of a file's RECORD KEY inside its FD record, when the record layout is
     * composable and contains the key (an elementary item or a group of consecutive items).
     */
    public Optional<int[]> recordKeySpan(CobolFileDescriptor file) {
        if (file == null || file.recordKey() == null || file.recordName() == null) {
            return Optional.empty();
        }
        Optional<CobolRecordLayout> record = findComposableLayout(file.recordName());
        if (record.isEmpty()) {
            return Optional.empty();
        }
        String keyName = file.recordKey().toUpperCase();
        List<String> keyFields = findComposableLayout(keyName)
                .map(group -> group.fields().stream().map(CobolRecordLayout.Field::name).toList())
                .orElse(List.of(keyName));
        List<CobolRecordLayout.Field> fields = record.orElseThrow().fields();
        int offset = 0;
        for (int i = 0; i < fields.size(); i++) {
            if (keyFields.get(0) != null && keyFields.get(0).equals(fields.get(i).name())) {
                int length = 0;
                for (int j = i; j < i + keyFields.size() && j < fields.size(); j++) {
                    length += fields.get(j).length();
                }
                return Optional.of(new int[] {offset, length});
            }
            offset += fields.get(i).length();
        }
        return Optional.empty();
    }

    /** True when the program issues EXEC CICS commands (an online, screen-driven program). */
    public boolean usesCics() {
        return paragraphs.values().stream().anyMatch(paragraph -> anyStatement(paragraph.statements(),
                statement -> statement instanceof SimpleStatement simple
                        && simple.text().toUpperCase().startsWith("EXEC CICS")));
    }

    /**
     * True when the run unit can end inside a performed paragraph (GOBACK / STOP RUN outside the
     * entry paragraph) or through CICS (XCTL, RETURN): generated code then needs a way to end
     * the whole program, not just the current method.
     */
    public boolean endsFromPerformedParagraphs() {
        if (usesCics()) {
            return true;
        }
        CobolParagraph entry = getEntryParagraph();
        return paragraphs.values().stream().filter(paragraph -> paragraph != entry)
                .anyMatch(paragraph -> anyStatement(paragraph.statements(),
                        statement -> statement instanceof SimpleStatement simple
                                && (simple.kind() == SimpleStatement.Kind.GOBACK
                                || simple.kind() == SimpleStatement.Kind.STOP_RUN)));
    }

    private static boolean anyStatement(List<CobolStatement> statements,
                                        java.util.function.Predicate<CobolStatement> test) {
        for (CobolStatement statement : statements) {
            if (test.test(statement)) {
                return true;
            }
            List<List<CobolStatement>> nested = new ArrayList<>();
            if (statement instanceof IfStatement branch) {
                nested.add(branch.thenStatements());
                nested.add(branch.elseStatements());
            } else if (statement instanceof EvaluateStatement evaluate) {
                evaluate.branches().forEach(branch -> nested.add(branch.statements()));
            } else if (statement instanceof PerformStatement perform) {
                nested.add(perform.inlineBody());
            } else if (statement instanceof FileOperationStatement file) {
                nested.add(file.exceptionStatements());
                nested.add(file.normalStatements());
            } else if (statement instanceof CallStatement call) {
                nested.add(call.exceptionStatements());
                nested.add(call.normalStatements());
            }
            for (List<CobolStatement> body : nested) {
                if (anyStatement(body, test)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<CobolFileDescriptor> findFile(String selectName) {
        return selectName == null ? Optional.empty()
                : Optional.ofNullable(fileDescriptors.get(selectName.toUpperCase()));
    }

    /**
     * Layout of every group item, keyed by upper-case COBOL name. Only
     * {@link CobolRecordLayout#composable() composable} layouts may be read or written as one
     * string by generated code.
     */
    public Map<String, CobolRecordLayout> getRecordLayouts() {
        return recordLayouts;
    }

    public Optional<CobolRecordLayout> findComposableLayout(String groupName) {
        if (groupName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(recordLayouts.get(groupName.toUpperCase()))
                .filter(CobolRecordLayout::composable);
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
        private final Map<String, CobolRecordLayout> recordLayouts = new LinkedHashMap<>();
        private final Map<String, CobolFileDescriptor> fileDescriptors = new LinkedHashMap<>();
        private final Map<String, String> initialValues = new LinkedHashMap<>();
        private final Map<String, Integer> tableDimensions = new LinkedHashMap<>();
        private final Map<String, CobolRedefinesView> redefinesViews = new LinkedHashMap<>();
        private java.util.Set<String> ambiguousDataNames = java.util.Set.of();

        public Builder ambiguousDataNames(java.util.Set<String> names) {
            this.ambiguousDataNames = names == null ? java.util.Set.of() : names;
            return this;
        }

        public Builder redefinesViews(Map<String, CobolRedefinesView> views) {
            this.redefinesViews.clear();
            if (views != null) {
                this.redefinesViews.putAll(views);
            }
            return this;
        }

        public Builder tableDimensions(Map<String, Integer> dimensions) {
            this.tableDimensions.clear();
            if (dimensions != null) {
                this.tableDimensions.putAll(dimensions);
            }
            return this;
        }

        public Builder initialValues(Map<String, String> values) {
            this.initialValues.clear();
            if (values != null) {
                this.initialValues.putAll(values);
            }
            return this;
        }

        public Builder fileDescriptors(Map<String, CobolFileDescriptor> files) {
            this.fileDescriptors.clear();
            if (files != null) {
                this.fileDescriptors.putAll(files);
            }
            return this;
        }

        public Builder recordLayouts(Map<String, CobolRecordLayout> layouts) {
            this.recordLayouts.clear();
            if (layouts != null) {
                this.recordLayouts.putAll(layouts);
            }
            return this;
        }
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
