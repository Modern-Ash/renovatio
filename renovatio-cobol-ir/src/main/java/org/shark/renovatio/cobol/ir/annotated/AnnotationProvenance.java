package org.shark.renovatio.cobol.ir.annotated;

import java.util.Objects;

public record AnnotationProvenance(String provider, String model, String promptId, String promptVersion,
                                   String outputSchemaVersion, String inputHash, String outputHash,
                                   String toolRunRef, CacheDisposition cacheDisposition) {
    public AnnotationProvenance {
        provider = AnnotatedContract.text(provider, "provider");
        model = AnnotatedContract.text(model, "model");
        promptId = AnnotatedContract.text(promptId, "promptId");
        promptVersion = AnnotatedContract.text(promptVersion, "promptVersion");
        outputSchemaVersion = AnnotatedContract.text(outputSchemaVersion, "outputSchemaVersion");
        inputHash = AnnotatedContract.hash(inputHash, "inputHash");
        outputHash = AnnotatedContract.hash(outputHash, "outputHash");
        AnnotatedContract.text(toolRunRef, "toolRunRef");
        if (!AnnotatedContract.TOOL_RUN.matcher(toolRunRef).matches()) {
            throw new IllegalArgumentException("toolRunRef has invalid format");
        }
        Objects.requireNonNull(cacheDisposition, "cacheDisposition");
    }

    public enum CacheDisposition { HIT, MISS }
}
