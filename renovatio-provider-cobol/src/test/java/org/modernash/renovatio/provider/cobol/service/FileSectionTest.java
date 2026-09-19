package org.modernash.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.DecomposedBusinessLogic;
import org.modernash.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.modernash.renovatio.shared.domain.Workspace;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileSectionTest {

    @TempDir
    Path tempDir;

    @Test
    void testFileSectionEntityNaming() throws Exception {
        // COBOL with FILE SECTION - this should use the 01 level record name, not FD name
        String cobol = """
                       IDENTIFICATION DIVISION.
                       PROGRAM-ID. TESTFILE.
                       
                       DATA DIVISION.
                       FILE SECTION.
                       FD  CUSTOMER-FILE.
                       01  CUSTOMER-RECORD.
                           05  CUSTOMER-ID    PIC 9(10).
                           05  CUSTOMER-NAME  PIC X(30).
                           05  CUSTOMER-AMOUNT PIC 9(7)V99.
                       
                       WORKING-STORAGE SECTION.
                       01  WS-PREV-CUSTOMER    PIC 9(10).
                       01  WS-TOTAL-AMOUNT     PIC 9(9)V99.
                       01  WS-COUNT-RECORDS    PIC 9(6).
                       
                       PROCEDURE DIVISION.
                       MAIN-PROCESS.
                           OPEN INPUT CUSTOMER-FILE.
                           PERFORM READ-CUSTOMER.
                           MOVE CUSTOMER-ID TO WS-PREV-CUSTOMER.
                           
                           PERFORM PROCESS-RECORD UNTIL END-OF-FILE.
                           
                           PERFORM CUSTOMER-BREAK.
                           PERFORM FINAL-TOTALS.
                           CLOSE CUSTOMER-FILE.
                           STOP RUN.
                           
                       PROCESS-RECORD.
                           IF CUSTOMER-ID NOT = WS-PREV-CUSTOMER
                               PERFORM CUSTOMER-BREAK
                           END-IF.
                           ADD CUSTOMER-AMOUNT TO WS-TOTAL-AMOUNT.
                           ADD 1 TO WS-COUNT-RECORDS.
                           PERFORM READ-CUSTOMER.
                           
                       READ-CUSTOMER.
                           READ CUSTOMER-FILE INTO CUSTOMER-RECORD.
                           
                       CUSTOMER-BREAK.
                           MOVE CUSTOMER-ID TO WS-PREV-CUSTOMER.
                           MOVE ZEROS TO WS-TOTAL-AMOUNT.
                           
                       FINAL-TOTALS.
                           DISPLAY "GRAND TOTAL: " WS-TOTAL-AMOUNT.
                           DISPLAY "RECORDS PROCESSED: " WS-COUNT-RECORDS.
               """;

        Path cobolFile = tempDir.resolve("TESTFILE.cob");
        Files.writeString(cobolFile, cobol);

        CobolParsingService parsingService = new CobolParsingService();
        CobolIntermediateModelService irService = new CobolIntermediateModelService();
        
        // Debug: parse directly with IR service
        CobolIntermediateModel model = irService.parse(cobolFile);
        System.out.println("DEBUG: File to Record Mapping from IR: " + model.getFileToRecordMapping());
        System.out.println("DEBUG: Data Items:");
        for (var item : model.getDataItems()) {
            System.out.println("  " + item.getName() + " (level=" + item.getLevel() + ")");
        }
        
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
        
        // BUG: Currently uses FD name "CUSTOMER-FILE" -> entity "Customer"
        // FIX: Should use 01 level record name "CUSTOMER-RECORD" -> entity "CustomerRecord"
        var dataAccess = logic.dataAccessComponents().get(0);
        assertEquals("CustomerRecord", dataAccess.entityName(), 
            "Entity name should be derived from 01 level record in FILE SECTION, not FD name");
    }
}
