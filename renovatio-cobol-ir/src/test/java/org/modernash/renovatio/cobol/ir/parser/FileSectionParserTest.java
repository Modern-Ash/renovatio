package org.modernash.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;

import static org.junit.jupiter.api.Assertions.*;

class FileSectionParserTest {

    @Test
    void testFileSectionMapping() {
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

        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);
        
        System.out.println("Program ID: " + model.getProgramId());
        System.out.println("File to Record Mapping: " + model.getFileToRecordMapping());
        System.out.println("Data Items:");
        for (var item : model.getDataItems()) {
            System.out.println("  " + item.getName() + " (level=" + item.getLevel() + ")");
        }
        
        // Verify the mapping
        assertTrue(model.getFileToRecordMapping().containsKey("CUSTOMER-FILE"));
        assertEquals("CUSTOMER-RECORD", model.getFileToRecordMapping().get("CUSTOMER-FILE"));
    }
}
