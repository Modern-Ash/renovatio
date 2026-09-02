package org.shark.renovatio.jcl.parse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parsed DD statement, including optional in-stream records. */
public record DdStatement(String ddName, Optional<String> dsn, String disposition,
                          boolean sysout, List<String> instreamData,
                          Map<String, String> parameters) {
    public DdStatement {
        if (ddName == null || ddName.isBlank()) throw new IllegalArgumentException("ddName is required");
        ddName = ddName.toUpperCase(Locale.ROOT);
        dsn = dsn == null ? Optional.empty() : dsn.map(String::trim).filter(value -> !value.isEmpty());
        disposition = disposition == null ? "" : disposition.trim().toUpperCase(Locale.ROOT);
        instreamData = instreamData == null ? List.of() : List.copyOf(instreamData);
        parameters = parameters == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    public boolean temporary() {
        return dsn.map(value -> value.startsWith("&&")).orElse(false)
                || disposition.contains("PASS");
    }
}
