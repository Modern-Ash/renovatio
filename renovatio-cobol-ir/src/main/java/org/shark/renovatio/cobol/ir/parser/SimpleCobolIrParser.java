package org.shark.renovatio.cobol.ir.parser;

import org.apache.commons.lang3.StringUtils;
import org.shark.renovatio.cobol.ir.context.CobolExecutionContext;
import org.shark.renovatio.cobol.ir.context.CobolTypeMapper;
import org.shark.renovatio.cobol.ir.flow.ControlFlowGraph;
import org.shark.renovatio.cobol.ir.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final Pattern DATA_ITEM_PATTERN = Pattern.compile("(?m)^(\\s*)(\\d{2})\\s+([A-Z0-9-]+)(?:\\s+REDEFINES\\s+([A-Z0-9-]+))?\\s+PIC\\s+([^\.]+)\\.");
    private static final Pattern PARAGRAPH_PATTERN = Pattern.compile("(?ms)^(?<!-) {0,6}([A-Z][A-Z0-9-]*)\\.(.*?)(?=^(?<!-) {0,6}[A-Z][A-Z0-9-]*\\.|\\Z)");
    private static final Pattern EXEC_SQL_PATTERN = Pattern.compile("EXEC\\s+SQL(.*?)END-EXEC", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public CobolIntermediateModel parse(Path cobolFile) throws IOException {
        String source = Files.readString(cobolFile);
        return parse(source);
    }

    public CobolIntermediateModel parse(String source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        String programId = extractProgramId(source);
        List<CobolDataItem> dataItems = extractDataItems(source);
        Map<String, CobolParagraph> paragraphs = extractParagraphs(source);
        ControlFlowGraph flowGraph = buildControlFlowGraph(paragraphs);

        CobolExecutionContext.Builder contextBuilder = CobolExecutionContext.builder();
        Set<String> workingStorageNames = new LinkedHashSet<>();
        for (CobolDataItem item : dataItems) {
            workingStorageNames.add(item.getName().toUpperCase(Locale.ROOT));
        }
        contextBuilder.registerVariables(workingStorageNames, "working-storage");
        contextBuilder.attribute("programId", programId);

        CobolIntermediateModel.Builder builder = CobolIntermediateModel.builder()
                .programId(programId)
                .dataItems(dataItems)
                .controlFlowGraph(flowGraph)
                .executionContext(contextBuilder.build());
        paragraphs.values().forEach(builder::addParagraph);
        return builder.build();
    }

    private String extractProgramId(String source) {
        Matcher matcher = PROGRAM_ID_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        return "COBOLPROGRAM";
    }

    private List<CobolDataItem> extractDataItems(String source) {
        Matcher matcher = DATA_ITEM_PATTERN.matcher(source);
        List<CobolDataItem> items = new ArrayList<>();
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(2));
            String name = matcher.group(3).toUpperCase(Locale.ROOT);
            String redefines = matcher.group(4) != null ? matcher.group(4).toUpperCase(Locale.ROOT) : null;
            String pic = matcher.group(5).trim();
            String javaType = CobolTypeMapper.picToJavaType(pic);
            items.add(new CobolDataItem(name, pic, level, null, redefines, javaType));
        }
        return items;
    }

    private Map<String, CobolParagraph> extractParagraphs(String source) {
        Matcher matcher = PARAGRAPH_PATTERN.matcher(extractProcedureDivision(source));
        Map<String, CobolParagraph> paragraphs = new LinkedHashMap<>();
        while (matcher.find()) {
            String name = matcher.group(1).toUpperCase(Locale.ROOT);
            String body = matcher.group(2);
            List<CobolStatement> statements = parseStatements(body);
            paragraphs.put(name, new CobolParagraph(name, statements));
        }
        if (paragraphs.isEmpty()) {
            paragraphs.put("MAIN", CobolParagraph.empty("MAIN"));
        }
        return paragraphs;
    }

    private String extractProcedureDivision(String source) {
        int procIdx = StringUtils.indexOfIgnoreCase(source, "PROCEDURE DIVISION");
        if (procIdx < 0) {
            return source;
        }
        return source.substring(procIdx);
    }

    private List<CobolStatement> parseStatements(String block) {
        List<CobolStatement> statements = new ArrayList<>();
        List<String> lines = Arrays.asList(block.split("\n"));
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("IF ")) {
                i = parseIf(lines, i, statements);
                continue;
            }
            if (line.startsWith("EVALUATE")) {
                i = parseEvaluate(lines, i, statements);
                continue;
            }
            if (line.startsWith("PERFORM")) {
                statements.add(parsePerform(line));
                continue;
            }
            if (line.startsWith("CALL")) {
                statements.add(parseCall(line));
                continue;
            }
            if (line.startsWith("EXEC SQL")) {
                i = parseExecSql(lines, i, statements);
                continue;
            }
            FileOperationStatement.OperationType op = detectFileOperation(line);
            if (op != null) {
                statements.add(new FileOperationStatement(op, parseFileName(line)));
                continue;
            }
            if (line.startsWith("COMPUTE")) {
                statements.add(parseCompute(line));
                continue;
            }
            if (line.startsWith("MOVE")) {
                statements.add(parseMove(line));
            }
        }
        return statements;
    }

    private int parseIf(List<String> lines, int index, List<CobolStatement> statements) {
        String line = lines.get(index).trim();
        String condition = line.substring(2).trim();
        List<String> thenLines = new ArrayList<>();
        List<String> elseLines = new ArrayList<>();
        boolean inElse = false;
        int i = index + 1;
        for (; i < lines.size(); i++) {
            String current = lines.get(i).trim();
            if (current.isEmpty()) {
                continue;
            }
            if (current.startsWith("ELSE")) {
                inElse = true;
                String afterElse = current.substring(4).trim();
                if (!afterElse.isEmpty() && !afterElse.equals(".")) {
                    (inElse ? elseLines : thenLines).add(afterElse);
                }
                continue;
            }
            if (current.startsWith("END-IF")) {
                break;
            }
            if (inElse) {
                elseLines.add(current);
            } else {
                thenLines.add(current);
            }
        }
        statements.add(new IfStatement(normalizeCondition(condition),
                parseStatements(String.join("\n", thenLines)),
                parseStatements(String.join("\n", elseLines))));
        return i;
    }

    private int parseEvaluate(List<String> lines, int index, List<CobolStatement> statements) {
        String expression = lines.get(index).trim().substring("EVALUATE".length()).trim();
        List<EvaluateStatement.EvaluateWhenBranch> branches = new ArrayList<>();
        List<String> accumulator = new ArrayList<>();
        String currentCondition = "OTHER";
        int i = index + 1;
        for (; i < lines.size(); i++) {
            String current = lines.get(i).trim();
            if (current.startsWith("WHEN")) {
                if (!accumulator.isEmpty()) {
                    branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                            parseStatements(String.join("\n", accumulator))));
                    accumulator.clear();
                }
                currentCondition = current.substring(4).trim();
                continue;
            }
            if (current.startsWith("WHEN OTHER")) {
                if (!accumulator.isEmpty()) {
                    branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                            parseStatements(String.join("\n", accumulator))));
                    accumulator.clear();
                }
                currentCondition = "OTHER";
                continue;
            }
            if (current.startsWith("END-EVALUATE")) {
                break;
            }
            accumulator.add(current);
        }
        if (!accumulator.isEmpty()) {
            branches.add(new EvaluateStatement.EvaluateWhenBranch(currentCondition,
                    parseStatements(String.join("\n", accumulator))));
        }
        statements.add(new EvaluateStatement(expression, branches));
        return i;
    }

    private int parseExecSql(List<String> lines, int index, List<CobolStatement> statements) {
        StringBuilder sql = new StringBuilder();
        for (int i = index; i < lines.size(); i++) {
            String current = lines.get(i);
            sql.append(current).append('\n');
            if (current.trim().toUpperCase(Locale.ROOT).contains("END-EXEC")) {
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
            return matcher.group(1).replaceAll("\n", " ").trim();
        }
        return raw.replaceAll("EXEC\\s+SQL", "").replace("END-EXEC", "").trim();
    }

    private PerformStatement parsePerform(String line) {
        String withoutPerform = line.substring("PERFORM".length()).trim();
        String[] parts = withoutPerform.split("\\s+THRU\\s+", 2);
        String first = parts[0].replace(".", "").trim();
        String thru = parts.length > 1 ? parts[1].replace(".", "").trim() : null;
        return new PerformStatement(first, thru);
    }

    private CobolStatement parseCall(String line) {
        String remainder = line.substring("CALL".length()).trim();
        String[] parts = remainder.split("\s+USING\s+", 2);
        String target = parts[0].replace("\"", "").replace("'", "");
        List<String> args = new ArrayList<>();
        if (parts.length > 1) {
            args.addAll(Arrays.stream(parts[1].split(",|\s+")).map(String::trim).filter(s -> !s.isEmpty()).toList());
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

    private String parseFileName(String line) {
        String[] parts = line.split("\\s+");
        return parts.length > 1 ? parts[1].replace(".", "").toUpperCase(Locale.ROOT) : "UNKNOWN";
    }

    private ComputeStatement parseCompute(String line) {
        String remainder = line.substring("COMPUTE".length()).trim();
        String[] parts = remainder.split("=", 2);
        if (parts.length != 2) {
            return new ComputeStatement("RESULT", remainder);
        }
        String target = parts[0].trim();
        String expression = parts[1].replace(".", "").trim();
        return new ComputeStatement(target, expression);
    }

    private MoveStatement parseMove(String line) {
        String trimmed = line.replace(".", "").trim();
        String remainder = trimmed.substring("MOVE".length()).trim();
        String[] parts = remainder.split("\\s+TO\\s+", 2);
        if (parts.length != 2) {
            return new MoveStatement(remainder, remainder);
        }
        return new MoveStatement(parts[0].trim(), parts[1].trim());
    }

    private String normalizeCondition(String condition) {
        return condition.replace("THEN", "").replace(".", "").trim();
    }

    private ControlFlowGraph buildControlFlowGraph(Map<String, CobolParagraph> paragraphs) {
        ControlFlowGraph.Builder builder = ControlFlowGraph.builder();
        String previous = null;
        for (CobolParagraph paragraph : paragraphs.values()) {
            builder.ensureNode(paragraph.getName());
            if (previous != null) {
                builder.addEdge(previous, paragraph.getName());
            }
            previous = paragraph.getName();
            for (CobolStatement statement : paragraph.getStatements()) {
                if (statement instanceof PerformStatement perform) {
                    builder.addEdge(paragraph.getName(), perform.getParagraph());
                    if (perform.getThroughParagraph() != null) {
                        builder.addEdge(perform.getParagraph(), perform.getThroughParagraph());
                    }
                }
            }
        }
        return builder.build();
    }
}
