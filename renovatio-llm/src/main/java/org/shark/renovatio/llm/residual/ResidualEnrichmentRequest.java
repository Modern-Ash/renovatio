package org.shark.renovatio.llm.residual;

import java.util.Objects;

/** Minimal routing facts derived from validated IR; no natural-language classification is allowed. */
public record ResidualEnrichmentRequest(
        String baseIrVersion,
        String nodeId,
        String nodeKind,
        ResidualConstruction construction,
        boolean explicitDomainNamingRequest,
        boolean irreducibleControlFlow,
        boolean containsGoTo,
        boolean residualBusinessIntent,
        String unsupportedDiagnostic) {

    public static final String SCHEMA_VERSION = "residual-enrichment-request.v1";

    public ResidualEnrichmentRequest {
        requireText(baseIrVersion, "baseIrVersion");
        requireText(nodeId, "nodeId");
        requireText(nodeKind, "nodeKind");
        Objects.requireNonNull(construction, "construction");
        unsupportedDiagnostic = normalize(unsupportedDiagnostic);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
