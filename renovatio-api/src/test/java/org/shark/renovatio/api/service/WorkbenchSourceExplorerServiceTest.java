package org.shark.renovatio.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.SourceFile;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.Symbol;

class WorkbenchSourceExplorerServiceTest {

    private final WorkbenchSourceExplorerService service = new WorkbenchSourceExplorerService(null);

    private static final String PROGRAM = String.join("\n",
            "       IDENTIFICATION DIVISION.",
            "       PROGRAM-ID. PAYROLL.",
            "       ENVIRONMENT DIVISION.",
            "       DATA DIVISION.",
            "       WORKING-STORAGE SECTION.",
            "       01 WS-COUNT PIC 9(4).",
            "           COPY COMMONREC.",
            "           COPY MISSINGCPY.",
            "       PROCEDURE DIVISION.",
            "       MAIN-SECTION SECTION.",
            "       0100-MAIN.",
            "           PERFORM 0200-LOAD",
            "           CALL 'SUBRTN'",
            "           EXEC SQL SELECT 1 FROM SYSIBM.SYSDUMMY1 END-EXEC",
            "           EXEC CICS RETURN END-EXEC.",
            "       0200-LOAD.",
            "           MOVE 0 TO WS-COUNT.");

    private static final String JCL = String.join("\n",
            "//PAYJOB   JOB (ACCT),'PAYROLL'",
            "//STEP01   EXEC PGM=PAYROLL",
            "//INFILE   DD DSN=NIGHTLY.BATCH.PAYIN,DISP=SHR",
            "//STEP02   EXEC PGM=SORT",
            "//OUTFILE  DD DSN=NIGHTLY.BATCH.PAYOUT,DISP=(NEW,CATLG)");

    private WorkbenchSourceExplorerDto explore(Path root) throws Exception {
        Files.writeString(root.resolve("PAYROLL.CBL"), PROGRAM);
        Files.writeString(root.resolve("COMMONREC.CPY"), "       01 COMMON-REC.\n          05 CR-ID PIC X(8).");
        Files.writeString(root.resolve("NIGHTLY-BATCH.JCL"), JCL);
        Files.writeString(root.resolve("NOTES.TXT"), "not a legacy asset");
        return service.explore(root);
    }

    @Test
    void buildsNavigableProgramTreeWithPositionedSymbols(@TempDir Path root) throws Exception {
        WorkbenchSourceExplorerDto dto = explore(root);
        SourceFile program = dto.files().stream()
                .filter(file -> file.name().equals("PAYROLL.CBL")).findFirst().orElseThrow();

        assertEquals("cobol-program", program.kind());
        assertEquals("parsed", program.analysisStatus());
        assertTrue(program.hash().startsWith("sha256:"));
        assertEquals("US-ASCII", program.encoding());

        assertTrue(hasSymbol(program, "division", "IDENTIFICATION DIVISION"));
        assertTrue(hasSymbol(program, "division", "PROCEDURE DIVISION"));
        assertTrue(hasSymbol(program, "section", "MAIN-SECTION"));

        Symbol paragraph = symbol(program, "paragraph", "0100-MAIN");
        assertEquals("PAYROLL#section:MAIN-SECTION", paragraph.parentId());
        assertEquals(11, paragraph.line());
        assertEquals("ir://PAYROLL/paragraph/0100-MAIN", paragraph.irCoordinate());

        assertEquals("0200-LOAD", symbol(program, "perform", "0200-LOAD").name());
        assertEquals("SUBRTN", symbol(program, "call", "SUBRTN").name());
        assertTrue(hasSymbol(program, "exec-sql", "EXEC SQL"));
        assertTrue(hasSymbol(program, "exec-cics", "EXEC CICS"));
        assertTrue(hasSymbol(program, "copy", "COMMONREC"));
    }

    @Test
    void reportsMissingCopybookAsDiagnosticButKeepsFileNavigable(@TempDir Path root) throws Exception {
        SourceFile program = explore(root).files().stream()
                .filter(file -> file.name().equals("PAYROLL.CBL")).findFirst().orElseThrow();
        assertTrue(program.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.severity().equals("warning")
                        && diagnostic.message().contains("MISSINGCPY")));
        assertFalse(program.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.message().contains("COMMONREC")));
        assertFalse(program.symbols().isEmpty());
    }

    @Test
    void aggregatesJclStepsDdStatementsAndDatasets(@TempDir Path root) throws Exception {
        WorkbenchSourceExplorerDto dto = explore(root);
        SourceFile jcl = dto.files().stream()
                .filter(file -> file.name().equals("NIGHTLY-BATCH.JCL")).findFirst().orElseThrow();
        assertEquals("jcl", jcl.kind());
        assertTrue(hasSymbol(jcl, "jcl-step", "STEP01"));
        assertTrue(hasSymbol(jcl, "jcl-step", "STEP02"));
        assertTrue(hasSymbol(jcl, "jcl-dd", "INFILE"));

        List<String> datasets = dto.datasets().stream().map(WorkbenchSourceExplorerDto.Dataset::id).toList();
        assertTrue(datasets.contains("NIGHTLY.BATCH.PAYIN"));
        assertTrue(datasets.contains("NIGHTLY.BATCH.PAYOUT"));
        assertEquals(List.of("NIGHTLY-BATCH.JCL"), dto.datasets().stream()
                .filter(dataset -> dataset.id().equals("NIGHTLY.BATCH.PAYIN")).findFirst().orElseThrow().referencedBy());
    }

    @Test
    void unsupportedFilesAreExcludedSoWorkspaceStaysNavigable(@TempDir Path root) throws Exception {
        WorkbenchSourceExplorerDto dto = explore(root);
        assertTrue(dto.files().stream().noneMatch(file -> file.name().equals("NOTES.TXT")));
        assertEquals(3, dto.files().size());
        dto.files().forEach(file -> assertNotNull(file.symbols()));
    }

    @Test
    void copybookFileParsesWithoutProcedureDivision(@TempDir Path root) throws Exception {
        SourceFile copybook = explore(root).files().stream()
                .filter(file -> file.name().equals("COMMONREC.CPY")).findFirst().orElseThrow();
        assertEquals("copybook", copybook.kind());
        assertTrue(List.of("parsed", "partial", "unsupported").contains(copybook.analysisStatus()));
    }

    private static boolean hasSymbol(SourceFile file, String kind, String name) {
        return file.symbols().stream().anyMatch(symbol -> symbol.kind().equals(kind) && symbol.name().equals(name));
    }

    private static Symbol symbol(SourceFile file, String kind, String name) {
        return file.symbols().stream()
                .filter(symbol -> symbol.kind().equals(kind) && symbol.name().equals(name))
                .findFirst().orElseThrow(() -> new AssertionError("missing symbol " + kind + ":" + name));
    }
}
