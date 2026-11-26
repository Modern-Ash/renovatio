package org.modernash.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.cobol.ir.model.CobolFileDescriptor;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolStatement;
import org.modernash.renovatio.cobol.ir.model.FileOperationStatement;
import org.modernash.renovatio.cobol.ir.model.FileOperationStatement.OpenMode;
import org.modernash.renovatio.cobol.ir.model.FileOperationStatement.OperationType;
import org.modernash.renovatio.cobol.ir.model.MoveStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileStatementParsingTest {

    private static final String HEADER = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. FILES01.
            ENVIRONMENT DIVISION.
            INPUT-OUTPUT SECTION.
            FILE-CONTROL.
                SELECT ACCT-FILE ASSIGN TO ACCTDAT
                       ORGANIZATION IS INDEXED
                       ACCESS MODE  IS RANDOM
                       RECORD KEY   IS ACCT-KEY
                       FILE STATUS  IS ACCT-STATUS.
                SELECT OUT-FILE ASSIGN TO OUTFILE
                       ORGANIZATION IS SEQUENTIAL
                       FILE STATUS  IS OUT-STATUS.
            DATA DIVISION.
            FILE SECTION.
            FD  ACCT-FILE.
            01  ACCT-REC.
                05  ACCT-KEY       PIC 9(5).
                05  ACCT-NAME      PIC X(10).
            FD  OUT-FILE.
            01  OUT-REC            PIC X(15).
            WORKING-STORAGE SECTION.
            01  ACCT-STATUS        PIC XX.
            01  OUT-STATUS         PIC XX.
            01  WS-EOF             PIC X VALUE 'N'.
            01  WS-REC             PIC X(15).
            PROCEDURE DIVISION.
            MAIN-PARA.
            """;

    private static List<CobolStatement> parse(String... body) {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(HEADER + String.join("\n", body) + "\n");
        return model.getEntryParagraph().statements();
    }

    @Test
    void describesFilesFromFileControl() {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(HEADER + "    GOBACK.\n");

        CobolFileDescriptor acct = model.findFile("ACCT-FILE").orElseThrow();
        assertEquals("ACCTDAT", acct.assignTarget());
        assertEquals(CobolFileDescriptor.Organization.INDEXED, acct.organization());
        assertEquals(CobolFileDescriptor.AccessMode.RANDOM, acct.accessMode());
        assertEquals("ACCT-KEY", acct.recordKey());
        assertEquals("ACCT-STATUS", acct.fileStatus());
        assertEquals("ACCT-REC", acct.recordName());
        CobolFileDescriptor out = model.findFile("OUT-FILE").orElseThrow();
        assertEquals(CobolFileDescriptor.Organization.SEQUENTIAL, out.organization());
        assertNull(out.recordKey());
    }

    @Test
    void splitsOpenIntoOneStatementPerFileWithItsMode() {
        List<CobolStatement> statements = parse(
                "    OPEN INPUT ACCT-FILE",
                "         OUTPUT OUT-FILE",
                "    GOBACK.");

        FileOperationStatement first = (FileOperationStatement) statements.get(0);
        FileOperationStatement second = (FileOperationStatement) statements.get(1);
        assertEquals(OperationType.OPEN, first.operationType());
        assertEquals("ACCT-FILE", first.fileName());
        assertEquals(OpenMode.INPUT, first.openMode());
        assertEquals("OUT-FILE", second.fileName());
        assertEquals(OpenMode.OUTPUT, second.openMode());
        assertEquals(3, statements.size());
    }

    @Test
    void parsesSequentialReadIntoWithAtEndClauses() {
        List<CobolStatement> statements = parse(
                "    READ OUT-FILE INTO WS-REC",
                "        AT END",
                "            MOVE 'Y' TO WS-EOF",
                "        NOT AT END",
                "            MOVE 'N' TO WS-EOF",
                "    END-READ",
                "    GOBACK.");

        FileOperationStatement read = (FileOperationStatement) statements.get(0);
        assertEquals(OperationType.READ, read.operationType());
        assertEquals("WS-REC", read.recordArea());
        assertEquals("'Y'", ((MoveStatement) read.exceptionStatements().get(0)).source());
        assertEquals("'N'", ((MoveStatement) read.normalStatements().get(0)).source());
        assertEquals(2, statements.size(), () -> String.valueOf(statements));
    }

    @Test
    void parsesKeyedReadWithInvalidKeyOnTheSameLine() {
        List<CobolStatement> statements = parse(
                "    READ ACCT-FILE KEY IS ACCT-KEY INVALID KEY MOVE 'Y' TO WS-EOF.",
                "    GOBACK.");

        FileOperationStatement read = (FileOperationStatement) statements.get(0);
        assertEquals("ACCT-FILE", read.fileName());
        assertEquals("ACCT-KEY", read.keyName());
        assertEquals(1, read.exceptionStatements().size());
        assertEquals(2, statements.size());
    }

    @Test
    void resolvesWriteRecordToItsFileAndKeepsFrom() {
        List<CobolStatement> statements = parse(
                "    WRITE OUT-REC FROM WS-REC",
                "    MOVE 'Y' TO WS-EOF",
                "    GOBACK.");

        FileOperationStatement write = (FileOperationStatement) statements.get(0);
        assertEquals("OUT-FILE", write.fileName());
        assertEquals("OUT-REC", write.recordName());
        assertEquals("WS-REC", write.recordArea());
        assertTrue(statements.get(1) instanceof MoveStatement);
    }

    @Test
    void doesNotMistakeReadyOrOpenedNamesForFileVerbs() {
        List<CobolStatement> statements = parse(
                "    MOVE 'N' TO WS-EOF",
                "    GOBACK.");

        assertTrue(statements.stream().noneMatch(FileOperationStatement.class::isInstance));
    }

    @Test
    void keepsEmbeddedExecCicsReadOutOfCobolFileStatements() {
        List<CobolStatement> statements = parse(
                "    EXEC CICS READ FILE('CUSTOMERS')",
                "         INTO (WS-REC)",
                "         RESP (WS-EOF)",
                "    END-EXEC",
                "    GOBACK.");

        assertTrue(statements.stream().noneMatch(FileOperationStatement.class::isInstance));
        assertEquals(2, statements.size(), () -> String.valueOf(statements));
    }
}
