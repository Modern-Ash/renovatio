package org.shark.renovatio.jcl.classify;

import org.shark.renovatio.jcl.parse.JclStep;
import org.shark.renovatio.semantic.ir.BatchStep;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Applies migrated-program, utility, then residue precedence. */
public final class StepClassifier {
    private final UtilityCatalog utilities;

    public StepClassifier() { this(new UtilityCatalog()); }
    public StepClassifier(UtilityCatalog utilities) { this.utilities = java.util.Objects.requireNonNull(utilities); }

    public Classification classify(JclStep step, Set<String> migratedPrograms) {
        Set<String> migrated = migratedPrograms == null ? Set.of() : migratedPrograms.stream()
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(java.util.stream.Collectors.toUnmodifiableSet());
        String executable = step.executable().toUpperCase(Locale.ROOT);
        if (step.execKind() == JclStep.ExecKind.PROGRAM && migrated.contains(executable)) {
            return new Classification(BatchStep.Kind.MIGRATED_PROGRAM_CALL,
                    Optional.of(executable), Optional.empty(), Optional.empty());
        }
        Optional<String> utility = step.execKind() == JclStep.ExecKind.PROGRAM
                ? utilities.recognize(executable) : Optional.empty();
        if (utility.isPresent()) {
            return new Classification(BatchStep.Kind.STANDARD_UTILITY,
                    Optional.empty(), utility, Optional.empty());
        }
        String reason = step.execKind() == JclStep.ExecKind.PROC
                ? "Manual action: resolve catalogued PROC " + executable
                : "Manual action: classify unsupported batch program " + executable;
        return new Classification(BatchStep.Kind.RESIDUE, Optional.empty(), Optional.empty(), Optional.of(reason));
    }

    public record Classification(BatchStep.Kind kind, Optional<String> programRef,
                                 Optional<String> utility, Optional<String> residueReason) { }
}
