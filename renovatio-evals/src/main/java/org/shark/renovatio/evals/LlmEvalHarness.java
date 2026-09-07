package org.shark.renovatio.evals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LlmEvalHarness {
    public static final String SUITE_SCHEMA = "renovatio.llm-eval-suite.v1";
    public static final String OUTPUT_SCHEMA = "renovatio.llm-eval-output.v1";
    public static final String REPORT_SCHEMA = "renovatio.llm-eval-report.v1";
    private static final Set<String> REVIEWABLE_STATES = Set.of("PROPOSED", "NEEDS_REVIEW", "REJECTED");
    private static final Set<String> COBOL_STRUCTURAL_WORDS = Set.of(
            "IDENTIFICATION", "PROGRAM-ID", "DATA", "WORKING-STORAGE", "PROCEDURE", "DIVISION", "SECTION",
            "IF", "ELSE", "END-IF", "DISPLAY", "STOP", "RUN");
    private static final Pattern PROGRAM_ID = Pattern.compile("(?m)^\\s*PROGRAM-ID\\.\\s+([A-Z0-9-]+)\\.");
    private static final Pattern DATA_NAME = Pattern.compile("(?m)^\\s*(\\d{2})\\s+([A-Z0-9-]+)\\b");
    private static final Pattern PARAGRAPH = Pattern.compile("(?m)^\\s*([A-Z][A-Z0-9-]*)\\.");

    private final ObjectMapper mapper;

    public LlmEvalHarness() {
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    public ObjectNode evaluate(Path suitePath, Path baselinePath) throws IOException {
        JsonNode suite = mapper.readTree(suitePath.toFile());
        requireText(suite, "schemaVersion", SUITE_SCHEMA, "suite.schemaVersion");
        requirePresentText(suite, "id", "suite.id");
        requirePresentText(suite.path("prompt"), "id", "suite.prompt.id");
        requirePresentText(suite.path("prompt"), "version", "suite.prompt.version");
        requirePresentText(suite, "model", "suite.model");
        Map<String, BigDecimal> baselineScores = baselinePath == null ? Map.of() : loadBaselineScores(baselinePath);
        boolean baselineRequired = baselinePath != null;
        ArrayNode caseReports = mapper.createArrayNode();
        int total = 0;
        int passed = 0;
        int criticalRegressions = 0;
        int invalidOutputs = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        long totalLatency = 0;
        int cacheHits = 0;
        BigDecimal fallbackRateTotal = BigDecimal.ZERO;

        for (JsonNode evalCase : suite.withArray("cases")) {
            total++;
            CaseResult result = evaluateCase(suitePath.getParent(), evalCase, baselineScores, baselineRequired);
            caseReports.add(result.report);
            if (result.passed) {
                passed++;
            }
            if (result.criticalRegression) {
                criticalRegressions++;
            }
            if (result.invalidOutput) {
                invalidOutputs++;
            }
            totalCost = totalCost.add(result.costUsd);
            totalLatency += result.latencyMs;
            if (result.cacheHit) {
                cacheHits++;
            }
            fallbackRateTotal = fallbackRateTotal.add(result.fallbackRate);
        }

        ObjectNode metrics = mapper.createObjectNode();
        metrics.put("acceptanceRate", total == 0 ? 0.0 : (double) passed / total);
        metrics.put("fallbackRate", total == 0 ? BigDecimal.ZERO : fallbackRateTotal.divide(BigDecimal.valueOf(total), 4, java.math.RoundingMode.HALF_UP));
        metrics.put("totalCostUsd", totalCost);
        metrics.put("averageLatencyMs", total == 0 ? 0 : totalLatency / total);
        metrics.put("cacheHitRate", total == 0 ? 0.0 : (double) cacheHits / total);

        ObjectNode report = mapper.createObjectNode();
        report.put("schemaVersion", REPORT_SCHEMA);
        report.put("generatedAt", Instant.EPOCH.toString());
        report.put("suiteId", suite.path("id").asText());
        report.put("promptId", suite.path("prompt").path("id").asText());
        report.put("promptVersion", suite.path("prompt").path("version").asText());
        report.put("model", suite.path("model").asText());
        report.put("totalCases", total);
        report.put("passedCases", passed);
        report.put("failedCases", total - passed);
        report.put("criticalRegressions", criticalRegressions);
        report.put("invalidOutputs", invalidOutputs);
        report.set("metrics", metrics);
        report.set("cases", caseReports);
        report.put("gate", total > 0 && passed == total && criticalRegressions == 0 && invalidOutputs == 0 ? "PASS" : "FAIL");
        return report;
    }

    public void writeReport(Path suitePath, Path baselinePath, Path reportPath) throws IOException {
        ObjectNode report = evaluate(suitePath, baselinePath);
        Files.createDirectories(reportPath.getParent());
        mapper.writeValue(reportPath.toFile(), report);
    }

    private CaseResult evaluateCase(Path suiteRoot, JsonNode evalCase, Map<String, BigDecimal> baselineScores, boolean baselineRequired) throws IOException {
        String id = text(evalCase, "id");
        boolean critical = evalCase.path("critical").asBoolean(false);
        Path fixturePath = suiteRoot.resolve(text(evalCase, "fixture")).normalize();
        Path outputPath = suiteRoot.resolve(text(evalCase, "sampleOutput")).normalize();
        JsonNode output = mapper.readTree(outputPath.toFile());
        Set<String> fixtureRefs = extractFixtureReferences(fixturePath);

        ObjectNode report = mapper.createObjectNode();
        ArrayNode failures = mapper.createArrayNode();
        report.put("id", id);
        report.put("task", text(evalCase, "task"));
        report.put("critical", critical);

        validateDeclaredReferences(evalCase, fixtureRefs, failures);
        validateOutputSchema(evalCase, output, fixtureRefs, failures);
        BigDecimal weightedScore = scoreRubrics(evalCase, output, failures);
        JsonNode metrics = output.path("metrics");
        BigDecimal baseline = baselineScores.get(id);
        boolean missingCriticalBaseline = baselineRequired && critical && baseline == null;
        if (missingCriticalBaseline) {
            failures.add("regression: missing baseline for critical case " + id);
        }
        boolean regression = baseline != null && weightedScore.compareTo(baseline) < 0;
        if (regression) {
            failures.add("regression: weighted rubric score " + weightedScore + " below baseline " + baseline);
        }

        report.put("weightedScore", weightedScore);
        if (baseline != null) {
            report.put("baselineScore", baseline);
        }
        report.set("failures", failures);
        report.put("status", failures.isEmpty() ? "PASS" : "FAIL");
        return new CaseResult(
                report,
                failures.isEmpty(),
                critical && (regression || missingCriticalBaseline),
                hasInvalidOutputFailure(failures),
                decimal(metrics, "costUsd"),
                metrics.path("latencyMs").asLong(0),
                metrics.path("cacheHit").asBoolean(false),
                decimal(metrics, "fallbackRate"));
    }

    private void validateDeclaredReferences(JsonNode evalCase, Set<String> fixtureRefs, ArrayNode failures) {
        for (JsonNode ref : evalCase.withArray("irReferences")) {
            String value = ref.asText();
            if (!fixtureRefs.contains(value)) {
                failures.add("fixture: declared IR reference not found in fixture " + value);
            }
        }
    }

    private void validateOutputSchema(JsonNode evalCase, JsonNode output, Set<String> fixtureRefs, ArrayNode failures) {
        if (!OUTPUT_SCHEMA.equals(output.path("schemaVersion").asText())) {
            failures.add("schema: unsupported output schemaVersion");
        }
        String expectedPrompt = evalCase.path("prompt").path("id").asText() + "@" + evalCase.path("prompt").path("version").asText();
        if (!expectedPrompt.equals(output.path("prompt").asText())) {
            failures.add("schema: output prompt binding mismatch");
        }
        if (!output.has("decisions") || !output.path("decisions").isArray() || output.path("decisions").isEmpty()) {
            failures.add("schema: decisions must be a non-empty array");
        }
        for (JsonNode decision : output.withArray("decisions")) {
            String ref = decision.path("irRef").asText();
            if (!fixtureRefs.contains(ref)) {
                failures.add("hallucination: unknown IR reference " + ref);
            }
            if (!REVIEWABLE_STATES.contains(decision.path("reviewState").asText())) {
                failures.add("review-boundary: decision is not reviewable");
            }
            if (blank(decision.path("proposal").asText()) || blank(decision.path("rationale").asText())) {
                failures.add("schema: proposal and rationale are required");
            }
            if (decision.has("finalCode") || decision.has("codePatch") || decision.has("applied")) {
                failures.add("safety: output attempts to promote a suggestion as code");
            }
        }
        JsonNode metrics = output.path("metrics");
        for (String field : Set.of("acceptanceRate", "fallbackRate", "costUsd", "latencyMs", "cacheHit")) {
            if (!metrics.has(field)) {
                failures.add("metrics: missing " + field);
            }
        }
    }

    private BigDecimal scoreRubrics(JsonNode evalCase, JsonNode output, ArrayNode failures) {
        JsonNode scores = output.path("rubricScores");
        BigDecimal weighted = BigDecimal.ZERO;
        BigDecimal weightTotal = BigDecimal.ZERO;
        for (JsonNode rubric : evalCase.withArray("rubrics")) {
            String id = text(rubric, "id");
            BigDecimal weight = decimal(rubric, "weight");
            BigDecimal minimum = decimal(rubric, "minimum");
            BigDecimal score = scores.has(id) ? scores.get(id).decimalValue() : BigDecimal.valueOf(-1);
            if (score.signum() < 0) {
                failures.add("rubric: missing score " + id);
                score = BigDecimal.ZERO;
            }
            if (score.compareTo(minimum) < 0) {
                failures.add("rubric: " + id + " below minimum " + minimum);
            }
            weighted = weighted.add(score.multiply(weight));
            weightTotal = weightTotal.add(weight);
        }
        return weightTotal.signum() == 0 ? BigDecimal.ZERO : weighted.divide(weightTotal, 4, java.math.RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> loadBaselineScores(Path baselinePath) throws IOException {
        JsonNode baseline = mapper.readTree(baselinePath.toFile());
        Map<String, BigDecimal> scores = new HashMap<>();
        for (JsonNode evalCase : baseline.withArray("cases")) {
            scores.put(text(evalCase, "id"), decimal(evalCase, "weightedScore"));
        }
        return scores;
    }

    private Set<String> extractFixtureReferences(Path fixturePath) throws IOException {
        String fixture = Files.readString(fixturePath).toUpperCase();
        Set<String> refs = new HashSet<>();
        Matcher programMatcher = PROGRAM_ID.matcher(fixture);
        while (programMatcher.find()) {
            refs.add("program:" + programMatcher.group(1));
        }
        Matcher dataMatcher = DATA_NAME.matcher(fixture);
        while (dataMatcher.find()) {
            String level = dataMatcher.group(1);
            String name = dataMatcher.group(2);
            if ("88".equals(level)) {
                refs.add("condition:" + name);
            } else {
                refs.add("data:" + name);
            }
        }
        Matcher paragraphMatcher = PARAGRAPH.matcher(fixture);
        while (paragraphMatcher.find()) {
            String name = paragraphMatcher.group(1);
            if (!COBOL_STRUCTURAL_WORDS.contains(name)) {
                refs.add("paragraph:" + name);
            }
        }
        return refs;
    }

    private static boolean hasInvalidOutputFailure(ArrayNode failures) {
        Iterator<JsonNode> iterator = failures.elements();
        while (iterator.hasNext()) {
            String value = iterator.next().asText();
            if (value.startsWith("schema:") || value.startsWith("hallucination:") || value.startsWith("safety:")) {
                return true;
            }
        }
        return false;
    }

    private static void requireText(JsonNode node, String field, String expected, String label) {
        if (!expected.equals(node.path(field).asText())) {
            throw new IllegalArgumentException(label + " must be " + expected);
        }
    }

    private static void requirePresentText(JsonNode node, String field, String label) {
        if (blank(node.path(field).asText())) {
            throw new IllegalArgumentException(label + " is required");
        }
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.path(field).decimalValue();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record CaseResult(ObjectNode report, boolean passed, boolean criticalRegression, boolean invalidOutput,
                              BigDecimal costUsd, long latencyMs, boolean cacheHit, BigDecimal fallbackRate) {
    }
}
