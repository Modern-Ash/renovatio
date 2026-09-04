package org.shark.renovatio.jcl.parse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parsed EXEC statement and its attached DD statements. */
public record JclStep(String stepName, ExecKind execKind, String executable,
                      Optional<CondClause> condition, Optional<String> ifExpression,
                      List<DdStatement> ddStatements, Map<String, String> parameters,
                      int sourceLine) {
    public JclStep {
        if (stepName == null || stepName.isBlank()) throw new IllegalArgumentException("stepName is required");
        stepName = stepName.toUpperCase(Locale.ROOT);
        if (execKind == null) throw new NullPointerException("execKind");
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("executable is required");
        executable = executable.toUpperCase(Locale.ROOT);
        condition = condition == null ? Optional.empty() : condition;
        ifExpression = ifExpression == null ? Optional.empty() : ifExpression.map(String::trim);
        ddStatements = ddStatements == null ? List.of() : List.copyOf(ddStatements);
        parameters = parameters == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        if (sourceLine < 1) throw new IllegalArgumentException("sourceLine must be positive");
    }

    public enum ExecKind { PROGRAM, PROC }
}
