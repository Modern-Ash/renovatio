package org.shark.renovatio.llm.eval;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LlmEvaluationTest {
 @Test void requiresCleanSchemaAndProvenance() {
  assertTrue(new LlmEvaluation("ds-v1", 10, 10, 0, 0, null).passes(1));
  assertFalse(new LlmEvaluation("ds-v1", 10, 10, 1, 0, null).passes(1));
 }
}
