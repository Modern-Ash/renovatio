package org.shark.renovatio.cobol.ir.parser;

import org.apache.commons.lang3.StringUtils;
import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.context.CobolTypeMapper;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;
import org.shark.renovatio.cobol.ir.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight IR parser that extracts an executable structure from COBOL
 * programs without requiring the full ProLeap dependency at runtime.  The
 * implementation intentionally focuses on a subset of the language that is
 * sufficient to drive the first iteration of the Java translator while keeping
 * the code easy to understand and extend.
 */
public class SimpleCobolIrParser {

    private static final Pattern PROGRAM_ID_PATTERN = Pattern.compile("PROGRAM-ID\\.\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_ITEM_PATTERN = Pattern.compile("(?m)^\\s*(0[1-9]|[1-4][0-9])\\s+([A-Z0-9-]+)(?:\\s+REDEFINES\\s+([A-Z0-9-]+))?\\s+PIC\\s+([^.]+)\\.");
    private static final Pattern LEVEL_88_PATTERN = Pattern.compile(
            "(?m)^\\s*88\\s+([A-Z0-9-]+)\\s+VALUES?(?:\\s+(?:IS|ARE))?\\s+(.+)\\.",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LEVEL_88_VALUE_PATTERN = Pattern.compile(
            "(?:'([^']*)'|\"([^\"]*)\"|([^\\s,]+))(?:\\s+THR(?:U|OUGH)\\s+(?:'([^']*)'|\"([^\"]*)\"|([^\\s,]+)))?",
            Pattern.CASE_INSENSITIVE);
    // Keep paragraph pattern for potential future use, but we'll prefer manual scan to avoid false positives
    @SuppressWarnings("unused")
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile(
            "(?ms)^\\s*([A-Z][A-Z0-9-]*)\\.(.*?)(?=^\\s*[A-Z][A-Z0-9-]*\\.|\\Z)"
    );
    private static final Pattern EXEC_SQL_PATTERN = Pattern.compile(Regexes.EXEC_SQL + "(.*?)" + Keywords.END_EXEC, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern DATA_NAME_PATTERN = Pattern.compile("[A-Z][A-Z0-9-]*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SET_CONDITION_PATTERN = Pattern.compile(
            "(?i)^SET\\s+(.+?)\\s+TO\\s+(TRUE|FALSE)$");

    private static final Pattern ENTRY_BLOCK_PATTERN;

    static {
        ENTRY_BLOCK_PATTERN = Pattern.compile(
                "ENTRY\\s+[\"']([^\"']+)[\"'](?:\\s+USING\\s+([A-Za-z0-9-]+))?\\s*\\.(.*?)(?=ENTRY\\s+[\"']|\\Z)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
    }

    private static final Pattern PARAGRAPH_HEADER_LINE = Pattern.compile("^([A-Z0-9][A-Z0-9-]*)\\.$");

    private static final Set<String> RESERVED_PARAGRAPH_TOKENS = Set.of(
            Keywords.IF, Keywords.ELSE, Keywords.MOVE, Keywords.COMPUTE, Keywords.EVALUATE, Keywords.PERFORM,
            Keywords.CALL, Keywords.GOBACK, Keywords.STOP, Keywords.EXIT, Keywords.EXEC, Keywords.READ,
            Keywords.WRITE, Keywords.ADD, Keywords.SUBTRACT, Keywords.MULTIPLY, Keywords.DIVIDE, Keywords.ENTRY,
            Keywords.INITIALIZE, Keywords.SET, "DISPLAY", "CONTINUE"
    );
    private static final Set<String> EXCLUDED_END_HEADERS = Set.of("END-IF", "END-EVALUATE", "END-EXEC", "END-PERFORM");

    /** Lines that are structural markers rather than statements (checked upper-case, period stripped). */
    private static final Set<String> STATEMENT_NOISE = Set.of(
            "EXIT", "ELSE", "THEN", "WHEN", "NEXT SENTENCE", "END-PERFORM");

    // ------------------ Constants extracted for literals ------------------
    private static final class Sections {
        private Sections() {}
        static final String WORKING_STORAGE = "WORKING-STORAGE SECTION";
        static final String LINKAGE = "LINKAGE SECTION";
        static final String PROCEDURE_DIVISION = "PROCEDURE DIVISION";
    }

    private static final class ContextKeys {
        private ContextKeys() {}
        static final String WORKING_STORAGE = "working-storage";
        static final String PROGRAM_ID = "programId";
    }

    private static final class Defaults {
        private Defaults() {}
        static final String PROGRAM_ID = "COBOLPROGRAM";
        static final String PARAGRAPH = "MAIN";
        static final String RESULT_VAR = "RESULT";
        static final String UNKNOWN_NAME = "UNKNOWN";
        static final String OTHER_BRANCH = "OTHER";
    }

    private static final class Keywords {
        private Keywords() {}
        // tokens and prefixes
        static final String IF = "IF";
        static final String IF_PREFIX = IF + " ";
        static final String ELSE = "ELSE";
        static final String END_IF = "END-IF";
        static final String THEN = "THEN";

        static final String EVALUATE = "EVALUATE";
        static final String WHEN = "WHEN";
        static final String WHEN_OTHER = "WHEN OTHER";
        static final String END_EVALUATE = "END-EVALUATE";

        static final String PERFORM = "PERFORM";
        static final String PERFORM_PREFIX = PERFORM + " ";
        static final String END_PERFORM = "END-PERFORM";
        static final String THRU = "THRU";
        static final String VARYING = "VARYING";
        static final String FROM = "FROM";
        static final String BY = "BY";
        static final String UNTIL = "UNTIL";
        static final String TIMES = "TIMES";
        static final String WITH = "WITH";
        static final String TEST = "TEST";
        static final String BEFORE = "BEFORE";
        static final String AFTER = "AFTER";

        static final String CALL = "CALL";

        static final String EXEC_SQL = "EXEC SQL";
        static final String END_EXEC = "END-EXEC";
        static final String EXIT_PROGRAM_LOWER = "exit program";

        static final String COMPUTE = "COMPUTE";
        static final String ADD = "ADD";
        static final String SUBTRACT = "SUBTRACT";
        static final String MULTIPLY = "MULTIPLY";
        static final String DIVIDE = "DIVIDE";
        static final String MOVE = "MOVE";
        static final String INITIALIZE = "INITIALIZE";
        static final String SET = "SET";

        static final String GIVING = "GIVING";

        // additional tokens used in reserved set
        static final String ENTRY = "ENTRY";
        static final String EXEC = "EXEC";
        static final String READ = "READ";
        static final String WRITE = "WRITE";
        static final String GOBACK = "GOBACK";
        static final String STOP = "STOP";
        static final String EXIT = "EXIT";
    }

    private static final class Regexes {
        private Regexes() {}
        static final String TO = "(?i)\\s+TO\\s+";
        static final String FROM = "(?i)\\s+FROM\\s+";
        static final String BY = "(?i)\\s+BY\\s+";
        static final String GIVING = "(?i)\\s+GIVING\\s+";
        static final String USING = "(?i)\\s+USING\\s+";
        static final String INTO = "(?i)\\s+INTO\\s+";
        static final String EXEC_SQL = "EXEC\\s+SQL";
    }

    private static final class Symbols {
        private Symbols() {}
        static final String DOT = ".";
        static final String EQUALS = "=";
        static final String NEWLINE = "\n";
        static final String SPACE = " ";
        static final String EMPTY = "";
        static final String DOUBLE_QUOTE = "\"";
        static final String SINGLE_QUOTE = "'";
        static final char NEWLINE_CHAR = '\n';
    }

    private static final class Messages {
        private Messages() {}
        static final String DEBUG_PREFIX = "IR DEBUG: ";
        static final String WS_ITEM_FOUND = DEBUG_PREFIX + "WS item found -> level={}, name={}, pic={}";
        static final String WS_ITEM_DUPLICATE = DEBUG_PREFIX + "duplicate WS item ignored -> name={}";
        static final String WS_TOTAL = DEBUG_PREFIX + "total WS items={}";
        static final String MODEL_DATA_ITEMS = DEBUG_PREFIX + "model data items size={}";
    }
    // ---------------------------------------------------------------------

    private static final Logger log = LoggerFactory.getLogger(SimpleCobolIrParser.class);

    public CobolIntermediateModel parse(Path cobolFile) throws IOException {
        String source = Files.readString(cobolFile);
        return parse(source);
    }

    public CobolIntermediateModel parse(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        String programId = extractProgramId(source);
        List<CobolDiagnostic> diagnostics = new ArrayList<>();
        List<CobolDataItem> dataItems = extractDataItems(source, diagnostics);
        log.debug(Messages.MODEL_DATA_ITEMS, dataItems.size());
        Map<String, CobolParagraph> paragraphs = extractParagraphs(source);
        Map<String, ParagraphLineRange> paragraphLineRanges = extractParagraphLineRanges(source);
        ControlFlowGraph flowGraph = buildControlFlowGraph(paragraphs);

        CobolExecutionContext.Builder contextBuilder = CobolExecutionContext.builder();
        Set<String> workingStorageNames = new LinkedHashSet<>();
        for (CobolDataItem item : dataItems) {
            workingStorageNames.add(item.getName().toUpperCase(Locale.ROOT));
        }
        contextBuilder.registerVariables(workingStorageNames, ContextKeys.WORKING_STORAGE);
        contextBuilder.attribute(ContextKeys.PROGRAM_ID, programId);

        CobolIntermediateModel.Builder builder = CobolIntermediateModel.builder()
                .programId(programId)
                .dataItems(dataItems)
                .controlFlowGraph(flowGraph)
                .executionContext(contextBuilder.build())
                .diagnostics(diagnostics)
                .paragraphLineRanges(paragraphLineRanges);
        paragraphs.values().forEach(builder::addParagraph);
        return builder.build();
    }

    private String extractProgramId(String source) {
        Matcher matcher = PROGRAM_ID_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return Defaults.PROGRAM_ID;
    }

    private List<CobolDataItem> extractDataItems(String source, List<CobolDiagnostic> diagnostics) {
        // Limit search to WORKING-STORAGE SECTION block
        int wsStart = StringUtils.indexOfIgnoreCase(source, Sections.WORKING_STORAGE);
        if (wsStart < 0) {
            return new ArrayList<>();
        }
        int lkStart = StringUtils.indexOfIgnoreCase(source, Sections.LINKAGE, wsStart);
        int pdStart = StringUtils.indexOfIgnoreCase(source, Sections.PROCEDURE_DIVISION, wsStart);
        int end = source.length();
        if (lkStart >= 0) end = Math.min(end, lkStart);
        if (pdStart >= 0) end = Math.min(end, pdStart);
        String wsSection = source.substring(wsStart, end);

        Matcher matcher = DATA_ITEM_PATTERN.matcher(wsSection);
        Map<String, CobolDataItem> unique = new LinkedHashMap<>();
        Map<String, Integer> declarationEnds = new LinkedHashMap<>();
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(1));
            String name = matcher.group(2).toUpperCase(Locale.ROOT);
            String redefines = matcher.group(3) != null ? matcher.group(3).toUpperCase(Locale.ROOT) : null;
            String pic = matcher.group(4).trim();
            String javaType = CobolTypeMapper.picToJavaType(pic);
            var picType = CobolTypeMapper.picType(pic);
            if (picType == null) {
                diagnostics.add(error("COBOL-PIC-001", "DATA_ITEM",
                        "Unsupported or malformed PIC clause for " + name, source, wsStart + matcher.start(4)));
            }
            if (!unique.containsKey(name)) {
                log.debug(Messages.WS_ITEM_FOUND, level, name, pic);
            } else {
                log.debug(Messages.WS_ITEM_DUPLICATE, name);
            }
            if (!unique.containsKey(name)) {
                unique.put(name, new CobolDataItem(name, pic, level, null, redefines, javaType,
                        picType, List.of()));
                declarationEnds.put(name, matcher.end());
            }
        }
        attachLevel88Conditions(wsSection, unique, declarationEnds);
        detectJavaNameCollisions(unique.values(), diagnostics, source);
        log.debug(Messages.WS_TOTAL, unique.size());
        return new ArrayList<>(unique.values());
    }

    private void detectJavaNameCollisions(Collection<CobolDataItem> items,
                                          List<CobolDiagnostic> diagnostics, String source) {
        Map<String, String> owners = new LinkedHashMap<>();
        for (CobolDataItem item : items) {
            String javaName = toJavaIdentifier(item.name());
            String previous = owners.putIfAbsent(javaName, item.name());
            if (previous != null && !previous.equals(item.name())) {
                int offset = Math.max(0, source.toUpperCase(Locale.ROOT).indexOf(item.name()));
                diagnostics.add(error("COBOL-NAME-001", "DATA_ITEM",
                        "Java name collision: " + previous + " and " + item.name()
                                + " both map to " + javaName, source, offset));
            }
        }
    }

    private String toJavaIdentifier(String cobolName) {
        String[] parts = cobolName.toLowerCase(Locale.ROOT).split("-+");
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
            }
        }
        return result.toString();
    }

    private CobolDiagnostic error(String code, String family, String message, String source, int offset) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < Math.min(offset, source.length()); i++) {
            if (source.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new CobolDiagnostic(code, CobolDiagnostic.Severity.ERROR, family, message,
                new SourceSpan("<memory>", line, column, line, column));
    }

    private void attachLevel88Conditions(String source, Map<String, CobolDataItem> items,
                                         Map<String, Integer> declarationEnds) {
        Matcher matcher = LEVEL_88_PATTERN.matcher(source);
        while (matcher.find()) {
            String parentName = null;
            int closestDeclaration = -1;
            for (Map.Entry<String, Integer> entry : declarationEnds.entrySet()) {
                if (entry.getValue() < matcher.start() && entry.getValue() > closestDeclaration) {
                    parentName = entry.getKey();
                    closestDeclaration = entry.getValue();
                }
            }
            if (parentName == null) {
                continue;
            }
            List<Level88Value> values = parseLevel88Values(matcher.group(2));
            if (values.isEmpty()) {
                continue;
            }
            CobolDataItem parent = items.get(parentName);
            List<Level88Condition> conditions = new ArrayList<>(parent.level88Conditions());
            conditions.add(new Level88Condition(
                    matcher.group(1).toUpperCase(Locale.ROOT), parentName, values));
            items.put(parentName, new CobolDataItem(parent.name(), parent.picture(), parent.level(),
                    parent.occurs(), parent.redefines(), parent.javaType(), parent.picType(), conditions));
        }
    }

    private List<Level88Value> parseLevel88Values(String clause) {
        List<Level88Value> values = new ArrayList<>();
        Matcher matcher = LEVEL_88_VALUE_PATTERN.matcher(clause);
        while (matcher.find()) {
            String lower = firstNonNull(matcher.group(1), matcher.group(2), matcher.group(3));
            String upper = firstNonNull(matcher.group(4), matcher.group(5), matcher.group(6));
            values.add(upper == null ? Level88Value.exact(lower) : Level88Value.range(lower, upper));
        }
        return values;
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<String, CobolParagraph> extractParagraphs(String source) {
        String procedureDiv = extractProcedureDivision(source);

        // Extract ENTRY blocks first
        Map<String, CobolParagraph> paragraphs = new LinkedHashMap<>(extractEntryParagraphs(procedureDiv));
        String procedureWithoutEntries = removeEntryBlocks(procedureDiv);

        // Parse non-ENTRY paragraphs using a manual scan
        paragraphs.putAll(parseNonEntryParagraphs(procedureWithoutEntries));

        if (paragraphs.isEmpty()) {
            paragraphs.put(Defaults.PARAGRAPH, CobolParagraph.empty(Defaults.PARAGRAPH));
        }
        return paragraphs;
    }

    /**
     * Lexical scan over the original PROCEDURE DIVISION text to recover the
     * absolute, one-based source line range of every paragraph, independently of
     * the substring surgery applied by {@link #extractParagraphs(String)}.
     */
    private Map<String, ParagraphLineRange> extractParagraphLineRanges(String source) {
        int procIdx = StringUtils.indexOfIgnoreCase(source, Sections.PROCEDURE_DIVISION);
        String procedureDiv = procIdx < 0 ? source : source.substring(procIdx);
        int baseLine = 1 + countLineFeeds(source, procIdx < 0 ? 0 : procIdx);

        List<int[]> entryBlockLines = entryBlockLineRanges(procedureDiv, baseLine);
        List<String> lines = List.of(procedureDiv.split(Symbols.NEWLINE, -1));
        List<ParagraphHeaderAtLine> headers = new ArrayList<>();
        String previousNonBlank = null;
        for (int i = 0; i < lines.size(); i++) {
            if (withinEntryBlock(baseLine + i, entryBlockLines)) {
                continue;
            }
            String trimmed = lines.get(i).stripLeading();
            if (trimmed.isEmpty()) {
                continue;
            }
            String detected = detectParagraphHeader(trimmed, previousNonBlank);
            previousNonBlank = trimmed;
            if (detected != null) {
                headers.add(new ParagraphHeaderAtLine(detected, baseLine + i));
            }
        }
        int lastContentLine = lastNonBlankLine(lines, baseLine);
        Map<String, ParagraphLineRange> ranges = new LinkedHashMap<>();
        for (int k = 0; k < headers.size(); k++) {
            ParagraphHeaderAtLine header = headers.get(k);
            int nextHeaderMinusOne = k + 1 < headers.size() ? headers.get(k + 1).line - 1 : lastContentLine;
            int endLine = Math.max(header.line, Math.min(nextHeaderMinusOne, lastContentLine));
            ranges.putIfAbsent(header.name, new ParagraphLineRange(header.line, endLine));
        }
        return ranges;
    }

    private List<int[]> entryBlockLineRanges(String procedureDiv, int baseLine) {
        List<int[]> ranges = new ArrayList<>();
        Matcher matcher = ENTRY_BLOCK_PATTERN.matcher(procedureDiv);
        while (matcher.find()) {
            int start = baseLine + countLineFeeds(procedureDiv, matcher.start());
            int end = baseLine + countLineFeeds(procedureDiv, matcher.end());
            ranges.add(new int[] {start, Math.max(start, end)});
        }
        return ranges;
    }

    private boolean withinEntryBlock(int absoluteLine, List<int[]> entryBlockLines) {
        for (int[] block : entryBlockLines) {
            if (absoluteLine >= block[0] && absoluteLine <= block[1]) {
                return true;
            }
        }
        return false;
    }

    private int lastNonBlankLine(List<String> lines, int baseLine) {
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (!lines.get(i).stripLeading().isEmpty()) {
                return baseLine + i;
            }
        }
        return baseLine;
    }

    private static int countLineFeeds(String text, int endExclusive) {
        int count = 0;
        for (int i = 0; i < endExclusive; i++) {
            if (text.charAt(i) == Symbols.NEWLINE_CHAR) {
                count++;
            }
        }
        return count;
    }

    /**
     * A paragraph header name paired with its absolute 1-based source line.
     */
    private record ParagraphHeaderAtLine(String name, int line) {
    }

    private Map<String, CobolParagraph> parseNonEntryParagraphs(String procedureSource) {
        Map<String, CobolParagraph> result = new LinkedHashMap<>();
        List<String> lines = List.of(procedureSource.split(Symbols.NEWLINE, -1));
        String currentHeader = null;
        StringBuilder currentBody = new StringBuilder();
        String previousNonBlank = null;

        for (String rawLine : lines) {
            String line = rawLine.stripLeading();
            if (line.isEmpty()) {
                if (currentHeader != null) currentBody.append(rawLine).append(Symbols.NEWLINE_CHAR);
                continue;
            }
            String detected = detectParagraphHeader(line, previousNonBlank);
            previousNonBlank = line;
            if (detected != null) {
                // flush previous
                flushCurrentParagraph(result, currentHeader, currentBody);
                currentHeader = detected;
                currentBody.setLength(0);
            } else if (currentHeader != null) {
                currentBody.append(rawLine).append(Symbols.NEWLINE_CHAR);
            }
        }
        flushCurrentParagraph(result, currentHeader, currentBody);
        return result;
    }

    private String detectParagraphHeader(String trimmedLine) {
        return detectParagraphHeader(trimmedLine, null);
    }

    private String detectParagraphHeader(String trimmedLine, String previousNonBlankTrimmedLine) {
        // A bare-THRU ending leaves a PERFORM range open; the next line is its
        // continuation token, not a new paragraph (mirrors parsePerform's join).
        if (endsWithThru(previousNonBlankTrimmedLine)) {
            return null;
        }
        Matcher headerMatcher = PARAGRAPH_HEADER_LINE.matcher(trimmedLine);
        if (!headerMatcher.find()) {
            return null;
        }
        String candidate = headerMatcher.group(1).toUpperCase(Locale.ROOT);
        return isReservedParagraphHeader(candidate) ? null : candidate;
    }

    private static boolean endsWithThru(String trimmedLine) {
        return trimmedLine != null && trimmedLine.toUpperCase(Locale.ROOT).endsWith(Keywords.THRU);
    }

    private boolean isReservedParagraphHeader(String candidate) {
        return RESERVED_PARAGRAPH_TOKENS.contains(candidate) || EXCLUDED_END_HEADERS.contains(candidate);
    }

    private void flushCurrentParagraph(Map<String, CobolParagraph> acc, String currentHeader, StringBuilder currentBody) {
        if (currentHeader == null) return;
        List<CobolStatement> statements = parseStatements(currentBody.toString());
        acc.put(currentHeader, new CobolParagraph(currentHeader, statements));
    }

    private String removeEntryBlocks(String source) {
        Matcher m = ENTRY_BLOCK_PATTERN.matcher(source);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Symbols.EMPTY);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Map<String, CobolParagraph> extractEntryParagraphs(String source) {
        Map<String, CobolParagraph> entries = new LinkedHashMap<>();
        Matcher matcher = ENTRY_BLOCK_PATTERN.matcher(source);
        while (matcher.find()) {
            String entryName = matcher.group(1).toUpperCase(Locale.ROOT);
            String body = matcher.group(3);
            int exitIdx = body.toLowerCase(Locale.ROOT).indexOf(Keywords.EXIT_PROGRAM_LOWER);
            if (exitIdx > 0) {
                body = body.substring(0, exitIdx);
            }
            body = body.trim();
            if (!body.isEmpty()) {
                List<CobolStatement> statements = parseStatements(body);
                entries.put(entryName, new CobolParagraph(entryName, statements));
            }
        }
        return entries;
    }

    private String extractProcedureDivision(String source) {
        int procIdx = StringUtils.indexOfIgnoreCase(source, Sections.PROCEDURE_DIVISION);
        if (procIdx < 0) {
            return source;
        }
        return source.substring(procIdx);
    }

    private List<CobolStatement> parseStatements(String block) {
        List<CobolStatement> statements = new ArrayList<>();
        List<String> lines = List.of(block.split(Symbols.NEWLINE));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String upperLine = line.toUpperCase(Locale.ROOT);
            if (upperLine.startsWith(Keywords.IF_PREFIX)) {
                i = parseIf(lines, i, statements);
                continue;
            }
            if (upperLine.startsWith(Keywords.EVALUATE)) {
                i = parseEvaluate(lines, i, statements);
                continue;
            }
            if (upperLine.startsWith(Keywords.PERFORM)) {
                i = parsePerform(lines, i, statements);
                continue;
            }
            if (upperLine.startsWith(Keywords.CALL)) {
                statements.add(parseCall(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.EXEC_SQL)) {
                i = parseExecSql(lines, i, statements);
                continue;
            }
            FileOperationStatement.OperationType op = detectFileOperation(upperLine);
            if (op != null) {
                statements.add(new FileOperationStatement(op, parseFileName(line, op)));
                continue;
            }
            if (upperLine.startsWith(Keywords.COMPUTE)) {
                statements.add(parseCompute(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.ADD)) {
                statements.add(parseAdd(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.SUBTRACT)) {
                statements.add(parseSubtract(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.MULTIPLY)) {
                statements.add(parseMultiply(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.DIVIDE)) {
                statements.add(parseDivide(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.MOVE)) {
                statements.add(parseMove(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.INITIALIZE)) {
                statements.add(parseInitialize(line));
                continue;
            }
            if (upperLine.startsWith(Keywords.SET)) {
                statements.add(parseSetCondition(line));
                continue;
            }
            SimpleStatement simple = parseSimpleStatement(line, upperLine);
            if (simple != null) {
                statements.add(simple);
            }
        }
        return statements;
    }

    /**
     * Recognises DISPLAY / CONTINUE / GOBACK / STOP RUN, and captures every other non-noise
     * PROCEDURE DIVISION line as {@link SimpleStatement.Kind#UNTRANSLATED} instead of dropping it.
     * Returns {@code null} for structural noise (period-only lines, {@code EXIT}, {@code END-*}).
     */
    private SimpleStatement parseSimpleStatement(String line, String upperLine) {
        String withoutPeriod = stripTrailingPeriod(line);
        String upper = stripTrailingPeriod(upperLine);
        if (upper.equals("DISPLAY")) {
            return new SimpleStatement(SimpleStatement.Kind.DISPLAY, "");
        }
        if (upper.startsWith("DISPLAY ")) {
            return new SimpleStatement(SimpleStatement.Kind.DISPLAY, withoutPeriod.substring("DISPLAY ".length()).trim());
        }
        if (upper.equals("CONTINUE")) {
            return new SimpleStatement(SimpleStatement.Kind.CONTINUE, "");
        }
        if (upper.equals("GOBACK")) {
            return new SimpleStatement(SimpleStatement.Kind.GOBACK, "");
        }
        if (upper.equals("STOP") || upper.equals("STOP RUN")) {
            return new SimpleStatement(SimpleStatement.Kind.STOP_RUN, "");
        }
        if (isStatementNoise(upper)) {
            return null;
        }
        return new SimpleStatement(SimpleStatement.Kind.UNTRANSLATED, withoutPeriod.trim());
    }

    private static String stripTrailingPeriod(String value) {
        String trimmed = value.trim();
        return trimmed.endsWith(".") ? trimmed.substring(0, trimmed.length() - 1).trim() : trimmed;
    }

    private static boolean isStatementNoise(String upperNoPeriod) {
        if (upperNoPeriod.isEmpty()) {
            return true;
        }
        if (STATEMENT_NOISE.contains(upperNoPeriod)) {
            return true;
        }
        return upperNoPeriod.startsWith("END-") || upperNoPeriod.startsWith("EXIT ");
    }

    private int parseIf(List<String> lines, int index, List<CobolStatement> statements) {
        String line = lines.get(index).trim();
        String condition = line.substring(Keywords.IF_PREFIX.length()).trim();
        List<String> thenLines = new ArrayList<>();
        List<String> elseLines = new ArrayList<>();
        boolean inElse = false;
        int i = index + 1;
        for (; i < lines.size(); i++) {
            String current = lines.get(i).trim();
            if (current.isEmpty()) {
                continue;
            }
            String up = current.toUpperCase(Locale.ROOT);
            if (up.startsWith(Keywords.ELSE)) {
                inElse = true;
                String afterElse = current.substring(current.length() >= Keywords.ELSE.length() ? Keywords.ELSE.length() : 0).trim();
                if (!afterElse.isEmpty() && !afterElse.equals(Symbols.DOT)) {
                    elseLines.add(afterElse);
                }
                continue;
            }
            if (up.startsWith(Keywords.END_IF)) {
                break;
            }
            if (inElse) {
                elseLines.add(current);
            } else {
                thenLines.add(current);
            }
        }
        statements.add(new IfStatement(normalizeCondition(condition),
                parseStatements(String.join(Symbols.NEWLINE, thenLines)),
                parseStatements(String.join(Symbols.NEWLINE, elseLines))));
        return i;
    }

    private int parseEvaluate(List<String> lines, int index, List<CobolStatement> statements) {
        String expression = lines.get(index).trim().substring(Keywords.EVALUATE.length()).trim();
        List<EvaluateStatement.EvaluateWhenBranch> branches = new ArrayList<>();
        List<String> accumulator = new ArrayList<>();
        String currentCondition = Defaults.OTHER_BRANCH;
        int i = index + 1;
        for (; i < lines.size(); i++) {
            String current = lines.get(i).trim();
            String up = current.toUpperCase(Locale.ROOT);
            if (up.startsWith(Keywords.WHEN_OTHER)) {
                if (!accumulator.isEmpty()) {
                    branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                            parseStatements(String.join(Symbols.NEWLINE, accumulator))));
                    accumulator.clear();
                }
                currentCondition = Defaults.OTHER_BRANCH;
                continue;
            }
            if (up.startsWith(Keywords.WHEN)) {
                if (!accumulator.isEmpty()) {
                    branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                            parseStatements(String.join(Symbols.NEWLINE, accumulator))));
                    accumulator.clear();
                }
                currentCondition = current.substring(Keywords.WHEN.length()).trim();
                continue;
            }
            if (up.startsWith(Keywords.END_EVALUATE)) {
                break;
            }
            if (!current.isEmpty()) {
                accumulator.add(current);
            }
        }
        if (!accumulator.isEmpty()) {
            branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                    parseStatements(String.join(Symbols.NEWLINE, accumulator))));
        }
        statements.add(new EvaluateStatement(expression, branches));
        return i;
    }

    private int parseExecSql(List<String> lines, int index, List<CobolStatement> statements) {
        StringBuilder sql = new StringBuilder();
        for (int i = index; i < lines.size(); i++) {
            String current = lines.get(i);
            sql.append(current).append(Symbols.NEWLINE_CHAR);
            if (current.trim().toUpperCase(Locale.ROOT).contains(Keywords.END_EXEC)) {
                statements.add(new Db2Statement(cleanSql(sql.toString())));
                return i;
            }
        }
        statements.add(new Db2Statement(cleanSql(sql.toString())));
        return lines.size();
    }

    private String cleanSql(String raw) {
        Matcher matcher = EXEC_SQL_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).replaceAll(Symbols.NEWLINE, Symbols.SPACE).trim();
        }
        return raw.replaceAll(Regexes.EXEC_SQL, Symbols.EMPTY).replace(Keywords.END_EXEC, Symbols.EMPTY).trim();
    }

    private static final java.util.regex.Pattern PERFORM_VARYING = java.util.regex.Pattern.compile(
            "\\s+VARYING\\s+(\\S+)\\s+FROM\\s+(\\S+)(?:\\s+BY\\s+(\\S+))?(?:\\s+UNTIL\\s+(.+))?$",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern PERFORM_TIMES = java.util.regex.Pattern.compile(
            "(\\d+)\\s+TIMES\\s*$", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern PERFORM_UNTIL = java.util.regex.Pattern.compile(
            "\\s+UNTIL\\s+(.+)$", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final java.util.regex.Pattern PERFORM_TEST = java.util.regex.Pattern.compile(
            "\\s+WITH\\s+TEST\\s+(BEFORE|AFTER)", java.util.regex.Pattern.CASE_INSENSITIVE);

    /**
     * Parses a {@code PERFORM} starting at {@code index}, which may span several physical lines:
     * <ul>
     *   <li>a {@code THRU} range continued on the next line
     *       ({@code PERFORM 3000-SEND-MAP THRU} / {@code 3000-SEND-MAP-EXIT});</li>
     *   <li>an inline {@code PERFORM ... END-PERFORM} block, which consumes its body and any
     *       balanced nested inline performs.</li>
     * </ul>
     * Returns the index of the last consumed physical line.
     */
    private int parsePerform(List<String> lines, int index, List<CobolStatement> statements) {
        String joined = lines.get(index).trim();
        int lastIndex = index;
        while (joined.toUpperCase(Locale.ROOT).endsWith(Keywords.THRU) && lastIndex + 1 < lines.size()) {
            String nextRaw = lines.get(lastIndex + 1).trim();
            String nextUpper = stripTrailingPeriod(nextRaw.toUpperCase(Locale.ROOT));
            if (!nextUpper.matches("[A-Z0-9][A-Z0-9-]*")) {
                break;
            }
            joined = joined + Symbols.SPACE + nextRaw;
            lastIndex++;
        }
        return finishParsePerform(joined, lines, lastIndex, statements);
    }

    private int finishParsePerform(String joined, List<String> lines, int lastIndex, List<CobolStatement> statements) {
        // The PERFORM keyword is always a prefix; anything after it is either the paragraph
        // range, trailing modifiers, or (for inline blocks) a modifier-only head.
        String withoutPerform = stripTrailingPeriod(joined.substring(Keywords.PERFORM.length()).trim());
        String raw = withoutPerform.replaceAll("\\s+" + Keywords.THRU + "\\s+", "~").trim();

        // Modifier clauses always trail the paragraph range. Split at the first modifier keyword
        // (or at the THRU marker when no modifier follows).
        int split = modifierStart(raw);
        String modifiers = split >= 0 ? raw.substring(Math.max(split, raw.indexOf('~') + 1)) : "";
        String range = split >= 0 ? raw.substring(0, Math.max(split, raw.indexOf('~') + 1)) : raw;

        // An inline PERFORM head starts with the modifier clause at position 0 (no leading
        // separator), which the clause regexes expect as whitespace -- normalise it.
        if (!modifiers.isEmpty() && !Character.isWhitespace(modifiers.charAt(0))) {
            modifiers = Symbols.SPACE + modifiers;
        }

        boolean testAfter = false;
        java.util.regex.Matcher test = PERFORM_TEST.matcher(modifiers);
        if (test.find()) {
            testAfter = Keywords.AFTER.equalsIgnoreCase(test.group(1));
            modifiers = modifiers.substring(0, test.start()) + modifiers.substring(test.end());
        }

        String varyingVariable = null;
        String varyingFrom = null;
        String varyingBy = null;
        String untilCondition = null;
        Integer timesCount = null;

        java.util.regex.Matcher varying = PERFORM_VARYING.matcher(modifiers);
        if (varying.find()) {
            varyingVariable = varying.group(1);
            varyingFrom = varying.group(2);
            varyingBy = varying.group(3);
            untilCondition = varying.group(4);
        } else {
            java.util.regex.Matcher times = PERFORM_TIMES.matcher(modifiers);
            if (times.find()) {
                timesCount = Integer.parseInt(times.group(1));
            }
            java.util.regex.Matcher until = PERFORM_UNTIL.matcher(modifiers);
            if (until.find()) {
                untilCondition = until.group(1).trim();
            }
        }

        String[] rangeParts = range.split("~", 2);
        String first = rangeParts[0].replace(Symbols.DOT, Symbols.EMPTY).trim();
        String thru = rangeParts.length > 1 ? rangeParts[1].replace(Symbols.DOT, Symbols.EMPTY).trim() : null;

        if (first.isEmpty()) {
            // Inline PERFORM ... END-PERFORM block: body starts on the next line.
            PerformStatement head = new PerformStatement(null, thru, varyingVariable, varyingFrom,
                    varyingBy, untilCondition, timesCount, testAfter, List.of());
            return parseInlinePerform(lines, lastIndex + 1, statements, head);
        }
        statements.add(new PerformStatement(first, thru, varyingVariable, varyingFrom,
                varyingBy, untilCondition, timesCount, testAfter, List.of()));
        return lastIndex;
    }

    /**
     * Collects the lines of an inline {@code PERFORM ... END-PERFORM} block (tracking balanced
     * nesting of nested inline performs), parses the body recursively, and adds the statement.
     */
    private int parseInlinePerform(List<String> lines, int bodyStart, List<CobolStatement> statements,
                                   PerformStatement head) {
        List<String> bodyLines = new ArrayList<>();
        int depth = 1;
        int j = bodyStart;
        for (; j < lines.size(); j++) {
            String current = lines.get(j).trim();
            if (current.isEmpty()) {
                continue;
            }
            String up = current.toUpperCase(Locale.ROOT);
            if (up.startsWith(Keywords.PERFORM_PREFIX) && startsInlinePerform(current)) {
                depth++;
            } else if (up.startsWith(Keywords.END_PERFORM)) {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
            bodyLines.add(current);
        }
        boolean closed = j < lines.size();
        List<CobolStatement> body = parseStatements(String.join(Symbols.NEWLINE, bodyLines));
        statements.add(new PerformStatement(null, head.throughParagraph(), head.varyingVariable(),
                head.varyingFrom(), head.varyingBy(), head.untilCondition(), head.timesCount(),
                head.testAfter(), body));
        return closed ? j : lines.size();
    }

    /**
     * True when {@code line} opens an inline {@code PERFORM} block (no trailing paragraph name),
     * i.e. the modifiers / {@code WITH TEST} clause or nothing at all follows the keyword.
     */
    private static boolean startsInlinePerform(String line) {
        String up = line.toUpperCase(Locale.ROOT);
        if (!up.startsWith(Keywords.PERFORM_PREFIX)) {
            return false;
        }
        String head = stripTrailingPeriod(line.substring(Keywords.PERFORM.length()).trim());
        String raw = head.replaceAll("\\s+" + Keywords.THRU + "\\s+", "~").trim();
        if (raw.isEmpty()) {
            return true; // bare "PERFORM" opens an inline block
        }
        return modifierStart(raw) == 0;
    }

    /** Index of the first trailing modifier clause ({@code VARYING}/{@code UNTIL}/{@code n TIMES}/{@code WITH TEST}) or {@code -1}. */
    private static int modifierStart(String raw) {
        java.util.regex.Matcher m = PERFORM_MODIFIER.matcher(raw);
        return m.find() ? m.start() : -1;
    }

    private static final java.util.regex.Pattern PERFORM_MODIFIER = java.util.regex.Pattern.compile(
            "(?:\\s+|^)(WITH\\s+TEST\\s+(?:BEFORE|AFTER)\\b|VARYING\\b|UNTIL\\b|\\d+\\s+TIMES\\b)",
            java.util.regex.Pattern.CASE_INSENSITIVE);

    private CobolStatement parseCall(String line) {
        String remainder = line.substring(Keywords.CALL.length()).trim();
        String[] parts = remainder.split(Regexes.USING, 2);
        String target = parts[0].replace(Symbols.DOUBLE_QUOTE, Symbols.EMPTY).replace(Symbols.SINGLE_QUOTE, Symbols.EMPTY);
        List<String> args = new ArrayList<>();
        if (parts.length > 1) {
            args.addAll(Arrays.stream(parts[1].split(",|\\s+")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        }
        return new CallStatement(target, args);
    }

    private FileOperationStatement.OperationType detectFileOperation(String line) {
        for (FileOperationStatement.OperationType type : FileOperationStatement.OperationType.values()) {
            if (line.startsWith(type.name())) {
                return type;
            }
        }
        return null;
    }

    private String parseFileName(String line, FileOperationStatement.OperationType operation) {
        String[] parts = line.split("\\s+");
        int fileNameIndex = operation == FileOperationStatement.OperationType.OPEN ? 2 : 1;
        return parts.length > fileNameIndex
                ? parts[fileNameIndex].replace(Symbols.DOT, Symbols.EMPTY).toUpperCase(Locale.ROOT)
                : Defaults.UNKNOWN_NAME;
    }

    private ComputeStatement parseCompute(String line) {
        String remainder = line.substring(Keywords.COMPUTE.length()).trim();
        String[] parts = remainder.split(Symbols.EQUALS, 2);
        if (parts.length != 2) {
            return new ComputeStatement(Defaults.RESULT_VAR, remainder);
        }
        String target = parts[0].trim();
        String expression = parts[1].replace(Symbols.DOT, Symbols.EMPTY).trim();
        return new ComputeStatement(target, expression);
    }

    private ComputeStatement parseAdd(String line) {
        String trimmed = line.replace(Symbols.DOT, Symbols.EMPTY).trim();
        String remainder = trimmed.substring(Keywords.ADD.length()).trim();
        // ADD source TO target
        String[] parts = remainder.split(Regexes.TO, 2);
        if (parts.length == 2) {
            String source = parts[0].trim();
            String target = parts[1].trim();
            return new ComputeStatement(target, target + " + " + source);
        }
        // ADD source1 source2 ... GIVING target
        parts = remainder.split(Regexes.GIVING, 2);
        if (parts.length == 2) {
            String target = parts[1].trim();
            String sources = parts[0].replace("+", Symbols.SPACE).trim();
            return new ComputeStatement(target, sources.replaceAll("\\s+", " + "));
        }
        return new ComputeStatement(Defaults.RESULT_VAR, remainder);
    }

    private ComputeStatement parseSubtract(String line) {
        String trimmed = line.replace(Symbols.DOT, Symbols.EMPTY).trim();
        String remainder = trimmed.substring(Keywords.SUBTRACT.length()).trim();
        // SUBTRACT source FROM target
        String[] parts = remainder.split(Regexes.FROM, 2);
        if (parts.length == 2) {
            String source = parts[0].trim();
            String target = parts[1].trim();
            return new ComputeStatement(target, target + " - " + source);
        }
        // SUBTRACT source1 FROM source2 GIVING target
        if (remainder.toUpperCase(Locale.ROOT).contains(Keywords.GIVING)) {
            String[] givingParts = remainder.split(Regexes.GIVING, 2);
            if (givingParts.length == 2) {
                String target = givingParts[1].trim();
                String[] fromParts = givingParts[0].split(Regexes.FROM, 2);
                if (fromParts.length == 2) {
                    return new ComputeStatement(target, fromParts[1].trim() + " - " + fromParts[0].trim());
                }
            }
        }
        return new ComputeStatement(Defaults.RESULT_VAR, remainder);
    }

    private ComputeStatement parseMultiply(String line) {
        String trimmed = line.replace(Symbols.DOT, Symbols.EMPTY).trim();
        String remainder = trimmed.substring(Keywords.MULTIPLY.length()).trim();
        // MULTIPLY source BY target
        String[] parts = remainder.split(Regexes.BY, 2);
        if (parts.length == 2) {
            String source = parts[0].trim();
            String target = parts[1].trim();
            return new ComputeStatement(target, target + " * " + source);
        }
        // MULTIPLY source1 BY source2 GIVING target
        parts = remainder.split(Regexes.GIVING, 2);
        if (parts.length == 2) {
            String target = parts[1].trim();
            String[] byParts = parts[0].split(Regexes.BY, 2);
            if (byParts.length == 2) {
                return new ComputeStatement(target, byParts[0].trim() + " * " + byParts[1].trim());
            }
        }
        return new ComputeStatement(Defaults.RESULT_VAR, remainder);
    }

    private ComputeStatement parseDivide(String line) {
        String trimmed = line.replace(Symbols.DOT, Symbols.EMPTY).trim();
        String remainder = trimmed.substring(Keywords.DIVIDE.length()).trim();
        // DIVIDE source INTO target
        String[] parts = remainder.split(Regexes.INTO, 2);
        if (parts.length == 2) {
            String source = parts[0].trim();
            String target = parts[1].trim();
            return new ComputeStatement(target, target + " / " + source);
        }
        // DIVIDE source1 BY source2 GIVING target
        parts = remainder.split(Regexes.GIVING, 2);
        if (parts.length == 2) {
            String target = parts[1].trim();
            String[] byParts = parts[0].split(Regexes.BY, 2);
            if (byParts.length == 2) {
                return new ComputeStatement(target, byParts[0].trim() + " / " + byParts[1].trim());
            }
        }
        return new ComputeStatement(Defaults.RESULT_VAR, remainder);
    }

    private MoveStatement parseMove(String line) {
        String trimmed = line.replace(Symbols.DOT, Symbols.EMPTY).trim();
        String remainder = trimmed.substring(Keywords.MOVE.length()).trim();
        String[] parts = remainder.split(Regexes.TO, 2);
        if (parts.length != 2) {
            return new MoveStatement(remainder, remainder);
        }
        return new MoveStatement(parts[0].trim(), parts[1].trim());
    }

    private CobolStatement parseInitialize(String line) {
        String sourceText = stripTrailingPeriod(line);
        String remainder = sourceText.substring(Keywords.INITIALIZE.length()).trim();
        if (remainder.isEmpty() || remainder.toUpperCase(Locale.ROOT).contains(" REPLACING ")) {
            return new SimpleStatement(SimpleStatement.Kind.UNTRANSLATED, sourceText);
        }
        List<String> targets = Arrays.stream(remainder.split("[,\\s]+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (targets.isEmpty() || targets.stream().anyMatch(target -> !DATA_NAME_PATTERN.matcher(target).matches())) {
            return new SimpleStatement(SimpleStatement.Kind.UNTRANSLATED, sourceText);
        }
        return new InitializeStatement(targets, sourceText);
    }

    private CobolStatement parseSetCondition(String line) {
        String sourceText = stripTrailingPeriod(line);
        Matcher matcher = SET_CONDITION_PATTERN.matcher(sourceText);
        if (!matcher.matches()) {
            return new SimpleStatement(SimpleStatement.Kind.UNTRANSLATED, sourceText);
        }
        List<String> conditionNames = Arrays.stream(matcher.group(1).trim().split("[,\\s]+"))
                .filter(token -> !token.isBlank())
                .toList();
        if (conditionNames.isEmpty()
                || conditionNames.stream().anyMatch(name -> !DATA_NAME_PATTERN.matcher(name).matches())) {
            return new SimpleStatement(SimpleStatement.Kind.UNTRANSLATED, sourceText);
        }
        return new SetConditionStatement(conditionNames,
                Boolean.parseBoolean(matcher.group(2).toLowerCase(Locale.ROOT)), sourceText);
    }

    private String normalizeCondition(String condition) {
        return condition.replace(Keywords.THEN, Symbols.EMPTY).replace(Symbols.DOT, Symbols.EMPTY).trim();
    }

    private ControlFlowGraph buildControlFlowGraph(Map<String, CobolParagraph> paragraphs) {
        ControlFlowGraph.Builder builder = ControlFlowGraph.builder();
        String previous = null;
        for (CobolParagraph paragraph : paragraphs.values()) {
            builder.ensureNode(paragraph.name());
            if (previous != null) {
                builder.addEdge(previous, paragraph.name());
            }
            previous = paragraph.name();
            for (CobolStatement statement : paragraph.statements()) {
                if (statement instanceof PerformStatement perform) {
                    if (perform.paragraph() != null) {
                        builder.addEdge(paragraph.name(), perform.paragraph());
                        if (perform.throughParagraph() != null) {
                            builder.addEdge(perform.paragraph(), perform.throughParagraph());
                        }
                    }
                }
            }
        }
        return builder.build();
    }
}
