package org.modernash.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.modernash.renovatio.cobol.ir.model.DecomposedBusinessLogic;
import org.modernash.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.modernash.renovatio.shared.domain.Workspace;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NoFileSectionTest {

    @TempDir
    Path tempDir;

    @Test
    void testNoFileSectionStillWorks() throws Exception {
        // COBOL WITHOUT FILE SECTION - should still work (backward compatibility)
        String cobol = """
                       IDENTIFICATION DIVISION.
                       PROGRAM-ID. SIMPLEBRK.
                       
                       DATA DIVISION.
                       WORKING-STORAGE SECTION.
                       01  WS-KEY              PIC X(10).
                       01  WS-PREV-KEY         PIC X(10).
                       01  WS-VALUE            PIC 9(5)V99.
                       01  WS-TOTAL-SUM        PIC 9(9)V99.
                       
                       PROCEDURE DIVISION.
                       MAIN-PARA.
                           OPEN INPUT DATA-FILE.
                           READ DATA-FILE.
                           PERFORM PROCESS-PARA UNTIL END-OF-FILE.
                           CLOSE DATA-FILE.
                           STOP RUN.
                           
                       PROCESS-PARA.
                           IF WS-KEY NOT = WS-PREV-KEY
                               PERFORM BREAK-PARA
                           END-IF.
                           ADD WS-VALUE TO WS-TOTAL-SUM.
                           READ DATA-FILE.
                           
                       BREAK-PARA.
                           MOVE WS-KEY TO WS-PREV-KEY.
                           MOVE ZEROS TO WS-TOTAL-SUM.
               """;

        Path cobolFile = tempDir.resolve("SIMPLEBRK.cob");
        Files.writeString(cobolFile, cobol);

        CobolParsingService parsingService = new CobolParsingService();
        CobolIntermediateModelService irService = new CobolIntermediateModelService();
        ControlBreakDecompositionService service = new ControlBreakDecompositionService(irService, parsingService);

        Workspace workspace = new Workspace();
        workspace.setId("test-workspace");
        workspace.setPath(tempDir.toString());

        var result = service.analyzeAndDecompose(workspace);

        assertTrue(result.hasResults(), "Should detect control break patterns");
        
        var decomp = result.getDecompositions().get(0);
        var logic = decomp.decomposedLogic();
        
        System.out.println("Program: " + logic.programId());
        for (var da : logic.dataAccessComponents()) {
            System.out.println("Data Access: " + da.componentId());
            System.out.println("  Entity Name: " + da.entityName());
            System.out.println("  Record Name: " + da.recordName());
            System.out.println("  Access Pattern: " + da.accessPattern());
        }
        
        // Without FILE SECTION, should fall back to FD name
        // DATA-FILE -> Data (stripping FILE)
        var dataAccess = logic.dataAccessComponents().get(0);
        assertEquals("Data", dataAccess.entityName(), 
            "Without FILE SECTION, should fall back to FD name stripped of FILE");
        assertEquals("DATA-FILE", dataAccess.recordName(),
            "Record name should be the FD name when no FILE SECTION");
    }
}
