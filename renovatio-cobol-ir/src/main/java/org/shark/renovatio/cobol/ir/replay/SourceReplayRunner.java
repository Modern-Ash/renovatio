package org.shark.renovatio.cobol.ir.replay;

import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;
import org.shark.renovatio.domain.model.ReplayRunner;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Replay adapter for the deterministic COBOL subset represented by the IR. */
public final class SourceReplayRunner implements ReplayRunner {
    private final CobolIntermediateModel program;
    private final Map<String, List<Map<String, ?>>> files;
    private final Map<String, Map<String, ?>> db2Responses;

    public SourceReplayRunner(Path source) throws IOException { this(new SimpleCobolIrParser().parse(source)); }
    public SourceReplayRunner(String source) { this(new SimpleCobolIrParser().parse(source)); }
    public SourceReplayRunner(String source, Map<String, List<Map<String, ?>>> files, Map<String, Map<String, ?>> db2Responses) {
        this(new SimpleCobolIrParser().parse(source), files, db2Responses);
    }
    public SourceReplayRunner(CobolIntermediateModel program) { this(program, Map.of(), Map.of()); }
    public SourceReplayRunner(CobolIntermediateModel program, Map<String, List<Map<String, ?>>> files) {
        this(program, files, Map.of());
    }
    public SourceReplayRunner(CobolIntermediateModel program, Map<String, List<Map<String, ?>>> files, Map<String, Map<String, ?>> db2Responses) {
        this.program = Objects.requireNonNull(program); this.files = files == null ? Map.of() : Map.copyOf(files); this.db2Responses = db2Responses == null ? Map.of() : Map.copyOf(db2Responses);
    }

    @Override public ReplayResult run(ReplayInput input) {
        Map<String,Object> state = new LinkedHashMap<>();
        input.values().forEach((k,v) -> state.put(k.toUpperCase(Locale.ROOT), v));
        List<String> changes = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        try {
            execute(program.getEntryParagraph(), state, changes, calls, new HashSet<>());
            Map<String,Object> output = new LinkedHashMap<>();
            program.getDataItems().forEach(item -> output.put(item.name(), state.getOrDefault(item.name(), defaultValue(item))));
            state.forEach(output::putIfAbsent);
            return new ReplayResult("SUCCESS", output, changes, calls, null);
        } catch (UnsupportedOperationException ex) {
            return new ReplayResult("UNSUPPORTED", state, changes, calls, ex.getMessage());
        } catch (RuntimeException ex) {
            return new ReplayResult("ERROR", state, changes, calls, ex.getMessage());
        }
    }

    private void execute(CobolParagraph p, Map<String,Object> state, List<String> changes, List<String> calls, Set<String> stack) {
        if (!stack.add(p.name())) throw new UnsupportedOperationException("Recursive PERFORM: " + p.name());
        for (CobolStatement s : p.statements()) {
            if (s instanceof MoveStatement m) assign(m.target(), value(m.source(), state), state, changes);
            else if (s instanceof ComputeStatement c) assign(c.target(), arithmetic(c.expression(), state), state, changes);
            else if (s instanceof PerformStatement f) {
                CobolParagraph target = program.findParagraph(f.paragraph()).orElseThrow(() -> new UnsupportedOperationException("Missing paragraph: " + f.paragraph()));
                execute(target, state, changes, calls, stack);
            } else if (s instanceof CallStatement c) calls.add(c.target());
            else if (s instanceof FileOperationStatement f) fileOperation(f, state, changes);
            else if (s instanceof Db2Statement d) db2Operation(d, state, calls);
            else if (s instanceof IfStatement i) {
                List<CobolStatement> branch = condition(i.condition(), state) ? i.thenStatements() : i.elseStatements();
                executeStatements(branch, state, changes, calls, stack);
            }
            else if (s instanceof EvaluateStatement e) executeEvaluate(e, state, changes, calls, stack);
            else throw new UnsupportedOperationException("Unsupported statement: " + s.getClass().getSimpleName());
        }
        stack.remove(p.name());
    }
    private void executeStatements(List<CobolStatement> statements, Map<String,Object> state, List<String> changes, List<String> calls, Set<String> stack) {
        for (CobolStatement statement : statements) {
            if (statement instanceof MoveStatement m) assign(m.target(), value(m.source(), state), state, changes);
            else if (statement instanceof ComputeStatement c) assign(c.target(), arithmetic(c.expression(), state), state, changes);
            else if (statement instanceof CallStatement c) calls.add(c.target());
            else if (statement instanceof FileOperationStatement f) fileOperation(f, state, changes);
            else if (statement instanceof Db2Statement d) db2Operation(d, state, calls);
            else if (statement instanceof IfStatement i) executeStatements(condition(i.condition(), state) ? i.thenStatements() : i.elseStatements(), state, changes, calls, stack);
            else if (statement instanceof EvaluateStatement e) executeEvaluate(e, state, changes, calls, stack);
            else throw new UnsupportedOperationException("Unsupported nested statement: " + statement.getClass().getSimpleName());
        }
    }
    private void executeEvaluate(EvaluateStatement evaluate, Map<String,Object> state, List<String> changes, List<String> calls, Set<String> stack) {
        Object subject = value(evaluate.expression(), state);
        EvaluateStatement.EvaluateWhenBranch other = null;
        for (var branch : evaluate.branches()) {
            String c = branch.condition().trim();
            if (c.equalsIgnoreCase("OTHER")) { other = branch; continue; }
            if (String.valueOf(subject).equalsIgnoreCase(String.valueOf(value(c, state)))) {
                executeStatements(branch.statements(), state, changes, calls, stack);
                return;
            }
        }
        if (other != null) executeStatements(other.statements(), state, changes, calls, stack);
    }
    private void fileOperation(FileOperationStatement operation, Map<String,Object> state, List<String> changes) {
        String name = operation.fileName().toUpperCase(Locale.ROOT);
        List<Map<String, ?>> records = files.getOrDefault(name, List.of());
        switch (operation.operationType()) {
            case READ -> { if (records.isEmpty()) { state.put("FILE-STATUS", "10"); return; } records.get(0).forEach((k,v) -> state.put(k.toUpperCase(Locale.ROOT), v)); state.put("FILE-STATUS", "00"); changes.add("READ:" + name); }
            case WRITE, REWRITE -> { state.put("FILE-STATUS", "00"); changes.add(operation.operationType() + ":" + name); }
            case OPEN, CLOSE, DELETE -> changes.add(operation.operationType() + ":" + name);
        }
    }
    private void db2Operation(Db2Statement statement, Map<String,Object> state, List<String> calls) {
        String sql = statement.sql().trim(); calls.add("DB2:" + sql);
        Map<String, ?> response = db2Responses.get(sql);
        if (response == null) { state.put("SQLCODE", -811); return; }
        response.forEach((k, v) -> state.put(k.toUpperCase(Locale.ROOT), v)); state.putIfAbsent("SQLCODE", 0);
    }
    private static boolean condition(String expression, Map<String,Object> state) {
        String c = expression.trim().replaceAll("\\s+", " ");
        String[] parts = c.split("\\s+(IS NOT|NOT EQUAL TO|EQUAL TO|GREATER THAN|LESS THAN|=|>|<)\\s+", 2);
        if (parts.length != 2) throw new UnsupportedOperationException("Unsupported IF condition: " + expression);
        String op = c.substring(parts[0].length(), c.length() - parts[1].length()).trim().toUpperCase(Locale.ROOT);
        Object left = value(parts[0], state), right = value(parts[1], state);
        int cmp;
        if (left instanceof Number l && right instanceof Number r) cmp = Double.compare(l.doubleValue(), r.doubleValue());
        else cmp = String.valueOf(left).compareTo(String.valueOf(right));
        return switch (op) { case "=", "EQUAL TO" -> cmp == 0; case "IS NOT", "NOT EQUAL TO" -> cmp != 0; case ">", "GREATER THAN" -> cmp > 0; case "<", "LESS THAN" -> cmp < 0; default -> throw new UnsupportedOperationException("Unsupported IF operator: " + op); };
    }
    private static void assign(String target, Object value, Map<String,Object> state, List<String> changes) {
        String key = target.trim().toUpperCase(Locale.ROOT); state.put(key, value); changes.add(key);
    }
    private static Object value(String token, Map<String,Object> state) {
        String t = token.trim();
        if ((t.startsWith("'") && t.endsWith("'")) || (t.startsWith("\"") && t.endsWith("\""))) return t.substring(1, t.length()-1);
        if (t.matches("[-+]?\\d+(\\.\\d+)?")) return Double.valueOf(t);
        return state.getOrDefault(t.toUpperCase(Locale.ROOT), t);
    }
    private static double arithmetic(String expression, Map<String,Object> state) {
        String[] parts = expression.trim().split("\\s*([+\\-*/])\\s*");
        if (parts.length == 0) throw new IllegalArgumentException("Empty COMPUTE expression");
        double result = ((Number) value(parts[0], state)).doubleValue();
        var operators = expression.trim().replaceAll("[^+\\-*/]", "").toCharArray();
        for (int i=1; i<parts.length; i++) { double n=((Number)value(parts[i], state)).doubleValue(); switch (operators[i-1]) { case '+' -> result+=n; case '-' -> result-=n; case '*' -> result*=n; case '/' -> result/=n; default -> throw new IllegalArgumentException("Unsupported operator"); } }
        return result;
    }
    private static Object defaultValue(CobolDataItem item) { return "String".equals(item.javaType()) ? "" : 0d; }
}
