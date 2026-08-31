package org.shark.renovatio.provider.cobol.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;

import javax.tools.ToolProvider;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterizationFixtureContractTest {

    private static final List<String> FIXTURES = List.of(
            "move-numeric", "move-alphanumeric-boundaries", "compute-decimal-sign", "if-nested",
            "evaluate-level-88", "perform-simple-nested", "goto-reducible", "goto-irreducible",
            "redefines-overlap", "odo-valid-boundary", "odo-invalid-count", "unsupported-construct");
    private static final Set<String> SUPPORTED = Set.of(
            "move-numeric", "move-alphanumeric-boundaries", "compute-decimal-sign", "if-nested",
            "evaluate-level-88", "perform-simple-nested", "goto-reducible");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void corpusSatisfiesSupportedAndResidualContracts(@TempDir Path compilationOutput) throws Exception {
        Path corpus = corpus();
        try (var fixturePaths = Files.list(corpus)) {
            assertThat(fixturePaths.filter(Files::isDirectory).map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrderElementsOf(FIXTURES);
        }

        for (String fixtureId : FIXTURES) {
            Path fixture = corpus.resolve(fixtureId);
            assertThat(fixture.resolve("input.cob")).isRegularFile();
            assertThat(Files.readString(fixture.resolve("input.cob"))).isNotBlank();
            JsonNode behavior = mapper.readTree(fixture.resolve("expected-behavior.json").toFile());
            assertThat(behavior.path("fixtureId").asText()).isEqualTo(fixtureId);
            assertThat(behavior.path("observations").isArray()).isTrue();

            Path actionItems = fixture.resolve("expected-action-items.json");
            assertSchemaValid("manual-action-item.v1", actionItems);
            int actionCount = mapper.readTree(actionItems.toFile()).path("items").size();

            if (SUPPORTED.contains(fixtureId)) {
                Path ir = fixture.resolve("expected-ir.json");
                Path java = fixture.resolve("expected.java");
                assertSchemaValid("cobol-ir.v1", ir);
                assertThat(actionCount).isZero();
                assertCompiles(java, compilationOutput.resolve(fixtureId));
            } else {
                assertThat(actionCount).isPositive();
                assertThat(fixture.resolve("expected.java")).doesNotExist();
            }
        }
    }

    @Test
    void committedOutputsAreByteStableAcrossRepeatedReads() throws Exception {
        Path corpus = corpus();
        for (String fixtureId : FIXTURES) {
            try (var paths = Files.list(corpus.resolve(fixtureId))) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    assertThat(sha256(Files.readAllBytes(path))).isEqualTo(sha256(Files.readAllBytes(path)));
                    assertThat(Files.readString(path)).doesNotContain("\r\n");
                }
            }
        }
    }

    private void assertSchemaValid(String version, Path document) throws Exception {
        JsonNode schemaNode = new GuardrailSchemaCatalog(mapper).resolve(version);
        var schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        assertThat(schema.validate(mapper.readTree(document.toFile())))
                .as("schema violations for %s", document)
                .isEmpty();
    }

    private static void assertCompiles(Path javaSource, Path output) throws Exception {
        Files.createDirectories(output);
        int exitCode = ToolProvider.getSystemJavaCompiler().run(
                null, null, null, "-d", output.toString(), javaSource.toString());
        assertThat(exitCode).as("javac exit code for %s", javaSource).isZero();
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static Path corpus() throws URISyntaxException {
        return Path.of(CharacterizationFixtureContractTest.class.getResource("/characterization").toURI());
    }
}
