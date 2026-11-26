package org.modernash.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * A COBOL file I/O statement. {@code fileName} is always the SELECT name of the file (WRITE and
 * REWRITE name a record in COBOL; the parser resolves it to its file through the FD).
 *
 * @param operationType      the verb
 * @param fileName           SELECT name of the file
 * @param openMode           OPEN mode, {@code null} for other verbs
 * @param recordArea         READ ... INTO area or WRITE/REWRITE ... FROM area, {@code null} when absent
 * @param keyName            READ ... KEY IS / START ... KEY data item, {@code null} when absent
 * @param next               READ NEXT (sequential read of a file opened for dynamic access)
 * @param exceptionStatements AT END / INVALID KEY imperative statements
 * @param normalStatements   NOT AT END / NOT INVALID KEY imperative statements
 * @param recordName         WRITE/REWRITE record name as written in the source, {@code null} otherwise
 */
public record FileOperationStatement(OperationType operationType, String fileName, OpenMode openMode,
                                     String recordArea, String keyName, boolean next,
                                     List<CobolStatement> exceptionStatements,
                                     List<CobolStatement> normalStatements,
                                     String recordName) implements CobolStatement {

    public enum OperationType {
        READ,
        WRITE,
        REWRITE,
        DELETE,
        OPEN,
        CLOSE,
        START
    }

    public enum OpenMode { INPUT, OUTPUT, I_O, EXTEND }

    public FileOperationStatement {
        operationType = Objects.requireNonNull(operationType, "operationType");
        fileName = Objects.requireNonNull(fileName, "fileName");
        exceptionStatements = List.copyOf(exceptionStatements == null ? List.of() : exceptionStatements);
        normalStatements = List.copyOf(normalStatements == null ? List.of() : normalStatements);
    }

    public FileOperationStatement(OperationType operationType, String fileName) {
        this(operationType, fileName, null, null, null, false, List.of(), List.of(), null);
    }

    public FileOperationStatement withFileName(String file) {
        return new FileOperationStatement(operationType, file, openMode, recordArea, keyName, next,
                exceptionStatements, normalStatements, recordName);
    }

    public FileOperationStatement withClauses(List<CobolStatement> exception, List<CobolStatement> normal) {
        return new FileOperationStatement(operationType, fileName, openMode, recordArea, keyName, next,
                exception, normal, recordName);
    }

    /** True for READ/WRITE/REWRITE/DELETE/START, which may carry AT END or INVALID KEY clauses. */
    public boolean hasClauses() {
        return !exceptionStatements.isEmpty() || !normalStatements.isEmpty();
    }
}
