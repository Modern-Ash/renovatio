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
    void casePromptMustMatchSuitePromptIdentity(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        output.put("prompt", "alternate-domain-eval@2026-09-07");
        Path outputPath = writeJson(temp.resolve("output-alternate-prompt.json"), output);

        ObjectNode suite = oneCaseSuiteWithOutput(outputPath);
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        ((ObjectNode) firstCase.path("prompt")).put("id", "alternate-domain-eval");
        Path suitePath = writeJson(temp.resolve("suite-case-prompt-mismatch.json"), suite);

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("case prompt binding mismatch"));
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("output prompt binding mismatch"));
    }

    @Test
    void outputModelMustMatchSuiteModel(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        output.put("model", "different-model");
        Path outputPath = writeJson(temp.resolve("output-wrong-model.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-wrong-output-model.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("output model binding mismatch"));
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

    @Test
    void nonArrayDecisionsFailClosed(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        output.set("decisions", OBJECT_MAPPER.createObjectNode().put("irRef", "program:CUSTOMER-BILLING"));
        Path outputPath = writeJson(temp.resolve("output-non-array-decisions.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-non-array-decisions.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("decisions must be a non-empty array"));
    }

    @Test
    void nestedPromotionFieldsFailClosed(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        ObjectNode metadata = OBJECT_MAPPER.createObjectNode();
        metadata.put("codePatch", "diff --git a/generated.java b/generated.java");
        output.set("metadata", metadata);
        Path outputPath = writeJson(temp.resolve("output-nested-promotion.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-nested-promotion.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("attempts to promote"));
    }

    @Test
    void outOfRangeRubricScoresFailClosed(@TempDir Path temp) throws Exception {
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        ((ObjectNode) output.path("rubricScores")).put("entities", 2.0);
        Path outputPath = writeJson(temp.resolve("output-invalid-rubric.json"), output);
        Path suitePath = writeJson(temp.resolve("suite-invalid-rubric.json"), oneCaseSuiteWithOutput(outputPath));

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("score entities must be between 0 and 1"));
        assertTrue(report.path("cases").get(0).path("weightedScore").asDouble() <= 1.0);
    }

    @Test
    void casesWithoutRubricDefinitionsFailClosed(@TempDir Path temp) throws Exception {
        ObjectNode suite = oneCaseSuiteWithOutput(resources.resolve("outputs/customer-billing-valid.json").toAbsolutePath());
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.set("rubrics", OBJECT_MAPPER.createArrayNode());
        Path suitePath = writeJson(temp.resolve("suite-empty-rubrics.json"), suite);

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("definitions must be a non-empty array"));
    }

    @Test
    void invalidRubricConfigurationFailsClosed(@TempDir Path temp) throws Exception {
        ObjectNode suite = oneCaseSuiteWithOutput(resources.resolve("outputs/customer-billing-valid.json").toAbsolutePath());
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        ((ObjectNode) firstCase.withArray("rubrics").get(0)).put("weight", 0.0);
        ((ObjectNode) firstCase.withArray("rubrics").get(1)).put("minimum", 1.5);
        Path suitePath = writeJson(temp.resolve("suite-invalid-rubric-config.json"), suite);

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertEquals(1, report.path("invalidOutputs").asInt());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("weight for entities must be positive"));
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("minimum for use-cases must be between 0 and 1"));
    }

    @Test
    void baselinesMustMatchSuiteIdentity(@TempDir Path temp) throws Exception {
        ObjectNode baseline = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("baseline-valid.json").toFile());
        baseline.put("promptVersion", "2026-09-06");
        Path baselinePath = writeJson(temp.resolve("baseline-wrong-prompt-version.json"), baseline);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> harness.evaluate(resources.resolve("suite-valid.json"), baselinePath));
        assertTrue(exception.getMessage().contains("baseline.promptVersion must be 2026-09-07"));
    }

    @Test
    void bareRelativeSuitePathsResolveAgainstWorkingDirectory(@TempDir Path temp) throws Exception {
        Path outputPath = temp.resolve("output-bare-relative.json");
        Path suitePath = Path.of("suite-bare-relative.json");
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        writeJson(outputPath, output);

        ObjectNode suite = oneCaseSuiteWithOutput(outputPath);
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.put("fixture", resources.resolve("fixtures/customer-billing.cob").toAbsolutePath().toString());
        firstCase.put("sampleOutput", outputPath.toString());
        writeJson(suitePath, suite);

        JsonNode report;
        try {
            report = harness.evaluate(suitePath, null);
        } finally {
            Files.deleteIfExists(suitePath);
        }

        assertEquals("PASS", report.path("gate").asText());
    }

    @Test
    void statementNamesAreNotParsedAsParagraphReferences(@TempDir Path temp) throws Exception {
        Path fixturePath = temp.resolve("statement-name.cob");
        Files.writeString(fixturePath, """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. STATEMENT-NAME.
                PROCEDURE DIVISION.
                MAIN.
                    CONTINUE.
                    STOP RUN.
                """);
        ObjectNode output = (ObjectNode) OBJECT_MAPPER.readTree(resources.resolve("outputs/customer-billing-valid.json").toFile());
        ((ObjectNode) output.withArray("decisions").get(0)).put("irRef", "paragraph:CONTINUE");
        Path outputPath = writeJson(temp.resolve("output-statement-name.json"), output);

        ObjectNode suite = oneCaseSuiteWithOutput(outputPath);
        ObjectNode firstCase = (ObjectNode) suite.withArray("cases").get(0);
        firstCase.put("fixture", fixturePath.toString());
        firstCase.put("sampleOutput", outputPath.toString());
        firstCase.withArray("irReferences").removeAll();
        firstCase.withArray("irReferences").add("program:STATEMENT-NAME");
        firstCase.withArray("irReferences").add("paragraph:MAIN");
        Path suitePath = writeJson(temp.resolve("suite-statement-name.json"), suite);

        JsonNode report = harness.evaluate(suitePath, null);

        assertEquals("FAIL", report.path("gate").asText());
        assertTrue(report.path("cases").get(0).path("failures").toString().contains("unknown IR reference paragraph:CONTINUE"));
    }

    private static Path writeJson(Path path, JsonNode json) throws Exception {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
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
