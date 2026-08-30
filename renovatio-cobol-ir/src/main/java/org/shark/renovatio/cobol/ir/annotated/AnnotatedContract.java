package org.shark.renovatio.cobol.ir.annotated;

import java.util.Objects;
import java.util.regex.Pattern;

final class AnnotatedContract {
    static final Pattern HASH = Pattern.compile("[0-9a-f]{64}");
    static final Pattern TOOL_RUN = Pattern.compile("tool-[0-9]{8}t[0-9]{14}z");

    private AnnotatedContract() {}

    static String text(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }

    static String hash(String value, String field) {
        text(value, field);
        if (!HASH.matcher(value).matches()) throw new IllegalArgumentException(field + " must be lowercase SHA-256");
        return value;
    }
}
