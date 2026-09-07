package org.shark.renovatio.evals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmEvalHarnessTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final LlmEvalHarness harness = new LlmEvalHarness();
    private final Path resources = Path.of("src/test/resources/evals");

    @Test
    void validCriticalFixturePassesComparableBaseline() throws Exception {
        JsonNode report = harness.evaluate(resources.resolve("suite-valid.json"), resources.resolve("baseline-valid.json"));

        assertEquals("PASS", report.path("gate").asText());
        assertEquals(2, report.path("passedCases").asInt());
        assertEquals(0, report.path("criticalRegressions").asInt());
        assertEquals(0, report.path("invalidOutputs").asInt());
        assertTrue(report.path("metrics").has("acceptanceRate"));
        assertTrue(report.path("metrics").has("fallbackRate"));
        assertTrue(report.path("metrics").has("totalCostUsd"));
        assertTrue(report.path("metrics").has("averageLatencyMs"));
        assertTrue(report.path("metrics").has("cacheHitRate"));
    }

    @Test
    void hallucinatedIrReferenceFailsClosed() throws Exception {
        JsonNode report = harness.evaluate(resources.resolve("suite-hallucination.json"), resources.resolve("baseline-valid.json"));

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").get(0).asText().startsWith("hallucination:"));
    }

    @Test
    void finalCodePromotionViolatesHumanReviewBoundary() throws Exception {
        JsonNode report = harness.evaluate(resources.resolve("suite-final-code.json"), resources.resolve("baseline-valid.json"));

        assertEquals("FAIL", report.path("gate").asText());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("safety:"));
    }

    @Test
    void criticalRegressionFailsPromptComparisonGate() throws Exception {
        JsonNode report = harness.evaluate(resources.resolve("suite-regression.json"), resources.resolve("baseline-valid.json"));

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("criticalRegressions").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("regression:"));
    }

    @Test
    void writesDeterministicComparableReport(@TempDir Path temp) throws Exception {
        Path reportPath = temp.resolve("llm-eval-report.json");

        harness.writeReport(resources.resolve("suite-valid.json"), resources.resolve("baseline-valid.json"), reportPath);

        JsonNode report = harness.evaluate(resources.resolve("suite-valid.json"), resources.resolve("baseline-valid.json"));
        JsonNode written = OBJECT_MAPPER.readTree(Files.readString(reportPath));
        assertEquals(OBJECT_MAPPER.writeValueAsString(report), OBJECT_MAPPER.writeValueAsString(written));
        assertEquals("renovatio.llm-eval-report.v1", written.path("schemaVersion").asText());
        assertEquals("1970-01-01T00:00:00Z", written.path("generatedAt").asText());
        assertEquals("PASS", written.path("gate").asText());
    }
}
