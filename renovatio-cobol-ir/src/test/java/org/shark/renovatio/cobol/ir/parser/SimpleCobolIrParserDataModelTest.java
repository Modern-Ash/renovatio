package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.model.Level88Condition;
import org.shark.renovatio.cobol.ir.model.Level88Value;
import org.shark.renovatio.cobol.runtime.PicType;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SimpleCobolIrParserDataModelTest {

    @Test
    void parse_shouldAttachRichPicTypesAndLevel88ConditionsToTheirParents() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DATA-MODEL.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-AMOUNT PIC S9(7)V99 COMP-3.
                01 WS-STATUS PIC X.
                   88 STATUS-ACTIVE VALUE 'A'.
                   88 STATUS-CLOSED VALUES 'C', 'X'.
                   88 STATUS-KNOWN VALUE 'A' THRU 'C'.
                   88 STATUS-OVERLAP VALUE 'B' THRU 'Z'.
                01 WS-COUNT PIC 9(4) COMP.
                PROCEDURE DIVISION.
                MAIN.
                    GOBACK.
                """;

        CobolIntermediateModel model = new SimpleCobolIrParser().parse(cobol);
        Map<String, CobolDataItem> items = model.getDataItems().stream()
                .collect(Collectors.toMap(CobolDataItem::name, Function.identity()));

        PicType amount = items.get("WS-AMOUNT").picType();
        assertEquals(PicType.Category.NUMERIC, amount.category());
        assertEquals(9, amount.digits());
        assertEquals(2, amount.scale());
        assertTrue(amount.signed());
        assertEquals(PicType.Usage.COMP_3, amount.usage());

        CobolDataItem status = items.get("WS-STATUS");
        assertEquals(PicType.Category.ALPHANUMERIC, status.picType().category());
        assertEquals(4, status.level88Conditions().size());
        assertEquals("WS-STATUS", status.level88Conditions().get(0).parentDataName());
        assertEquals(Level88Value.exact("A"), status.level88Conditions().get(0).values().get(0));
        assertEquals(2, status.level88Conditions().get(1).values().size());
        assertEquals(Level88Value.exact("C"), status.level88Conditions().get(1).values().get(0));
        assertEquals(Level88Value.exact("X"), status.level88Conditions().get(1).values().get(1));
        assertEquals(Level88Value.range("A", "C"), status.level88Conditions().get(2).values().get(0));
        assertEquals(Level88Value.range("B", "Z"), status.level88Conditions().get(3).values().get(0));

        Level88Condition overlapping = status.level88Conditions().get(3);
        assertEquals("STATUS-OVERLAP", overlapping.name());
        assertTrue(items.get("WS-COUNT").level88Conditions().isEmpty());
        assertEquals(PicType.Usage.COMP, items.get("WS-COUNT").picType().usage());
    }

    @Test
    void parse_shouldTreatCommasAsLevel88SeparatorsRatherThanValues() {
        String cobol = """
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-CODE PIC 9.
                   88 VALID-CODE VALUES 1, 2, 3.
                PROCEDURE DIVISION.
                MAIN.
                    GOBACK.
                """;

        var values = new SimpleCobolIrParser().parse(cobol).getDataItems().get(0)
                .level88Conditions().get(0).values();

        assertEquals(java.util.List.of(
                Level88Value.exact("1"), Level88Value.exact("2"), Level88Value.exact("3")), values);
    }

    @Test
    void legacyConstructor_shouldRemainCompatibleAndUseEmptySemanticMetadata() {
        CobolDataItem item = new CobolDataItem("LEGACY", "X(3)", 1, null, null, "String");

        assertNull(item.picType());
        assertEquals(java.util.List.of(), item.level88Conditions());
    }

    @Test
    void level88Value_shouldRejectInvalidEmptyRangeEndpoint() {
        assertThrows(NullPointerException.class, () -> Level88Value.exact(null));
        assertThrows(NullPointerException.class, () -> Level88Value.range("A", null));
    }

    @Test
    void parse_shouldDiagnoseMalformedPicAndJavaNameCollisionsInSourceOrder() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. INVALID-DATA.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 A-B PIC X.
                01 A--B PIC 9.
                01 BAD-PIC PIC ???.
                PROCEDURE DIVISION.
                MAIN.
                    GOBACK.
                """;

        CobolIntermediateModel model = new SimpleCobolIrParser().parse(cobol);

        assertEquals(2, model.getDiagnostics().size());
        assertEquals("COBOL-NAME-001", model.getDiagnostics().get(0).code());
        assertEquals("COBOL-PIC-001", model.getDiagnostics().get(1).code());
        assertTrue(model.getDataItems().stream()
                .filter(item -> item.name().equals("BAD-PIC"))
                .allMatch(item -> item.picType() == null));
    }
}
