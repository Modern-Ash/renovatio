package org.shark.renovatio.cobol.ir.parser;

import org.shark.renovatio.cobol.ir.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Detects control break patterns in COBOL programs.
 *
 * <p>Control break patterns are characterized by:
 * <ul>
 *   <li>File READ operations in a loop</li>
 *   <li>Comparison of current field value with previous value</li>
 *   <li>Processing of subtotals/group totals when breaks occur</li>
 *   <li>Accumulation of values during detail processing</li>
 * </ul>
 *
 * <p>This detector identifies these patterns and creates {@link ControlBreakPattern}
 * objects that can be used to generate architecture-appropriate code.
 */
public class ControlBreakPatternDetector {

    private static final Logger log = LoggerFactory.getLogger(ControlBreakPatternDetector.class);

    // Pattern to detect "SAVE" or "PREV" or "OLD" prefix/suffix in variable names
    private static final Pattern PREVIOUS_VALUE_PATTERN = Pattern.compile(
            "(SAVE[-_]?|PREV[-_]?|OLD[-_]?|WS[-_]?SAVE[-_]?|WS[-_]?PREV[-_]?)([A-Z0-9-]+)|" +
            "([A-Z0-9-]+)([-_]?SAVE|[-_]?PREV|[-_]?OLD|[-_]?ANTERIOR|[-_]?ANT)",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to detect accumulator variables (TOTAL, SUM, ACUM, COUNT, etc.)
    private static final Pattern ACCUMULATOR_PATTERN = Pattern.compile(
            "(TOTAL|SUM|ACUM|COUNT|CNT|SUBTOTAL|SUMA|CONTADOR|ACUMULADOR)[-_]?([A-Z0-9-]*)|" +
            "([A-Z0-9-]*)[-_]?(TOTAL|SUM|ACUM|COUNT|CNT|SUBTOTAL)",
            Pattern.CASE_INSENSITIVE
    );

    // Common control break paragraph names
    private static final Set<String> BREAK_PARAGRAPH_INDICATORS = Set.of(
            "BREAK", "CORTE", "RUPTURA", "CAMBIO", "CHANGE",
            "SUBTOTAL", "GROUP", "GRUPO", "NIVEL", "LEVEL",
            "TOTALES", "TOTALS", "HEADER", "CABECERA", "FOOTER", "PIE"
    );

    /**
     * Detects control break patterns in the given COBOL intermediate model.
     *
     * @param model The parsed COBOL intermediate model
     * @return List of detected control break patterns
     */
    public List<ControlBreakPattern> detectPatterns(CobolIntermediateModel model) {
        if (model == null) {
            return List.of();
        }

        List<ControlBreakPattern> patterns = new ArrayList<>();

        // Find file operations
        Map<String, List<FileOperationStatement>> fileOperations = findFileOperations(model);
        if (fileOperations.isEmpty()) {
            log.debug("No file operations found in program {}", model.getProgramId());
            return patterns;
        }

        // For each file with READ operations, analyze for control break patterns
        for (Map.Entry<String, List<FileOperationStatement>> entry : fileOperations.entrySet()) {
            String fileName = entry.getKey();
            List<FileOperationStatement> ops = entry.getValue();

            // Only analyze files with READ operations
            boolean hasReads = ops.stream()
                    .anyMatch(op -> op.operationType() == FileOperationStatement.OperationType.READ);

            if (hasReads) {
                ControlBreakPattern pattern = analyzeFileProcessing(model, fileName);
                if (pattern != null) {
                    patterns.add(pattern);
                    log.info("Detected control break pattern for file: {}", fileName);
                }
            }
        }

        return patterns;
    }

    /**
     * Finds all file operations grouped by file name.
     */
    private Map<String, List<FileOperationStatement>> findFileOperations(CobolIntermediateModel model) {
        Map<String, List<FileOperationStatement>> result = new LinkedHashMap<>();

        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (stmt instanceof FileOperationStatement fop) {
                    result.computeIfAbsent(fop.fileName(), k -> new ArrayList<>()).add(fop);
                }
            }
        }

        return result;
    }

    /**
     * Analyzes file processing paragraphs for control break patterns.
     */
    private ControlBreakPattern analyzeFileProcessing(CobolIntermediateModel model, String fileName) {
        // Find paragraphs involved in file processing
        List<CobolParagraph> processingParagraphs = findProcessingParagraphs(model, fileName);
        if (processingParagraphs.isEmpty()) {
            return null;
        }

        // Detect break levels by analyzing field comparisons
        List<ControlBreakPattern.BreakLevel> breakLevels = detectBreakLevels(model, processingParagraphs);

        // Separate initialization, detail processing, and finalization
        List<CobolStatement> initStatements = new ArrayList<>();
        List<CobolStatement> detailStatements = new ArrayList<>();
        List<CobolStatement> finalStatements = new ArrayList<>();

        categorizeParagraphs(processingParagraphs, initStatements, detailStatements, finalStatements);

        // Determine file operation type
        ControlBreakPattern.FileOperationType fileType = determineFileType(model, fileName);

        return ControlBreakPattern.builder()
                .patternId(UUID.randomUUID().toString())
                .fileName(fileName)
                .breakLevels(breakLevels)
                .initializationStatements(initStatements)
                .detailProcessingStatements(detailStatements)
                .finalizationStatements(finalStatements)
                .fileOperationType(fileType)
                .build();
    }

    /**
     * Finds paragraphs that are involved in processing a specific file.
     */
    private List<CobolParagraph> findProcessingParagraphs(CobolIntermediateModel model, String fileName) {
        List<CobolParagraph> result = new ArrayList<>();

        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            boolean involvesFile = paragraph.statements().stream()
                    .anyMatch(stmt -> {
                        if (stmt instanceof FileOperationStatement fop) {
                            return fop.fileName().equalsIgnoreCase(fileName);
                        }
                        return false;
                    });

            if (involvesFile || isBreakRelatedParagraph(paragraph.name())) {
                result.add(paragraph);
            }
        }

        return result;
    }

    /**
     * Detects break levels by analyzing field comparisons and "previous value" patterns.
     */
    private List<ControlBreakPattern.BreakLevel> detectBreakLevels(
            CobolIntermediateModel model,
            List<CobolParagraph> paragraphs
    ) {
        List<ControlBreakPattern.BreakLevel> breakLevels = new ArrayList<>();

        // Find potential control fields by analyzing data items with "SAVE" or "PREV" patterns
        Map<String, String> controlFieldPairs = findControlFieldPairs(model);

        // Find accumulator fields
        List<ControlBreakPattern.AggregationField> aggregations = findAggregationFields(model);

        int level = 1;
        for (Map.Entry<String, String> pair : controlFieldPairs.entrySet()) {
            String controlField = pair.getKey();
            String previousField = pair.getValue();

            // Find statements that process this break level
            List<CobolStatement> breakStatements = findBreakStatements(paragraphs, controlField);

            breakLevels.add(new ControlBreakPattern.BreakLevel(
                    level++,
                    controlField,
                    previousField,
                    breakStatements,
                    aggregations
            ));
        }

        return breakLevels;
    }

    /**
     * Finds pairs of current/previous value fields.
     */
    private Map<String, String> findControlFieldPairs(CobolIntermediateModel model) {
        Map<String, String> pairs = new LinkedHashMap<>();
        Set<String> dataItemNames = model.getDataItems().stream()
                .map(CobolDataItem::getName)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());

        for (String name : dataItemNames) {
            Matcher matcher = PREVIOUS_VALUE_PATTERN.matcher(name);
            if (matcher.matches()) {
                // Extract the base field name
                String baseField = null;
                if (matcher.group(2) != null) {
                    baseField = matcher.group(2);
                } else if (matcher.group(3) != null) {
                    baseField = matcher.group(3);
                }

                if (baseField != null && dataItemNames.contains(baseField.toUpperCase())) {
                    pairs.put(baseField.toUpperCase(), name.toUpperCase());
                }
            }
        }

        return pairs;
    }

    /**
     * Finds aggregation fields (totals, counts, etc.).
     */
    private List<ControlBreakPattern.AggregationField> findAggregationFields(CobolIntermediateModel model) {
        List<ControlBreakPattern.AggregationField> aggregations = new ArrayList<>();

        for (CobolDataItem item : model.getDataItems()) {
            String name = item.getName().toUpperCase();
            Matcher matcher = ACCUMULATOR_PATTERN.matcher(name);

            if (matcher.find()) {
                ControlBreakPattern.AggregationType type = determineAggregationType(name);
                aggregations.add(new ControlBreakPattern.AggregationField(
                        name,
                        type,
                        null // Source field would need deeper analysis
                ));
            }
        }

        return aggregations;
    }

    /**
     * Determines the aggregation type based on field name patterns.
     */
    private ControlBreakPattern.AggregationType determineAggregationType(String fieldName) {
        String upper = fieldName.toUpperCase();
        if (upper.contains("COUNT") || upper.contains("CNT") || upper.contains("CONTADOR")) {
            return ControlBreakPattern.AggregationType.COUNT;
        }
        if (upper.contains("SUM") || upper.contains("SUMA")) {
            return ControlBreakPattern.AggregationType.SUM;
        }
        if (upper.contains("AVG") || upper.contains("AVERAGE") || upper.contains("PROMEDIO")) {
            return ControlBreakPattern.AggregationType.AVERAGE;
        }
        if (upper.contains("MIN")) {
            return ControlBreakPattern.AggregationType.MIN;
        }
        if (upper.contains("MAX")) {
            return ControlBreakPattern.AggregationType.MAX;
        }
        return ControlBreakPattern.AggregationType.ACCUMULATOR;
    }

    /**
     * Finds statements related to a specific break level.
     */
    private List<CobolStatement> findBreakStatements(List<CobolParagraph> paragraphs, String controlField) {
        List<CobolStatement> breakStatements = new ArrayList<>();

        for (CobolParagraph paragraph : paragraphs) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (isRelatedToField(stmt, controlField)) {
                    breakStatements.add(stmt);
                }
            }
        }

        return breakStatements;
    }

    /**
     * Checks if a statement is related to a specific field.
     */
    private boolean isRelatedToField(CobolStatement stmt, String fieldName) {
        if (stmt instanceof MoveStatement move) {
            return move.source().toUpperCase().contains(fieldName) ||
                   move.target().toUpperCase().contains(fieldName);
        }
        if (stmt instanceof IfStatement ifStmt) {
            return ifStmt.condition().toUpperCase().contains(fieldName);
        }
        if (stmt instanceof ComputeStatement compute) {
            return compute.target().toUpperCase().contains(fieldName) ||
                   compute.expression().toUpperCase().contains(fieldName);
        }
        return false;
    }

    /**
     * Checks if a paragraph name suggests it's related to break processing.
     */
    private boolean isBreakRelatedParagraph(String paragraphName) {
        String upper = paragraphName.toUpperCase();
        return BREAK_PARAGRAPH_INDICATORS.stream()
                .anyMatch(upper::contains);
    }

    /**
     * Categorizes paragraphs into initialization, detail, and finalization.
     */
    private void categorizeParagraphs(
            List<CobolParagraph> paragraphs,
            List<CobolStatement> initStatements,
            List<CobolStatement> detailStatements,
            List<CobolStatement> finalStatements
    ) {
        for (CobolParagraph paragraph : paragraphs) {
            String name = paragraph.name().toUpperCase();

            if (name.contains("INIT") || name.contains("OPEN") || name.contains("START") ||
                name.contains("BEGIN") || name.contains("INICIO")) {
                initStatements.addAll(paragraph.statements());
            } else if (name.contains("END") || name.contains("CLOSE") || name.contains("FINAL") ||
                       name.contains("TERM") || name.contains("FIN")) {
                finalStatements.addAll(paragraph.statements());
            } else {
                // Default to detail processing
                detailStatements.addAll(paragraph.statements());
            }
        }
    }

    /**
     * Determines the file operation type based on file definitions and access patterns.
     */
    private ControlBreakPattern.FileOperationType determineFileType(
            CobolIntermediateModel model,
            String fileName
    ) {
        // Analyze file operations to determine type
        for (CobolParagraph paragraph : model.getParagraphs().values()) {
            for (CobolStatement stmt : paragraph.statements()) {
                if (stmt instanceof FileOperationStatement fop) {
                    if (fop.fileName().equalsIgnoreCase(fileName)) {
                        // Would need additional context from FILE SECTION to determine
                        // VSAM vs. sequential vs. indexed
                        // For now, default to sequential
                        return ControlBreakPattern.FileOperationType.SEQUENTIAL;
                    }
                }
            }
        }
        return ControlBreakPattern.FileOperationType.SEQUENTIAL;
    }
}
