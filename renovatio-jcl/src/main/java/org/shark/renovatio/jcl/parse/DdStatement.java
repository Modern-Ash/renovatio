package org.shark.renovatio.jcl.parse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parsed DD statement, including optional in-stream records. */
public record DdStatement(String ddName, Optional<String> dsn, String disposition,
                          boolean sysout, List<String> instreamData,
                          Map<String, String> parameters,
                          List<Concatenation> concatenations) {
    public DdStatement(String ddName, Optional<String> dsn, String disposition,
                       boolean sysout, List<String> instreamData,
                       Map<String, String> parameters) {
        this(ddName, dsn, disposition, sysout, instreamData, parameters, List.of());
    }

    public DdStatement {
        if (ddName == null || ddName.isBlank()) throw new IllegalArgumentException("ddName is required");
        ddName = ddName.toUpperCase(Locale.ROOT);
        dsn = dsn == null ? Optional.empty() : dsn.map(String::trim).filter(value -> !value.isEmpty());
        disposition = disposition == null ? "" : disposition.trim().toUpperCase(Locale.ROOT);
        instreamData = instreamData == null ? List.of() : List.copyOf(instreamData);
        parameters = parameters == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
        concatenations = concatenations == null ? List.of() : List.copyOf(concatenations);
    }

    public boolean temporary() {
        // A `&&name` dataset is always temporary. A DD with no DSN and a PASS disposition is a
        // system-allocated temporary. A catalogued DSN keeps its file backing even with DISP=PASS.
        return dsn.map(value -> value.startsWith("&&"))
                .orElseGet(() -> disposition.contains("PASS"));
    }

    public DdStatement append(Concatenation concatenation) {
        List<Concatenation> values = new java.util.ArrayList<>(concatenations);
        values.add(java.util.Objects.requireNonNull(concatenation, "concatenation"));
        return new DdStatement(ddName, dsn, disposition, sysout, instreamData, parameters, values);
    }

    /** One unnamed DD statement concatenated after the named statement. */
    public record Concatenation(Optional<String> dsn, String disposition,
                                List<String> instreamData, Map<String, String> parameters) {
        public Concatenation {
            dsn = dsn == null ? Optional.empty() : dsn.map(String::trim).filter(value -> !value.isEmpty());
            disposition = disposition == null ? "" : disposition.trim().toUpperCase(Locale.ROOT);
            instreamData = instreamData == null ? List.of() : List.copyOf(instreamData);
            parameters = parameters == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
            if (dsn.isEmpty() && instreamData.isEmpty())
                throw new IllegalArgumentException("concatenated DD requires DSN or in-stream data");
        }
    }
}
