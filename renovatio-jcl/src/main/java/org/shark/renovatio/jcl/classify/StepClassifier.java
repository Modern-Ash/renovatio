package org.shark.renovatio.jcl.classify;

import org.shark.renovatio.jcl.emit.util.SortUtility;
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
        if (utility.filter("IDCAMS"::equals).isPresent() && !supportedIdcamsControl(step)) utility = Optional.empty();
        if (utility.filter(value -> value.equals("SORT") || value.equals("MERGE")).isPresent()
                && !supportedSortControl(step)) utility = Optional.empty();
        if (utility.isPresent()) {
            return new Classification(BatchStep.Kind.STANDARD_UTILITY,
                    Optional.empty(), utility, Optional.empty());
        }
        String reason = executable.equals("IDCAMS")
                ? "Manual action: unsupported IDCAMS control statement"
                : executable.equals("SORT") || executable.equals("MERGE")
                ? "Manual action: unsupported SORT/MERGE control statement"
                : step.execKind() == JclStep.ExecKind.PROC
                ? "Manual action: resolve catalogued PROC " + executable
                : "Manual action: classify unsupported batch program " + executable;
        return new Classification(BatchStep.Kind.RESIDUE, Optional.empty(), Optional.empty(), Optional.of(reason));
    }

    private static boolean supportedIdcamsControl(JclStep step) {
        Optional<String> controls = step.ddStatements().stream()
                .filter(dd -> dd.ddName().equalsIgnoreCase("SYSIN"))
                .flatMap(dd -> dd.instreamData().stream())
                .map(String::trim).filter(value -> !value.isEmpty() && !value.startsWith("/*"))
                .findFirst();
        if (controls.isEmpty()) return true;
        String operation = controls.orElseThrow().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        return Set.of("REPRO", "DELETE", "DEFINE").contains(operation);
    }

    /** A SORT/MERGE step is a supported utility only if its control cards parse in the F7 subset. */
    private static boolean supportedSortControl(JclStep step) {
        String controls = step.ddStatements().stream()
                .filter(dd -> dd.ddName().equalsIgnoreCase("SYSIN") || dd.ddName().equalsIgnoreCase("SORTCNTL"))
                .flatMap(dd -> dd.instreamData().stream())
                .map(String::trim).filter(value -> !value.isEmpty() && !value.startsWith("/*"))
                .reduce("", (left, right) -> left.isEmpty() ? right : left + " " + right);
        if (controls.isEmpty()) return true;
        try {
            new SortUtility().parse(controls);
            return true;
        } catch (UnsupportedOperationException | IllegalArgumentException rejected) {
            return false;
        }
    }

    public record Classification(BatchStep.Kind kind, Optional<String> programRef,
                                 Optional<String> utility, Optional<String> residueReason) { }
}
