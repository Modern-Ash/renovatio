package org.shark.renovatio.provider.cobol.polish;

import java.util.Objects;

public record PolishValidationChecks(
        PolishCandidateCheck schema,
        PolishCandidateCheck compilation,
        PolishCandidateCheck characterization) {

    public PolishValidationChecks {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(compilation, "compilation");
        Objects.requireNonNull(characterization, "characterization");
    }
}
