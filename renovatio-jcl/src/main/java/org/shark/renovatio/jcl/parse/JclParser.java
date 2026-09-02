package org.shark.renovatio.jcl.parse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.HexFormat;

/** Deterministic parser for the bounded F7 JCL subset. */
public final class JclParser {
    private final JclLexer lexer;

    public JclParser() { this(new JclLexer()); }
    public JclParser(JclLexer lexer) { this.lexer = java.util.Objects.requireNonNull(lexer); }

    public JclJob parse(String path, String content) {
        List<JclJob> jobs = parseAll(List.of(new JclSource(path, content)));
        if (jobs.isEmpty()) throw new IllegalArgumentException("JCL contains no JOB card");
        return jobs.get(0);
    }

    public List<JclJob> parseAll(List<JclSource> sources) {
        List<JclSource> ordered = sources == null ? List.of() : sources.stream()
                .sorted(java.util.Comparator.comparing(JclSource::path)).toList();
        Map<String, ProcDefinition> procedures = collectProcedures(ordered);
        List<JclJob> jobs = new ArrayList<>();
        for (JclSource source : ordered) jobs.addAll(parseSource(source, procedures));
        return List.copyOf(jobs);
    }

    private List<JclJob> parseSource(JclSource source, Map<String, ProcDefinition> procedures) {
        List<JclLexer.Statement> statements = lexer.lex(source.content());
        List<JclJob> result = new ArrayList<>();
        JobBuilder job = null;
        StepBuilder step = null;
        String ifExpression = null;
        boolean inProc = false;
        for (JclLexer.Statement statement : statements) {
            switch (statement.operation()) {
                case "PROC" -> inProc = true;
                case "PEND" -> inProc = false;
                case "JOB" -> {
                    if (job != null) result.add(job.build());
                    job = new JobBuilder(statement.name(), source.path(), sha256(source.content()));
                    step = null;
                }
                case "SET" -> {
                    if (job != null) job.symbols.putAll(assignments(statement.operands(), job.symbols));
                }
                case "IF" -> { if (job != null && !inProc) ifExpression = substitute(statement.operands(), job.symbols); }
                case "ELSE" -> { if (ifExpression != null) ifExpression = "NOT (" + ifExpression + ")"; }
                case "ENDIF" -> ifExpression = null;
                case "EXEC" -> {
                    if (job == null || inProc) break;
                    if (step != null) job.steps.add(step.build());
                    StepBuilder parsed = parseExec(statement, job.symbols, ifExpression);
                    if (parsed.execKind == JclStep.ExecKind.PROC) {
                        ProcDefinition proc = procedures.get(parsed.executable);
                        if (proc == null) {
                            job.unresolved.add(new JclJob.UnresolvedProc(
                                    parsed.stepName, parsed.executable, statement.line()));
                            step = parsed;
                        } else {
                            job.steps.addAll(expandProc(proc, parsed, procedures, job, job.symbols, 0));
                            step = null;
                        }
                    } else {
                        step = parsed;
                    }
                }
                case "DD" -> {
                    if (step != null && job != null && !inProc)
                        step.dds.add(parseDd(statement, job.symbols));
                }
                default -> { }
            }
        }
        if (job != null) {
            if (step != null) job.steps.add(step.build());
            result.add(job.build());
        }
        return result;
    }

    private static List<JclStep> expandProc(ProcDefinition procedure, StepBuilder call,
                                            Map<String, ProcDefinition> procedures, JobBuilder job,
                                            Map<String, String> inheritedSymbols, int depth) {
        LinkedHashMap<String, String> symbols = new LinkedHashMap<>(inheritedSymbols);
        symbols.putAll(procedure.defaults);
        symbols.putAll(call.parameters);
        List<JclStep> result = new ArrayList<>();
        StepBuilder current = null;
        String nestedIf = call.ifExpression.orElse(null);
        for (JclLexer.Statement statement : procedure.statements) {
            switch (statement.operation()) {
                case "SET" -> symbols.putAll(assignments(statement.operands(), symbols));
                case "IF" -> nestedIf = substitute(statement.operands(), symbols);
                case "ELSE" -> { if (nestedIf != null) nestedIf = "NOT (" + nestedIf + ")"; }
                case "ENDIF" -> nestedIf = call.ifExpression.orElse(null);
                case "EXEC" -> {
                    if (current != null) result.add(current.build());
                    JclLexer.Statement named = new JclLexer.Statement(
                            call.stepName + "_" + statement.name(), statement.operation(), statement.operands(),
                            statement.line(), statement.instreamData());
                    current = parseExec(named, symbols, nestedIf);
                    if (current.execKind == JclStep.ExecKind.PROC) {
                        ProcDefinition nested = procedures.get(current.executable);
                        if (nested != null && depth < 1) {
                            result.addAll(expandProc(nested, current, procedures, job, symbols, depth + 1));
                            current = null;
                        } else if (nested == null) {
                            job.unresolved.add(new JclJob.UnresolvedProc(current.stepName,
                                    current.executable, statement.line()));
                        }
                    }
                }
                case "DD" -> {
                    if (current != null) current.dds.add(parseDd(statement, symbols));
                }
                default -> { }
            }
        }
        if (current != null) result.add(current.build());
        return result;
    }

    private static StepBuilder parseExec(JclLexer.Statement statement, Map<String, String> symbols,
                                         String ifExpression) {
        String operands = substitute(statement.operands(), symbols);
        Map<String, String> values = assignments(operands, symbols);
        JclStep.ExecKind kind;
        String executable;
        if (operands.stripLeading().toUpperCase(Locale.ROOT).startsWith("PGM=")) {
            kind = JclStep.ExecKind.PROGRAM;
            executable = values.get("PGM");
        } else {
            kind = JclStep.ExecKind.PROC;
            executable = values.getOrDefault("PROC", firstToken(operands));
        }
        Optional<CondClause> condition = extractCond(operands).map(CondClause::parse);
        return new StepBuilder(statement.name(), kind, executable, condition,
                Optional.ofNullable(ifExpression), values, statement.line());
    }

    private static DdStatement parseDd(JclLexer.Statement statement, Map<String, String> symbols) {
        String operands = substitute(statement.operands(), symbols);
        Map<String, String> values = assignments(operands, symbols);
        Optional<String> dsn = Optional.ofNullable(values.get("DSN"));
        String disposition = values.getOrDefault("DISP", "");
        boolean sysout = values.containsKey("SYSOUT") || statement.name().equals("SYSOUT");
        return new DdStatement(statement.name(), dsn, disposition, sysout, statement.instreamData(), values);
    }

    private static Optional<String> extractCond(String operands) {
        String upper = operands.toUpperCase(Locale.ROOT);
        int start = upper.indexOf("COND=");
        if (start < 0) return Optional.empty();
        int valueStart = start + 5;
        if (valueStart >= operands.length()) return Optional.empty();
        if (operands.charAt(valueStart) != '(') {
            int end = operands.indexOf(',', valueStart);
            return Optional.of(operands.substring(valueStart, end < 0 ? operands.length() : end));
        }
        int depth = 0;
        for (int index = valueStart; index < operands.length(); index++) {
            if (operands.charAt(index) == '(') depth++;
            else if (operands.charAt(index) == ')' && --depth == 0)
                return Optional.of(operands.substring(valueStart, index + 1));
        }
        throw new IllegalArgumentException("unterminated COND");
    }

    private Map<String, ProcDefinition> collectProcedures(List<JclSource> sources) {
        Map<String, ProcDefinition> result = new LinkedHashMap<>();
        for (JclSource source : sources) {
            List<JclLexer.Statement> statements = lexer.lex(source.content());
            ProcDefinition current = null;
            for (JclLexer.Statement statement : statements) {
                if (statement.operation().equals("PROC")) {
                    String name = statement.name().isBlank() ? memberName(source.path()) : statement.name();
                    current = new ProcDefinition(name, assignments(statement.operands(), Map.of()), new ArrayList<>());
                } else if (statement.operation().equals("PEND") && current != null) {
                    result.put(current.name, current);
                    current = null;
                } else if (current != null) current.statements.add(statement);
            }
            if (current != null) result.put(current.name, current);
        }
        return result;
    }

    private static String memberName(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        int dot = file.indexOf('.');
        return (dot < 0 ? file : file.substring(0, dot)).toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> assignments(String value, Map<String, String> symbols) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String item : CondClause.splitTopLevel(value)) {
            int equals = item.indexOf('=');
            if (equals <= 0) continue;
            String key = item.substring(0, equals).trim().toUpperCase(Locale.ROOT);
            String raw = item.substring(equals + 1).trim();
            result.put(key, substitute(raw, symbols));
        }
        return result;
    }

    private static String substitute(String value, Map<String, String> symbols) {
        String result = value;
        for (Map.Entry<String, String> entry : symbols.entrySet())
            result = result.replace("&" + entry.getKey() + ".", entry.getValue())
                    .replace("&" + entry.getKey(), entry.getValue());
        return result;
    }

    private static String firstToken(String value) {
        int comma = value.indexOf(',');
        String result = (comma < 0 ? value : value.substring(0, comma)).trim();
        if (result.isBlank()) throw new IllegalArgumentException("EXEC requires PGM or PROC");
        return result;
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static final class JobBuilder {
        final String name;
        final String path;
        final String sourceSha256;
        final List<JclStep> steps = new ArrayList<>();
        final List<JclJob.UnresolvedProc> unresolved = new ArrayList<>();
        final Map<String, String> symbols = new LinkedHashMap<>();
        JobBuilder(String name, String path, String sourceSha256) {
            this.name = name; this.path = path; this.sourceSha256 = sourceSha256;
        }
        JclJob build() { return new JclJob(name, path, sourceSha256, steps, unresolved, symbols); }
    }

    private static final class StepBuilder {
        final String stepName;
        final JclStep.ExecKind execKind;
        final String executable;
        final Optional<CondClause> condition;
        final Optional<String> ifExpression;
        final Map<String, String> parameters;
        final int line;
        final List<DdStatement> dds = new ArrayList<>();
        StepBuilder(String stepName, JclStep.ExecKind execKind, String executable,
                    Optional<CondClause> condition, Optional<String> ifExpression,
                    Map<String, String> parameters, int line) {
            this.stepName = stepName; this.execKind = execKind; this.executable = executable;
            this.condition = condition; this.ifExpression = ifExpression;
            this.parameters = parameters; this.line = line;
        }
        JclStep build() { return new JclStep(stepName, execKind, executable, condition,
                ifExpression, dds, parameters, line); }
    }

    private static final class ProcDefinition {
        final String name;
        final Map<String, String> defaults;
        final List<JclLexer.Statement> statements;
        ProcDefinition(String name, Map<String, String> defaults, List<JclLexer.Statement> statements) {
            this.name = name.toUpperCase(Locale.ROOT); this.defaults = defaults; this.statements = statements;
        }
    }
}
