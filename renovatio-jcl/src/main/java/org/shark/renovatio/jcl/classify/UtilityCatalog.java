package org.shark.renovatio.jcl.classify;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Deterministic catalog of the standard utilities admitted by F7. */
public final class UtilityCatalog {
    private static final Set<String> SUPPORTED = Set.of("SORT", "MERGE", "IEBGENER", "IDCAMS", "ICETOOL");

    public Optional<String> recognize(String program) {
        if (program == null) return Optional.empty();
        String normalized = program.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED.contains(normalized) ? Optional.of(normalized) : Optional.empty();
    }

    public boolean supportsIdcamsOperation(String operation) {
        if (operation == null) return false;
        String normalized = operation.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("REPRO") || normalized.startsWith("DELETE") || normalized.startsWith("DEFINE");
    }
}
