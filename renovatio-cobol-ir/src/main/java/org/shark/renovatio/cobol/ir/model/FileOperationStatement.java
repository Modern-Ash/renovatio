package org.shark.renovatio.cobol.ir.model;

import java.util.Objects;

public final class FileOperationStatement implements CobolStatement {

    public enum OperationType {
        READ,
        WRITE,
        REWRITE,
        DELETE,
        OPEN,
        CLOSE
    }

    private final OperationType operationType;
    private final String fileName;

    public FileOperationStatement(OperationType operationType, String fileName) {
        this.operationType = Objects.requireNonNull(operationType, "operationType");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getFileName() {
        return fileName;
    }
}
