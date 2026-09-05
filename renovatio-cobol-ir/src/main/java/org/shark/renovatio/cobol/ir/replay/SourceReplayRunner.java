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

    public SourceReplayRunner(Path source) throws IOException { this(new SimpleCobolIrParser().parse(source)); }
    public SourceReplayRunner(String source) { this(new SimpleCobolIrParser().parse(source)); }
    public SourceReplayRunner(CobolIntermediateModel program) { this.program = Objects.requireNonNull(program); }

    @Override public ReplayResult run(ReplayInput input) {
        Map<String,Object> state = new LinkedHashMap<>();
        input.values().forEach((k,v) -> state.put(k.toUpperCase(Locale.ROOT), v));
        List<String> changes = new ArrayList<>();
        List<String> calls = new ArrayList<>();
        try {
            execute(program.getEntryParagraph(), state, changes, calls, new HashSet<>());
            Map<String,Object> output = new LinkedHashMap<>();
            program.getDataItems().forEach(item -> output.put(item.name(), state.getOrDefault(item.name(), defaultValue(item))));
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
            else if (s instanceof IfStatement) throw new UnsupportedOperationException("IF replay requires runtime condition evaluator");
            else throw new UnsupportedOperationException("Unsupported statement: " + s.getClass().getSimpleName());
        }
        stack.remove(p.name());
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
