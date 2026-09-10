package org.shark.renovatio.cobol.ir.annotated;

import java.util.List;

public record AnnotatedCobolModel(String schemaVersion, String baseIrVersion, String baseIrHash,
                                  List<CobolAnnotation> annotations) {
    public static final String SCHEMA_VERSION = "cobol-annotated-ir.v1";

    public AnnotatedCobolModel {
        schemaVersion = AnnotatedContract.text(schemaVersion, "schemaVersion");
        if (!SCHEMA_VERSION.equals(schemaVersion)) throw new IllegalArgumentException("unsupported schemaVersion");
        baseIrVersion = AnnotatedContract.text(baseIrVersion, "baseIrVersion");
        baseIrHash = AnnotatedContract.hash(baseIrHash, "baseIrHash");
        annotations = List.copyOf(annotations == null ? List.of() : annotations);
    }
}
