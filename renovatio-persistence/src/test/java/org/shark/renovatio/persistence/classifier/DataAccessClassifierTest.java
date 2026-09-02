package org.shark.renovatio.persistence.classifier;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DataAccessClassifierTest {

    private static final String PROGRAM_ID = "TESTPGM";
    private static final String SOURCE_PATH = "src/cobol/test.cbl";
    private static final String CONTENT_HASH = "a".repeat(64);
    private static final SourceSpan SPAN = new SourceSpan(SOURCE_PATH, 1, 1, 1, 10);
    private static final SourceProvenance PROVENANCE = new SourceProvenance(SOURCE_PATH, CONTENT_HASH, "COBOL", Optional.empty(), List.of());

    @Test
    void classifyDatabaseAccessProducesExecSql() {
        var ioOp = createIoOperation(SemanticProgram.IoKind.DATABASE, "SELECT", "CUSTOMER-MASTER",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(ioOp), List.of());

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(1, results.size());
        assertEquals(DataAccessKind.EXEC_SQL, results.get(0).kind());
        assertEquals(1.0, results.get(0).confidence());
        assertEquals("CUSTOMER-MASTER", results.get(0).resourceReference().orElse(null));
    }

    @Test
    void classifyVsamKeyAccess() {
        var ioOp = createIoOperation(SemanticProgram.IoKind.FILE, "READ KEY", "VSAM-DATASET",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(ioOp), List.of());

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(1, results.size());
        assertEquals(DataAccessKind.VSAM_KEY, results.get(0).kind());
        assertEquals(1.0, results.get(0).confidence());
        assertFalse(results.get(0).keyShape().isNone());
    }

    @Test
    void classifyVsamSequentialAccess() {
        var ioOp = createIoOperation(SemanticProgram.IoKind.FILE, "READ NEXT", "VSAM-SEQ",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(ioOp), List.of());

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(1, results.size());
        assertEquals(DataAccessKind.VSAM_SEQUENTIAL, results.get(0).kind());
        assertEquals(1.0, results.get(0).confidence());
        assertTrue(results.get(0).keyShape().isNone());
    }

    @Test
    void classifySequentialFdAccess() {
        var ioOp = createIoOperation(SemanticProgram.IoKind.FILE, "READ", "INPUT-FILE",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(ioOp), List.of());

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(1, results.size());
        assertEquals(DataAccessKind.SEQUENTIAL_FD, results.get(0).kind());
        assertEquals(0.8, results.get(0).confidence());
    }

    @Test
    void classifyUnclassifiedAccessProducesResidual() {
        var unclassified = createUnclassified("UNKNOWN-TABLE", "BRWS", "pattern not recognized");
        var program = buildProgram(List.of(), List.of(unclassified));

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(1, results.size());
        assertEquals(DataAccessKind.RESIDUAL, results.get(0).kind());
        assertEquals(0.0, results.get(0).confidence());
    }

    @Test
    void classifyMixedAccesses() {
        var dbOp = createIoOperation(SemanticProgram.IoKind.DATABASE, "INSERT", "ORDERS",
                SemanticProgram.Direction.WRITE);
        var fileOp = createIoOperation(SemanticProgram.IoKind.FILE, "READ NEXT", "LOG-FILE",
                SemanticProgram.Direction.READ);
        var unclassified = createUnclassified("CICS-MAP", "SEND", "CICS not supported");
        var program = buildProgram(List.of(dbOp, fileOp), List.of(unclassified));

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(3, results.size());
        var kinds = results.stream().map(DataAccessClassification::kind).toList();
        assertTrue(kinds.contains(DataAccessKind.EXEC_SQL));
        assertTrue(kinds.contains(DataAccessKind.VSAM_SEQUENTIAL));
        assertTrue(kinds.contains(DataAccessKind.RESIDUAL));
    }

    @Test
    void classificationIdsAreStable() {
        var ioOp = createIoOperation(SemanticProgram.IoKind.DATABASE, "SELECT", "TABLE1",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(ioOp), List.of());

        var classifier = new DataAccessClassifier();
        var first = classifier.classify(program);
        var second = classifier.classify(program);

        assertEquals(first.get(0).id(), second.get(0).id());
    }

    @Test
    void classificationsAreSortedById() {
        var dbOp = createIoOperation(SemanticProgram.IoKind.DATABASE, "SELECT", "T1",
                SemanticProgram.Direction.READ);
        var fileOp = createIoOperation(SemanticProgram.IoKind.FILE, "READ", "F1",
                SemanticProgram.Direction.READ);
        var program = buildProgram(List.of(dbOp, fileOp), List.of());

        var classifier = new DataAccessClassifier();
        var results = classifier.classify(program);

        assertEquals(2, results.size());
        assertTrue(results.get(0).id().compareTo(results.get(1).id()) <= 0);
    }

    @Test
    void nullProgramThrows() {
        var classifier = new DataAccessClassifier();
        assertThrows(NullPointerException.class, () -> classifier.classify(null));
    }

    private SemanticProgram.IoOperation createIoOperation(
            SemanticProgram.IoKind ioKind, String operation, String resourceRef,
            SemanticProgram.Direction direction) {
        var header = SemanticProgram.Header.create(PROGRAM_ID, SemanticProgram.NodeKind.IO_OPERATION,
                ioKind.name().toLowerCase() + "-op", SPAN);
        return new SemanticProgram.IoOperation(header, ioKind, operation,
                Optional.of(resourceRef), direction, List.of());
    }

    private SemanticProgram.UnclassifiedDataAccess createUnclassified(
            String subject, String operation, String reason) {
        var header = SemanticProgram.Header.create(PROGRAM_ID, SemanticProgram.NodeKind.UNCLASSIFIED_DATA_ACCESS,
                "unclassified-" + subject.toLowerCase(), SPAN);
        return new SemanticProgram.UnclassifiedDataAccess(header, subject, operation, reason, List.of());
    }

    private SemanticProgram buildProgram(List<SemanticProgram.IoOperation> ioOps,
                                         List<SemanticProgram.UnclassifiedDataAccess> unclassified) {
        return new SemanticProgram(
                SemanticProgram.SCHEMA_VERSION,
                SemanticProgram.Header.create(PROGRAM_ID, SemanticProgram.NodeKind.PROGRAM, "program", SPAN),
                PROGRAM_ID,
                PROVENANCE,
                List.of(),
                List.of(),
                List.of(),
                ioOps,
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()),
                unclassified);
    }
}
