package org.shark.renovatio.jcl.parse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** JCL COND semantics: a normal condition skips the step when any predicate is true. */
public record CondClause(Kind kind, List<Predicate> predicates, String source) {
    private static final int MAX_RETURN_CODE = 4095;

    public CondClause {
        if (kind == null) throw new NullPointerException("kind");
        predicates = predicates == null ? List.of() : List.copyOf(predicates);
        source = source == null ? kind.name() : source.trim();
        if (kind == Kind.NORMAL && predicates.isEmpty())
            throw new IllegalArgumentException("normal COND requires a predicate");
    }

    public static CondClause parse(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("COND value is required");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("COND=")) normalized = normalized.substring(5).trim();
        if (normalized.equals("EVEN")) return new CondClause(Kind.EVEN, List.of(), value);
        if (normalized.equals("ONLY")) return new CondClause(Kind.ONLY, List.of(), value);

        String body = stripOuter(normalized);
        List<String> tuples = tupleBodies(body);
        if (tuples.isEmpty()) tuples = List.of(body);
        List<Predicate> predicates = new ArrayList<>();
        for (String tuple : tuples) {
            List<String> parts = splitTopLevel(tuple);
            if (parts.size() < 2 || parts.size() > 3)
                throw new IllegalArgumentException("invalid COND predicate: " + tuple);
            int code = Integer.parseInt(parts.get(0).trim());
            Operator operator = Operator.valueOf(parts.get(1).trim());
            Optional<String> step = parts.size() == 3
                    ? Optional.of(parts.get(2).trim().toUpperCase(Locale.ROOT)) : Optional.empty();
            predicates.add(new Predicate(code, operator, step));
        }
        return new CondClause(Kind.NORMAL, predicates, value);
    }

    public boolean shouldSkip(Map<String, Integer> returnCodes, boolean priorStepAbended) {
        Map<String, Integer> codes = returnCodes == null ? Map.of() : returnCodes;
        return switch (kind) {
            case EVEN -> false;
            case ONLY -> !priorStepAbended;
            case NORMAL -> predicates.stream().anyMatch(predicate -> {
                if (predicate.referencedStep().isPresent()) {
                    Integer rc = codes.get(predicate.referencedStep().get());
                    return rc != null && predicate.matches(rc);
                }
                return codes.values().stream().anyMatch(predicate::matches);
            });
        };
    }

    /** A deterministic table whose boolean value means RUN (rather than skip). */
    public Map<String, Boolean> truthTable() {
        LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
        if (kind == Kind.EVEN) {
            result.put("PRIOR=SUCCESS", true);
            result.put("PRIOR=ABEND", true);
        } else if (kind == Kind.ONLY) {
            result.put("PRIOR=SUCCESS", false);
            result.put("PRIOR=ABEND", true);
        } else {
            LinkedHashMap<String, List<Predicate>> grouped = new LinkedHashMap<>();
            for (Predicate predicate : predicates)
                grouped.computeIfAbsent(predicate.referencedStep().orElse("ANY"), ignored -> new ArrayList<>())
                        .add(predicate);
            grouped.forEach((label, matching) -> {
                for (int rc = 0; rc <= MAX_RETURN_CODE; rc++) {
                    int returnCode = rc;
                    boolean skip = matching.stream().anyMatch(predicate -> predicate.matches(returnCode));
                    result.put(label + ".RC=" + rc, !skip);
                }
            });
        }
        return java.util.Collections.unmodifiableMap(result);
    }

    public String normalizedExpression() {
        if (kind != Kind.NORMAL) return kind.name();
        return predicates.stream().map(Predicate::normalized).reduce((left, right) -> left + " OR " + right).orElseThrow();
    }

    private static String stripOuter(String value) {
        String result = value;
        while (result.startsWith("(") && result.endsWith(")") && enclosesAll(result))
            result = result.substring(1, result.length() - 1).trim();
        return result;
    }

    private static boolean enclosesAll(String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '(') depth++;
            else if (value.charAt(i) == ')' && --depth == 0) return i == value.length() - 1;
        }
        return false;
    }

    private static List<String> tupleBodies(String value) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character == '(' && depth++ == 0) start = i + 1;
            else if (character == ')' && --depth == 0 && start >= 0) result.add(value.substring(start, i));
        }
        return result;
    }

    static List<String> splitTopLevel(String value) {
        List<String> result = new ArrayList<>();
        int depth = 0;
        int start = 0;
        boolean quoted = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '\'') quoted = !quoted;
            else if (!quoted && character == '(') depth++;
            else if (!quoted && character == ')') depth--;
            else if (!quoted && depth == 0 && character == ',') {
                result.add(value.substring(start, index).trim());
                start = index + 1;
            }
        }
        result.add(value.substring(start).trim());
        return result;
    }

    public enum Kind { NORMAL, EVEN, ONLY }
    public enum Operator { GT, GE, EQ, LT, LE, NE }

    public record Predicate(int code, Operator operator, Optional<String> referencedStep) {
        public Predicate {
            if (code < 0 || code > 4095) throw new IllegalArgumentException("return code out of range");
            if (operator == null) throw new NullPointerException("operator");
            referencedStep = referencedStep == null ? Optional.empty()
                    : referencedStep.map(value -> value.trim().toUpperCase(Locale.ROOT));
        }
        public boolean matches(int returnCode) {
            return switch (operator) {
                case GT -> returnCode > code;
                case GE -> returnCode >= code;
                case EQ -> returnCode == code;
                case LT -> returnCode < code;
                case LE -> returnCode <= code;
                case NE -> returnCode != code;
            };
        }
        public Map<String, Boolean> truthTable() {
            LinkedHashMap<String, Boolean> result = new LinkedHashMap<>();
            String label = referencedStep.orElse("ANY");
            for (int rc = 0; rc <= MAX_RETURN_CODE; rc++) result.put(label + ".RC=" + rc, !matches(rc));
            return java.util.Collections.unmodifiableMap(result);
        }

        public String normalized() {
            return referencedStep.orElse("ANY") + ".RC " + operator + " " + code;
        }
    }
}
