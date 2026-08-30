package org.shark.renovatio.cobol.ir.annotated;

public record AnnotatedValidationDiagnostic(Code code, String pointer, String message)
        implements Comparable<AnnotatedValidationDiagnostic> {

    @Override
    public int compareTo(AnnotatedValidationDiagnostic other) {
        int pointerOrder = pointer.compareTo(other.pointer);
        return pointerOrder != 0 ? pointerOrder : code.name().compareTo(other.code.name());
    }

    public enum Code {
        ANNOTATED_IR_BASE_HASH_MISMATCH,
        ANNOTATED_IR_NODE_UNRESOLVED,
        ANNOTATED_IR_NODE_KIND_MISMATCH,
        ANNOTATED_IR_DUPLICATE_IDENTITY,
        ANNOTATED_IR_UNSUPPORTED_VERSION,
        ANNOTATED_IR_NONDETERMINISTIC_OUTPUT
    }
}
