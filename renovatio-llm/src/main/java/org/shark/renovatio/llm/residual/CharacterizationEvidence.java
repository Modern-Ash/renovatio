package org.shark.renovatio.llm.residual;

/** Commit-bound prerequisite evidence for retaining an advisory control-flow plan. */
public record CharacterizationEvidence(
        String baselineRef,
        boolean schemaGreen,
        boolean compilationGreen,
        boolean characterizationGreen) {

    public CharacterizationEvidence {
        if (baselineRef != null && baselineRef.isBlank()) baselineRef = null;
    }

    public boolean isGreen() {
        return baselineRef != null && schemaGreen && compilationGreen && characterizationGreen;
    }

    public static CharacterizationEvidence missing() {
        return new CharacterizationEvidence(null, false, false, false);
    }
}
