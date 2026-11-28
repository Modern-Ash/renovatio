package org.modernash.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolRecordLayout;
import org.modernash.renovatio.cobol.ir.model.CobolRedefinesView;

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
                    05  WS-COUNTER   PIC S9(4) COMP.
                01  WS-FLOAT.
                    05  WS-RATE      COMP-2.
                01  WS-TABLE.
                    05  WS-ROW       PIC X(4) OCCURS 3 TIMES DEPENDING ON WS-DUP.
                01  WS-EDITED.
                    05  WS-PRINT     PIC ZZ9.99.
                01  WS-FIRST.
                    05  WS-DUP       PIC X.
                01  WS-SECOND.
                    05  WS-DUP       PIC X.
                """);

        // Binary and packed fields have exact encodings: 5 bytes COMP-3 + 2 bytes COMP.
        assertEquals(7, model.findComposableLayout("WS-PACKED").orElseThrow().length());
        assertTrue(model.findComposableLayout("WS-FLOAT").isEmpty());
        assertTrue(model.getRecordLayouts().get("WS-FLOAT").nonComposableReason().orElseThrow()
                .contains("COMP-2"));
        assertTrue(model.findComposableLayout("WS-TABLE").isEmpty());
        assertTrue(model.findComposableLayout("WS-EDITED").isEmpty());
        assertTrue(model.findComposableLayout("WS-FIRST").isEmpty());
    }

    @Test
    void capturesInitialValuesOfUniquelyNamedElementaryItems() {
        CobolIntermediateModel model = parse("""
                01  END-OF-FILE             PIC X(01)    VALUE 'N'.
                01  APPL-RESULT             PIC S9(9)   COMP VALUE 8.
                01  WS-RATE                 PIC S9(3)V99 VALUE -1.25.
                01  WS-NAME                 PIC X(10) VALUE SPACES.
                01  WS-NONE                 PIC X(10).
                01  WS-TABLE.
                    05  WS-ROW              PIC X(4) VALUE 'AB' OCCURS 3 TIMES.
                """);

        assertEquals("'N'", model.getInitialValues().get("END-OF-FILE"));
        assertEquals("8", model.getInitialValues().get("APPL-RESULT"));
        assertEquals("-1.25", model.getInitialValues().get("WS-RATE"));
        assertEquals("SPACES", model.getInitialValues().get("WS-NAME"));
        assertFalse(model.getInitialValues().containsKey("WS-NONE"));
        assertFalse(model.getInitialValues().containsKey("WS-ROW"));
    }

    @Test
    void repeatsFixedOccursFieldsPerOccurrenceAndReportsTableDimensions() {
        CobolIntermediateModel model = parse("""
                01  ARR-ARRAY-REC.
                    05  ARR-ACCT-ID                PIC 9(11).
                    05  ARR-ACCT-BAL OCCURS 2 TIMES.
                        10  ARR-ACCT-CURR-BAL       PIC S9(10)V99.
                        10  ARR-ACCT-CURR-CYC-DEBIT PIC S9(10)V99 COMP-3.
                    05  ARR-CODES                  PIC X(2) OCCURS 3 TIMES.
                01  WS-GRID.
                    05  WS-ROW OCCURS 2 TIMES.
                        10  WS-CELL OCCURS 3 TIMES PIC X.
                """);

        CobolRecordLayout record = model.findComposableLayout("ARR-ARRAY-REC").orElseThrow();
        assertEquals(java.util.Arrays.asList("ARR-ACCT-ID", "ARR-ACCT-CURR-BAL", "ARR-ACCT-CURR-CYC-DEBIT",
                        "ARR-ACCT-CURR-BAL", "ARR-ACCT-CURR-CYC-DEBIT", "ARR-CODES", "ARR-CODES", "ARR-CODES"),
                record.fields().stream().map(CobolRecordLayout.Field::name).toList());
        assertEquals(java.util.List.of(0, 1, 1, 2, 2, 1, 2, 3),
                record.fields().stream().map(CobolRecordLayout.Field::occurrence).toList());
        assertEquals(11 + 2 * (12 + 7) + 3 * 2, record.length());
        assertEquals(2, model.getTableDimensions().get("ARR-ACCT-CURR-BAL"));
        assertEquals(3, model.getTableDimensions().get("ARR-CODES"));
        assertEquals(0, model.getTableDimensions().get("WS-CELL"));
        assertFalse(model.getTableDimensions().containsKey("ARR-ACCT-ID"));
        assertTrue(model.findComposableLayout("WS-GRID").isEmpty());
        assertTrue(model.findComposableLayout("WS-ROW").isEmpty(), "a table element group needs a subscript");
    }

    @Test
    void modelsRedefinesAsViewsOverTheBaseStorage() {
        CobolIntermediateModel model = parse("""
                01  WS-MESSAGE.
                    02 WS-SEVERITY   PIC X(04).
                    02 WS-SEVERITY-N REDEFINES WS-SEVERITY PIC 9(4).
                    02 FILLER        PIC X(11) VALUE 'Mesg Code:'.
                01  WS-DATE-AREA.
                    05 WS-DATE       PIC X(8).
                01  WS-DATE-PARTS REDEFINES WS-DATE-AREA.
                    05 WS-YYYY       PIC 9(4).
                    05 WS-MMDD       PIC X(4).
                01  WS-SHORT         PIC X(2).
                01  WS-LONG REDEFINES WS-SHORT PIC X(5).
                """);

        CobolRedefinesView numeric = model.getRedefinesViews().get("WS-SEVERITY-N");
        assertEquals("WS-SEVERITY", numeric.base());
        assertFalse(numeric.baseIsGroup());
        assertEquals(4, numeric.length());
        assertEquals(15, model.findComposableLayout("WS-MESSAGE").orElseThrow().length(),
                "the redefinition adds no storage");

        CobolRedefinesView month = model.getRedefinesViews().get("WS-MMDD");
        assertEquals("WS-DATE-AREA", month.base());
        assertTrue(month.baseIsGroup());
        assertEquals(4, month.offset());
        assertEquals(8, model.getRedefinesViews().get("WS-DATE-PARTS").length());
        assertFalse(model.getRedefinesViews().get("WS-LONG").supported(), "longer than its base");
    }

    @Test
    void aPeriodInsideAValueLiteralDoesNotEndTheEntry() {
        var values = CobolRecordLayoutExtractor.initialValues(String.join("\n",
                "       01 MESSAGES.",
                "         05 MSG-THANKS   PIC X(30) VALUE",
                "              'Thanks for using it...    '.",
                "         05 MSG-NEXT     PIC X(04) VALUE 'NEXT'."));

        assertEquals("'Thanks for using it...    '", values.get("MSG-THANKS"));
        assertEquals("'NEXT'", values.get("MSG-NEXT"));
    }
}
