package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;
import java.util.Optional;

/** A value or inclusive range accepted by a COBOL level-88 condition name. */
public record Level88Value(String value, String through) {

    public Level88Value {
        value = Objects.requireNonNull(value, "value");
    }

    public static Level88Value exact(String value) {
        return new Level88Value(value, null);
    }

    public static Level88Value range(String lowerBound, String upperBound) {
        return new Level88Value(lowerBound, Objects.requireNonNull(upperBound, "upperBound"));
    }

    public boolean isRange() {
        return through != null;
    }

    public Optional<String> getThrough() {
        return Optional.ofNullable(through);
    }
}
