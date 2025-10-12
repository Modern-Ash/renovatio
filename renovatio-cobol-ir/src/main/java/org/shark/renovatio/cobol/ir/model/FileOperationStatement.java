package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public record FileOperationStatement(OperationType operationType, String fileName) implements CobolStatement {

    public enum OperationType {
        READ,
        WRITE,
        REWRITE,
        DELETE,
        OPEN,
        CLOSE
    }

    public FileOperationStatement(OperationType operationType, String fileName) {
        this.operationType = Objects.requireNonNull(operationType, "operationType");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }
}
