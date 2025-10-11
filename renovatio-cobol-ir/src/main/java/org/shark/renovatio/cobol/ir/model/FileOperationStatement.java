package org.shark.renovatio.cobol.ir.model;

import lombok.Value;

import java.util.Objects;

@Value
public final class FileOperationStatement implements CobolStatement {

    public enum OperationType {
        READ,
        WRITE,
        REWRITE,
        DELETE,
        OPEN,
        CLOSE
    }

    OperationType operationType;
    String fileName;

    public FileOperationStatement(OperationType operationType, String fileName) {
        this.operationType = Objects.requireNonNull(operationType, "operationType");
        this.fileName = Objects.requireNonNull(fileName, "fileName");
    }
}
