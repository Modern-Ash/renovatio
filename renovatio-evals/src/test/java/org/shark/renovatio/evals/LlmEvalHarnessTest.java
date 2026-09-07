package org.shark.renovatio.evals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void rootFinalCodePromotionViolatesHumanReviewBoundary(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        output.put("finalCode", "class CustomerBillingService {}");
        Path outputPath = writeJson(temp.resolve("output-root-final-code.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-root-final-code.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("output attempts to promote"));
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
        assertEquals("renovatio.llm-eval-report.v1", written.path("schemaVersion").asText());
        assertEquals("1970-01-01T00:00:00Z", written.path("generatedAt").asText());
        assertEquals(report.path("gate").asText(), written.path("gate").asText());
        assertEquals(report.path("totalCases").asInt(), written.path("totalCases").asInt());
        assertEquals(report.path("metrics").path("fallbackRate").asDouble(), written.path("metrics").path("fallbackRate").asDouble());
    }

    @Test
    void suiteIdentityFieldsAreRequired(@TempDir Path temp) throws Exception {
        ObjectNode suite = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("suite-valid.json").toFile());
        suite.remove("id");
        Path suitePath = writeJson(temp.resolve("suite-missing-id.json"), suite);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> harness.evaluate(suitePath, resources.resolve("baseline-valid.json")));
        assertTrue(exception.getMessage().contains("suite.id is required"));
    }

    @Test
    void declaredIrReferencesMustResolveAgainstFixture(@TempDir Path temp) throws Exception {
        ObjectNode suite = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("suite-valid.json").toFile());
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.put("fixture", resources.resolve("fixtures/customer-billing.cob").toAbsolutePath().toString());
        firstCase.put("sampleOutput", resources.resolve("outputs/customer-billing-valid.json").toAbsolutePath().toString());
        firstCase.withArray("irReferences").add("paragraph:INVENTED");
        ArrayNode cases = OBJECT_MAPPER.createArrayNode();
        cases.add(firstCase);
        suite.set("cases", cases);
        Path suitePath = writeJson(temp.resolve("suite-invented-ref.json"), suite);

        JsonNode report = harness.evaluate(suitePath, resources.resolve("baseline-valid.json"));

        assertEquals("FAIL", report.path("gate").asText());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("fixture:"));
    }

    @Test
    void criticalCasesRequireBaselineCoverage(@TempDir Path temp) throws Exception {
        ObjectNode baseline = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("baseline-valid.json").toFile());
        ArrayNode cases = OBJECT_MAPPER.createArrayNode();
        cases.add(baseline.withArray("cases").get(1));
        baseline.set("cases", cases);
        Path baselinePath = writeJson(temp.resolve("baseline-missing-critical.json"), baseline);

        JsonNode report = harness.evaluate(resources.resolve("suite-valid.json"), baselinePath);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("criticalRegressions").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("missing baseline"));
    }

    @Test
    void fallbackAggregationUsesMetricsFallbackRate(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        output.remove("fallback");
        ((ObjectNode) output.path("metrics")).put("fallbackRate", 1.0);
        Path outputPath = writeJson(temp.resolve("output-with-metric-fallback.json"), output);

        ObjectNode suite = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("suite-valid.json").toFile());
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.put("fixture", resources.resolve("fixtures/customer-billing.cob").toAbsolutePath().toString());
        firstCase.put("sampleOutput", outputPath.toString());
        ArrayNode cases = OBJECT_MAPPER.createArrayNode();
        cases.add(firstCase);
        suite.set("cases", cases);
        Path suitePath = writeJson(temp.resolve("suite-metric-fallback.json"), suite);

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals(1.0, report.path("metrics").path("fallbackRate").asDouble());
    }

    @Test
    void invalidMetricTypesAndRangesFailClosed(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        ObjectNode metrics = (ObjectNode) output.path("metrics");
        metrics.put("acceptanceRate", 2.0);
        metrics.put("fallbackRate", -2.0);
        metrics.put("costUsd", -10.0);
        metrics.put("latencyMs", -1);
        metrics.put("cacheHit", "yes");
        Path outputPath = writeJson(temp.resolve("output-invalid-metrics.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-invalid-metrics.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("metrics:"));
        assertEquals(0.0, report.path("metrics").path("fallbackRate").asDouble());
        assertEquals(0.0, report.path("metrics").path("totalCostUsd").asDouble());
        assertEquals(0, report.path("metrics").path("averageLatencyMs").asInt());
    }

    private static Path writeJson(Path path, JsonNode json) throws Exception {
        Files.createDirectories(path.getParent());
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), json);
        return path;
    }

    private ObjectNode oneCaseSuiteWithOutput(Path outputPath) throws Exception {
        ObjectNode suite = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("suite-valid.json").toFile());
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.put("fixture", resources.resolve("fixtures/customer-billing.cob").toAbsolutePath().toString());
        firstCase.put("sampleOutput", outputPath.toString());
        ArrayNode cases = OBJECT_MAPPER.createArrayNode();
        cases.add(firstCase);
        suite.set("cases", cases);
        return suite;
    }
}
