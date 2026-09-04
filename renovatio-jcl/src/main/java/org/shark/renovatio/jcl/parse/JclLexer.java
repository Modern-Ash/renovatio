package org.shark.renovatio.jcl.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Small deterministic lexer for the JCL statement boundary used by F7. */
public final class JclLexer {
    private static final Set<String> OPERATIONS = Set.of(
            "JOB", "EXEC", "DD", "SET", "PROC", "PEND", "IF", "THEN", "ELSE", "ENDIF", "INCLUDE");

    public List<Statement> lex(String content) {
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        List<Statement> result = new ArrayList<>();
        for (int index = 0; index < lines.length; index++) {
            String raw = lines[index];
            String bounded = raw.length() > 72 ? raw.substring(0, 72) : raw;
            if (bounded.startsWith("//*") || bounded.isBlank()) continue;
            if (!bounded.startsWith("//")) continue;
            String body = bounded.substring(2);
            if (body.isBlank()) continue;
            String trimmed = body.trim();
            String[] tokens = trimmed.split("\\s+", 3);
            String name;
            String operation;
            String operands;
            if (OPERATIONS.contains(tokens[0].toUpperCase(Locale.ROOT))) {
                name = "";
                operation = tokens[0];
                operands = tokens.length > 1 ? trimmed.substring(tokens[0].length()).trim() : "";
            } else if (tokens.length >= 2 && OPERATIONS.contains(tokens[1].toUpperCase(Locale.ROOT))) {
                name = tokens[0];
                operation = tokens[1];
                operands = tokens.length == 3 ? tokens[2] : "";
            } else if (!result.isEmpty()) {
                Statement previous = result.remove(result.size() - 1);
                result.add(previous.withOperands(previous.operands() + trimmed));
                continue;
            } else continue;

            List<String> data = new ArrayList<>();
            String ddOperand = operands.trim().toUpperCase(Locale.ROOT);
            if (operation.equalsIgnoreCase("DD")
                    && (ddOperand.equals("*") || ddOperand.equals("DATA") || ddOperand.startsWith("DATA,"))) {
                while (++index < lines.length && !lines[index].startsWith("/*")) data.add(lines[index]);
            }
            result.add(new Statement(name.toUpperCase(Locale.ROOT), operation.toUpperCase(Locale.ROOT),
                    operands.trim(), index + 1 - data.size(), data));
        }
        return List.copyOf(result);
    }

    public record Statement(String name, String operation, String operands, int line, List<String> instreamData) {
        public Statement {
            instreamData = instreamData == null ? List.of() : List.copyOf(instreamData);
        }
        Statement withOperands(String value) { return new Statement(name, operation, value, line, instreamData); }
        Statement withName(String value) { return new Statement(value, operation, operands, line, instreamData); }
    }
}
