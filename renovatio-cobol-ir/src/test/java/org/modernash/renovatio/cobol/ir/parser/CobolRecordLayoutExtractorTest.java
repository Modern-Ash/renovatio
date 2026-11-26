package org.modernash.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolRecordLayout;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CobolRecordLayoutExtractorTest {

    private static CobolIntermediateModel parse(String dataItems) {
        return new SimpleCobolIrParser().parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. LAYOUT01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                """ + dataItems + """
                PROCEDURE DIVISION.
                MAIN-PARA.
                    GOBACK.
                """);
    }

    @Test
    void composesDisplayRecordInStorageOrderIncludingNestedGroupsAndFiller() {
        CobolIntermediateModel model = parse("""
                01  ACCOUNT-RECORD.
                    05  ACCT-ID                 PIC 9(11).
                    05  ACCT-ACTIVE-STATUS      PIC X(01).
                    05  ACCT-CURR-BAL           PIC S9(10)V99.
                    05  ACCT-DATES.
                        10  ACCT-OPEN-DATE      PIC X(10).
                    05  FILLER                  PIC X(03) VALUE 'AB'.
                01  ACCTFILE-STATUS.
                    05  ACCTFILE-STAT1      PIC X.
                    05  ACCTFILE-STAT2      PIC X.
                """);

        CobolRecordLayout account = model.findComposableLayout("ACCOUNT-RECORD").orElseThrow();
        assertEquals(Arrays.asList("ACCT-ID", "ACCT-ACTIVE-STATUS", "ACCT-CURR-BAL", "ACCT-OPEN-DATE", null),
                account.fields().stream().map(CobolRecordLayout.Field::name).toList());
        assertEquals(11 + 1 + 12 + 10 + 3, account.length());
        assertEquals("AB ", account.fields().get(4).fillerValue());
        assertEquals(10, model.findComposableLayout("ACCT-DATES").orElseThrow().length());
        assertEquals(2, model.findComposableLayout("ACCTFILE-STATUS").orElseThrow().length());
    }

    @Test
    void reportsGroupsThatCannotBeEncodedExactlyAsOpaque() {
        CobolIntermediateModel model = parse("""
                01  WS-PACKED.
                    05  WS-AMOUNT    PIC S9(7)V99 COMP-3.
                01  WS-TABLE.
                    05  WS-ROW       PIC X(4) OCCURS 3 TIMES.
                01  WS-EDITED.
                    05  WS-PRINT     PIC ZZ9.99.
                01  WS-FIRST.
                    05  WS-DUP       PIC X.
                01  WS-SECOND.
                    05  WS-DUP       PIC X.
                """);

        assertTrue(model.findComposableLayout("WS-PACKED").isEmpty());
        assertTrue(model.getRecordLayouts().get("WS-PACKED").nonComposableReason().orElseThrow()
                .contains("USAGE DISPLAY"));
        assertTrue(model.findComposableLayout("WS-TABLE").isEmpty());
        assertTrue(model.findComposableLayout("WS-EDITED").isEmpty());
        assertTrue(model.findComposableLayout("WS-FIRST").isEmpty());
    }
}
