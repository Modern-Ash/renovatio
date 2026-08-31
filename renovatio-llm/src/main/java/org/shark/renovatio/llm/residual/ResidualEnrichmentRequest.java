package org.shark.renovatio.llm.residual;

import java.util.Objects;
import java.util.List;
import java.util.regex.Pattern;

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
        String unsupportedDiagnostic,
        List<String> collisionScope,
        boolean publicSignatureProtected,
        String agoraToolRunRef) {

    public static final String SCHEMA_VERSION = "residual-enrichment-request.v1";
    private static final Pattern TOOL_RUN = Pattern.compile("tool-[0-9]{8}t[0-9]{14}z");

    public ResidualEnrichmentRequest {
        requireText(baseIrVersion, "baseIrVersion");
        requireText(nodeId, "nodeId");
        requireText(nodeKind, "nodeKind");
        Objects.requireNonNull(construction, "construction");
        unsupportedDiagnostic = normalize(unsupportedDiagnostic);
        collisionScope = List.copyOf(collisionScope == null ? List.of() : collisionScope);
        agoraToolRunRef = normalize(agoraToolRunRef);
        if (explicitDomainNamingRequest
                && (agoraToolRunRef == null || !TOOL_RUN.matcher(agoraToolRunRef).matches())) {
            throw new IllegalArgumentException("explicit domain naming requires an Agora tool-run identity");
        }
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
