package org.shark.renovatio.jcl.characterization;

import org.shark.renovatio.semantic.ir.BatchDataset;
import org.shark.renovatio.semantic.ir.BatchJob;
import org.shark.renovatio.semantic.ir.BatchStep;
import org.shark.renovatio.semantic.ir.ConditionGraph;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** In-process hook for comparing a projected batch job with reference fixture outputs. */
public final class BatchCharacterizationHarness {
    public RunResult run(BatchJob job, Map<String, List<String>> inputs, StepExecutor executor) throws Exception {
        LinkedHashMap<String, List<String>> data = new LinkedHashMap<>();
        if (inputs != null) inputs.forEach((key, value) -> data.put(key, new ArrayList<>(value)));
        LinkedHashMap<String, Integer> returnCodes = new LinkedHashMap<>();
        List<String> executed = new ArrayList<>();
        for (BatchStep step : job.steps()) {
            if (!shouldRun(job, step, returnCodes)) continue;
            int returnCode = executor.execute(step, data);
            returnCodes.put(step.stepName(), returnCode);
            executed.add(step.stepName());
        }
        job.datasets().stream().filter(dataset -> dataset.access() == BatchDataset.AccessKind.TEMP)
                .forEach(dataset -> {
                    data.remove(dataset.ddName());
                    dataset.resourceReference().ifPresent(data::remove);
                });
        LinkedHashMap<String, List<String>> immutable = new LinkedHashMap<>();
        data.forEach((key, value) -> immutable.put(key, List.copyOf(value)));
        return new RunResult(java.util.Collections.unmodifiableMap(immutable),
                java.util.Collections.unmodifiableMap(returnCodes), List.copyOf(executed));
    }

    private static boolean shouldRun(BatchJob job, BatchStep step, Map<String, Integer> returnCodes) {
        for (ConditionGraph.Guard guard : job.conditionGraph().guards()) {
            if (!guard.memberStepIds().contains(step.id())) continue;
            if (guard.truthTable().containsKey("PREDICATE=TRUE")) {
                if (!IfExpression.evaluate(guard.predicate(), returnCodes)) return false;
                continue;
            }
            if (guard.truthTable().containsKey("PRIOR=SUCCESS")) {
                Integer priorReturnCode = returnCodes.isEmpty() ? null : returnCodes.values().stream()
                        .reduce((previous, current) -> current).orElse(null);
                String priorState = priorReturnCode != null && priorReturnCode < 0
                        ? "PRIOR=ABEND" : "PRIOR=SUCCESS";
                Boolean run = guard.truthTable().get(priorState);
                if (run != null && !run) return false;
                continue;
            }
            if (guard.referencedStepId().isEmpty()) continue;
            BatchStep referenced = job.steps().stream()
                    .filter(value -> value.id().equals(guard.referencedStepId().get())).findFirst().orElseThrow();
            Integer rc = returnCodes.get(referenced.stepName());
            if (rc == null) return false;
            Boolean run = guard.truthTable().get(referenced.stepName() + ".RC=" + rc);
            if (run == null) run = guard.truthTable().get("ANY.RC=" + rc);
            if (run != null && !run) return false;
        }
        return true;
    }

    /** Evaluates the bounded JCL IF expression subset used by the parser and emitted guards. */
    private static final class IfExpression {
        static boolean evaluate(String expression, Map<String, Integer> returnCodes) {
            Parser parser = new Parser(tokens(expression), returnCodes);
            boolean result = parser.or();
            if (parser.hasNext()) throw new IllegalArgumentException(
                    "unsupported IF expression near " + parser.peek());
            return result;
        }

        private static List<String> tokens(String expression) {
            List<String> result = new ArrayList<>();
            for (int index = 0; index < expression.length();) {
                char value = expression.charAt(index);
                if (Character.isWhitespace(value)) {
                    index++;
                } else if (value == '(' || value == ')') {
                    result.add(String.valueOf(value));
                    index++;
                } else if ("<>=!^".indexOf(value) >= 0) {
                    int end = index + 1;
                    if (end < expression.length() && expression.charAt(end) == '=') end++;
                    result.add(expression.substring(index, end));
                    index = end;
                } else {
                    int end = index + 1;
                    while (end < expression.length() && !Character.isWhitespace(expression.charAt(end))
                            && "()<>=!^".indexOf(expression.charAt(end)) < 0) end++;
                    result.add(expression.substring(index, end).toUpperCase(java.util.Locale.ROOT));
                    index = end;
                }
            }
            return result;
        }

        private static final class Parser {
            private final List<String> tokens;
            private final Map<String, Integer> returnCodes;
            private int index;

            Parser(List<String> tokens, Map<String, Integer> returnCodes) {
                this.tokens = tokens;
                this.returnCodes = returnCodes;
            }

            boolean or() {
                boolean result = and();
                while (accept("OR") || accept("|")) result = and() || result;
                return result;
            }

            boolean and() {
                boolean result = unary();
                while (accept("AND") || accept("&")) result = unary() && result;
                return result;
            }

            boolean unary() {
                if (accept("NOT")) return !unary();
                if (accept("(")) {
                    boolean value = or();
                    require(")");
                    return value;
                }
                if (accept("TRUE")) return true;
                if (accept("FALSE")) return false;
                String left = next();
                String operator = next();
                String right = next();
                Integer actual = returnCode(left);
                if (actual == null) return false;
                int expected;
                try {
                    expected = Integer.parseInt(right);
                } catch (NumberFormatException exception) {
                    throw new IllegalArgumentException("IF comparison requires a numeric return code: " + right,
                            exception);
                }
                return switch (operator) {
                    case "=", "==", "EQ" -> actual == expected;
                    case "!=", "^=", "NE" -> actual != expected;
                    case ">", "GT" -> actual > expected;
                    case ">=", "GE" -> actual >= expected;
                    case "<", "LT" -> actual < expected;
                    case "<=", "LE" -> actual <= expected;
                    default -> throw new IllegalArgumentException("unsupported IF operator " + operator);
                };
            }

            private Integer returnCode(String operand) {
                if (!operand.endsWith(".RC"))
                    throw new IllegalArgumentException("unsupported IF operand " + operand);
                return returnCodes.get(operand.substring(0, operand.length() - 3));
            }

            boolean accept(String value) {
                if (!hasNext() || !peek().equals(value)) return false;
                index++;
                return true;
            }

            void require(String value) {
                if (!accept(value)) throw new IllegalArgumentException("expected " + value);
            }

            String next() {
                if (!hasNext()) throw new IllegalArgumentException("incomplete IF expression");
                return tokens.get(index++);
            }

            String peek() { return tokens.get(index); }
            boolean hasNext() { return index < tokens.size(); }
        }
    }

    @FunctionalInterface
    public interface StepExecutor {
        int execute(BatchStep step, Map<String, List<String>> datasets) throws Exception;
    }

    public record RunResult(Map<String, List<String>> datasets, Map<String, Integer> returnCodes,
                            List<String> executedSteps) { }
}
