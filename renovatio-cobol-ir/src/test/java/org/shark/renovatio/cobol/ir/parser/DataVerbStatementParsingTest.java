package org.shark.renovatio.cobol.ir.parser;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.model.InitializeStatement;
import org.shark.renovatio.cobol.ir.model.SetConditionStatement;
import org.shark.renovatio.cobol.ir.model.SimpleStatement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataVerbStatementParsingTest {

    @Test
    void parsesInitializeAndSetLevel88FormsAsTypedStatements() {
        var model = new SimpleCobolIrParser().parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DATA-VERBS.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-NAME PIC X(8).
                01 WS-COUNT PIC 9(3).
                01 WS-STATUS PIC X.
                   88 READY VALUE 'Y'.
                PROCEDURE DIVISION.
                MAIN.
                    INITIALIZE WS-NAME, WS-COUNT.
                    SET READY TO TRUE.
                    SET READY TO FALSE.
                """);

        InitializeStatement initialize = (InitializeStatement) model.getEntryParagraph().statements().get(0);
        assertEquals(List.of("WS-NAME", "WS-COUNT"), initialize.targets());
        SetConditionStatement setTrue = (SetConditionStatement) model.getEntryParagraph().statements().get(1);
        SetConditionStatement setFalse = (SetConditionStatement) model.getEntryParagraph().statements().get(2);
        assertEquals(List.of("READY"), setTrue.conditionNames());
        assertEquals(true, setTrue.value());
        assertEquals(false, setFalse.value());
    }

    @Test
    void unsupportedVariantsStayVisibleAsUntranslatedStatements() {
        var model = new SimpleCobolIrParser().parse("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. DATA-RESIDUE.
                PROCEDURE DIVISION.
                MAIN.
                    INITIALIZE WS-NAME REPLACING ALPHANUMERIC DATA BY SPACES.
                    SET WS-INDEX UP BY 1.
                """);

        assertEquals(List.of(SimpleStatement.Kind.UNTRANSLATED, SimpleStatement.Kind.UNTRANSLATED),
                model.getEntryParagraph().statements().stream()
                        .map(SimpleStatement.class::cast)
                        .map(SimpleStatement::kind)
                        .toList());
    }

    @Test
    void typedStatementsRejectEmptyTargetsAndDefensivelyCopyInputs() {
        assertThrows(IllegalArgumentException.class, () -> new InitializeStatement(List.of(), "INITIALIZE"));
        assertThrows(IllegalArgumentException.class,
                () -> new SetConditionStatement(List.of(), true, "SET TO TRUE"));

        List<String> names = new java.util.ArrayList<>(List.of("ready"));
        SetConditionStatement statement = new SetConditionStatement(names, true, "SET READY TO TRUE");
        names.clear();
        assertEquals(List.of("READY"), statement.conditionNames());
    }
}
