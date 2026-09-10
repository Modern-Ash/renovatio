package org.shark.renovatio.provider.cobol.pipeline;

import java.nio.file.Path;
import java.util.List;

/**
 * Report from equivalence checking between generated and expected output.
 */
public record EquivalenceReport(
    String fixtureId,
    Path actualPath,
    Path expectedPath,
    boolean byteIdentical,
    List<LineDivergence> divergences,
    List<String> excludedMetadata,
    GateDecision decision
) {
    /**
     * Line-level divergence between actual and expected files.
     */
    public record LineDivergence(
        int lineNumber,
        String actualLine,
        String expectedLine,
        DivergenceType type
    ) {
        public enum DivergenceType {
            CONTENT,
            WHITESPACE,
            ORDERING,
            METADATA
        }
    }

    /**
     * Gate decision for equivalence check.
     */
    public enum GateDecision {
        /**
         * All checks passed, no blocking divergences.
         */
        PASS,
        
        /**
         * Blocking divergences detected.
         */
        FAIL,
        
        /**
         * Equivalence waived with explicit approval.
         */
        WAIVED
    }

    /**
     * Check if the equivalence check passed.
     */
    public boolean isPassed() {
        return decision == GateDecision.PASS;
    }

    /**
     * Check if there are blocking divergences.
     */
    public boolean hasBlockingDivergences() {
        return divergences.stream()
            .anyMatch(d -> d.type() == LineDivergence.DivergenceType.CONTENT);
    }
}
