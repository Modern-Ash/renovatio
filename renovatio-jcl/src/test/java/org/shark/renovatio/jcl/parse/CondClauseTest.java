package org.shark.renovatio.jcl.parse;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CondClauseTest {
    @Test
    void implementsSkipWhenPredicateIsTrueForEveryOperator() {
        assertTrue(CondClause.parse("(4,GT,S1)").shouldSkip(Map.of("S1", 8), false));
        assertTrue(CondClause.parse("(4,GE,S1)").shouldSkip(Map.of("S1", 4), false));
        assertTrue(CondClause.parse("(4,EQ,S1)").shouldSkip(Map.of("S1", 4), false));
        assertTrue(CondClause.parse("(4,LT,S1)").shouldSkip(Map.of("S1", 0), false));
        assertTrue(CondClause.parse("(4,LE,S1)").shouldSkip(Map.of("S1", 4), false));
        assertTrue(CondClause.parse("(4,NE,S1)").shouldSkip(Map.of("S1", 8), false));
        assertFalse(CondClause.parse("(4,EQ,S1)").shouldSkip(Map.of("S1", 0), false));
    }

    @Test
    void evenAndOnlyHaveExplicitAbendTruthTables() {
        assertFalse(CondClause.parse("EVEN").shouldSkip(Map.of(), true));
        assertFalse(CondClause.parse("ONLY").shouldSkip(Map.of(), true));
        assertTrue(CondClause.parse("ONLY").shouldSkip(Map.of(), false));
        assertEquals(Map.of("PRIOR=SUCCESS", false, "PRIOR=ABEND", true),
                CondClause.parse("ONLY").truthTable());
    }

    @Test
    void multiplePredicatesAreCombinedWithOr() {
        CondClause clause = CondClause.parse("((0,NE,S1),(4,LT,S2))");
        assertTrue(clause.shouldSkip(Map.of("S1", 0, "S2", 0), false));
        assertFalse(clause.shouldSkip(Map.of("S1", 0, "S2", 8), false));
        assertEquals(25, clause.truthTable().size());
        assertTrue(clause.truthTable().get("S1.RC=0,S2.RC=8"));
    }
}
