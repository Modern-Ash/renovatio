package org.shark.renovatio.semantic.ir;

import java.util.Locale;
import java.util.Optional;

/** Dataset/resource binding used by one or more batch steps. */
public record BatchDataset(String id, String ddName, AccessKind access,
                           Optional<String> resourceReference) {
    public BatchDataset {
        id = SemanticIdentity.hash(id, "id");
        ddName = SemanticIdentity.text(ddName, "ddName").toUpperCase(Locale.ROOT);
        if (access == null) throw new NullPointerException("access");
        resourceReference = resourceReference == null ? Optional.empty()
                : resourceReference.map(value -> SemanticIdentity.text(value, "resourceReference"));
    }

    public enum AccessKind { SEQ_IN, SEQ_OUT, VSAM, TEMP, STDIO }
}
