package org.shark.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/** Typed representation of a COBOL level-88 condition attached to its parent item. */
public record Level88Condition(String name, String parentDataName, List<Level88Value> values) {

    public Level88Condition {
        name = Objects.requireNonNull(name, "name");
        parentDataName = Objects.requireNonNull(parentDataName, "parentDataName");
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
    }
}
