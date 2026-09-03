package org.shark.renovatio.semantic.ir;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Dataset/resource binding used by one or more batch steps. */
public record BatchDataset(String id, String ddName, AccessKind access,
                           Optional<String> resourceReference,
                           List<String> inlineRecords,
                           List<ResourcePart> concatenations) {
    public BatchDataset(String id, String ddName, AccessKind access,
                        Optional<String> resourceReference) {
        this(id, ddName, access, resourceReference, List.of(), List.of());
    }

    public BatchDataset {
        id = SemanticIdentity.hash(id, "id");
        ddName = SemanticIdentity.text(ddName, "ddName").toUpperCase(Locale.ROOT);
        if (access == null) throw new NullPointerException("access");
        resourceReference = resourceReference == null ? Optional.empty()
                : resourceReference.map(value -> SemanticIdentity.text(value, "resourceReference"));
        inlineRecords = inlineRecords == null ? List.of() : List.copyOf(inlineRecords);
        concatenations = concatenations == null ? List.of() : List.copyOf(concatenations);
    }

    public record ResourcePart(Optional<String> resourceReference, List<String> inlineRecords) {
        public ResourcePart {
            resourceReference = resourceReference == null ? Optional.empty()
                    : resourceReference.map(value -> SemanticIdentity.text(value, "resourceReference"));
            inlineRecords = inlineRecords == null ? List.of() : List.copyOf(inlineRecords);
            if (resourceReference.isEmpty() && inlineRecords.isEmpty())
                throw new IllegalArgumentException("resource part requires a reference or in-line records");
        }
    }

    public enum AccessKind { SEQ_IN, SEQ_OUT, VSAM, TEMP, STDIO }
}
