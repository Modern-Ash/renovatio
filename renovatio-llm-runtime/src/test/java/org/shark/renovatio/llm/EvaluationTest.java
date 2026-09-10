package org.shark.renovatio.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.llm.provider.FakeLLMProvider;
import org.shark.renovatio.llm.domain.LLMConfig;
import org.shark.renovatio.llm.domain.ProposalRequest;
import org.shark.renovatio.llm.domain.TypedProposal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluation test running against versioned corpus.
 * Measures schema validity, determinism/replay, acceptance quality, unsafe output, and regression.
 */
class EvaluationTest {

    private FakeLLMProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FakeLLMProvider();
        provider.configure(LLMConfig.builder().build());
    }

    private Path getCorpusRoot() {
        // From module directory (renovatio-llm-runtime/), the test resources are at:
        Path p1 = Paths.get("target/test-classes/evals/corpus/v1");
        if (Files.exists(p1)) return p1;

        Path p2 = Paths.get("src/test/resources/evals/corpus/v1");
        if (Files.exists(p2)) return p2;

        // Fallback
        return Paths.get("src/test/resources/evals/corpus/v1");
    }

    @Test
    void shouldValidateSchemaForAllCorpusCases() throws IOException {
        Path corpusRoot = getCorpusRoot();
        System.err.println("Corpus root: " + corpusRoot.toAbsolutePath());

        try (Stream<Path> files = Files.walk(getCorpusRoot())) {
            List<Path> filesList = files.filter(p -> p.toString().endsWith("_expected.json"))
                .toList();
            System.err.println("Found " + filesList.size() + " expected files");

            filesList.forEach(expectedPath -> {
                try {
                    // Use absolute path for reading
                    Path absolutePath = expectedPath.toAbsolutePath();
                    String expectedJson = Files.readString(absolutePath);
                    System.err.println("Reading: " + absolutePath + " length=" + expectedJson.length());
                    System.err.println("First 200 chars: " + expectedJson.substring(0, Math.min(200, expectedJson.length())));
                    var expected = new com.fasterxml.jackson.databind.ObjectMapper().readTree(expectedJson);
                    System.err.println("Parsed version: '" + expected.path("version").asText() + "' kind: '" + expected.path("kind").asText() + "'");
                    System.err.println("File: " + absolutePath.getFileName());

                    // Verify required fields exist - use appropriate checks for different node types
                    assertThat(expected.path("version").asText()).isNotEmpty();
                    assertThat(expected.path("kind").asText()).isNotEmpty();
                    assertThat(expected.path("proposal").isObject()).isTrue();
                    assertThat(expected.path("confidence").asDouble()).isBetween(0.0, 1.0);
                    assertThat(expected.path("rationale").asText()).isNotEmpty();
                    assertThat(expected.path("schema").asText()).isNotEmpty();
                    assertThat(expected.path("metadata").isObject()).isTrue();

                    // Verify confidence is in valid range
                    double confidence = expected.path("confidence").asDouble();
                    assertThat(confidence).isBetween(0.0, 1.0);

                    // Verify metadata fields
                    var metadata = expected.path("metadata");
                    System.err.println("Metadata model: '" + metadata.path("model").asText() + "'");
                    System.err.println("Metadata model isEmpty: " + metadata.path("model").isEmpty());
                    assertThat(metadata.path("model").asText()).isNotEmpty();
                    assertThat(metadata.path("promptHash").asText()).isNotEmpty();
                    assertThat(metadata.path("inputHash").asText()).isNotEmpty();
                    assertThat(metadata.path("outputHash").asText()).isNotEmpty();
                    assertThat(metadata.path("latencyMs").asLong()).isGreaterThan(0);
                    assertThat(metadata.path("tokensUsed").asInt()).isGreaterThan(0);
                    assertThat(metadata.path("cacheStatus").asText()).isNotEmpty();

                } catch (Exception e) {
                    throw new AssertionError("Failed to validate " + expectedPath, e);
                }
            });
        }
    }

    @Test
    void shouldHaveNoUnsafeOutputInCorpus() throws IOException {
        try (Stream<Path> files = Files.walk(getCorpusRoot())) {
            files.filter(p -> p.toString().endsWith("_expected.json"))
                .forEach(expectedPath -> {
                    try {
                        String expectedJson = Files.readString(expectedPath);

                        // Check for unsafe patterns
                        assertThat(expectedJson).doesNotContain("ignore previous instructions");
                        assertThat(expectedJson).doesNotContain("jailbreak");
                        assertThat(expectedJson).doesNotContain("system prompt");
                        assertThat(expectedJson).doesNotContain("rm -rf");
                        assertThat(expectedJson).doesNotContain("DROP TABLE");
                        assertThat(expectedJson).doesNotContain("DELETE FROM");

                    } catch (Exception e) {
                        throw new AssertionError("Failed to validate " + expectedPath, e);
                    }
                });
        }
    }

    @Test
    void shouldHaveDeterministicOutput() throws IOException {
        // Test that fake provider produces deterministic output for same input
        String sourceHash = "sha256:determinism_test";

        ProposalRequest request = ProposalRequest.builder()
            .version("1.0")
            .kind("cobol.structure_analysis")
            .input(new ProposalRequest.Input(sourceHash, Map.of("programName", "TEST")))
            .schema("proposal-request.v1.json")
            .budget(new ProposalRequest.Budget(5000, 5000, 0.50))
            .model(new ProposalRequest.Model("gemini-2.0-flash", "2025-01"))
            .build();

        // Run multiple times
        TypedProposal first = provider.propose(request);
        TypedProposal second = provider.propose(request);
        TypedProposal third = provider.propose(request);

        // Should be identical (deterministic)
        assertThat(first).isEqualTo(second);
        assertThat(second).isEqualTo(third);
        assertThat(first.metadata().cacheStatus()).isEqualTo("BYPASS");
    }

    @Test
    void shouldAchieveMinimumAcceptanceQuality() throws IOException {
        Path corpusRoot = getCorpusRoot();

        try (Stream<Path> files = Files.walk(getCorpusRoot())) {
            long expectedCount = files.filter(p -> p.toString().endsWith("_expected.json")).count();

            // We expect at least 3 cases (one per category)
            assertThat(expectedCount).isGreaterThanOrEqualTo(3);
        }

        // Quality threshold: all cases must have confidence > 0.7
        try (Stream<Path> files = Files.walk(getCorpusRoot())) {
            files.filter(p -> p.toString().endsWith("_expected.json"))
                .forEach(expectedPath -> {
                    try {
                        String expectedJson = Files.readString(expectedPath);
                        var expected = new com.fasterxml.jackson.databind.ObjectMapper().readTree(expectedJson);
                        double confidence = expected.path("confidence").asDouble();
                        assertThat(confidence).isGreaterThan(0.7);
                    } catch (Exception e) {
                        throw new AssertionError("Failed to validate " + expectedPath, e);
                    }
                });
        }
    }
}