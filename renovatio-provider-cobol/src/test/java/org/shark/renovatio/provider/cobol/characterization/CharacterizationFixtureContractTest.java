package org.shark.renovatio.provider.cobol.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.guardrail.GuardrailSchemaCatalog;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.java.OpenRewriteRunner;

import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterizationFixtureContractTest {
    private static final List<String> FIXTURES = List.of(
            "move-numeric", "move-alphanumeric-boundaries", "compute-decimal-sign", "if-nested",
            "evaluate-level-88", "perform-simple-nested", "goto-reducible", "goto-irreducible",
            "redefines-overlap", "odo-valid-boundary", "odo-invalid-count", "unsupported-construct");
    /* Only fixtures exercised end-to-end by today's production translator may be admitted here. */
    private static final Set<String> SUPPORTED = Set.of("move-numeric");

    private final ObjectMapper mapper = new ObjectMapper();
    private final CobolIntermediateModelService modelService = new CobolIntermediateModelService();
    private final CobolSemanticTranspiler transpiler = new CobolSemanticTranspiler(new OpenRewriteRunner());

    @Test
    void corpusExercisesProductionTranslationAndResidualContracts(@TempDir Path compilationOutput) throws Exception {
        Path corpus = corpus();
        try (var fixturePaths = Files.list(corpus)) {
            assertThat(fixturePaths.filter(Files::isDirectory).map(path -> path.getFileName().toString()))
                    .containsExactlyInAnyOrderElementsOf(FIXTURES);
        }
        for (String fixtureId : FIXTURES) {
            Path fixture = corpus.resolve(fixtureId);
            Path cobol = fixture.resolve("input.cob");
            assertThat(cobol).isRegularFile();
            JsonNode behavior = mapper.readTree(fixture.resolve("expected-behavior.json").toFile());
            assertThat(behavior.path("fixtureId").asText()).isEqualTo(fixtureId);
            assertThat(behavior.path("observations").isArray()).isTrue();

            Path actionItems = fixture.resolve("expected-action-items.json");
            assertSchemaValid("manual-action-item.v1", actionItems);
            int actionCount = mapper.readTree(actionItems.toFile()).path("items").size();
            if (SUPPORTED.contains(fixtureId)) {
                assertThat(actionCount).isZero();
                assertSchemaValid("cobol-ir.v1", fixture.resolve("expected-ir.json"));
                String first = translate(cobol, fixture.resolve("translation-input.java"));
                String second = translate(cobol, fixture.resolve("translation-input.java"));
                assertThat(first).as("independent production translations for %s", fixtureId).isEqualTo(second);
                assertThat(first).as("committed production golden for %s", fixtureId)
                        .isEqualTo(Files.readString(fixture.resolve("expected.java")));
                Path output = compilationOutput.resolve(fixtureId);
                assertCompiles(first, output);
                assertBehavior(output, behavior);
            } else {
                assertThat(actionCount).isPositive();
                assertThat(fixture.resolve("expected.java")).doesNotExist();
                assertThat(fixture.resolve("translation-input.java")).doesNotExist();
            }
        }
    }

    private String translate(Path cobol, Path javaStub) throws Exception {
        CobolIntermediateModel model = modelService.parse(Files.readString(cobol));
        return transpiler.enrichServiceImplementation(Files.readString(javaStub), model);
    }

    private void assertBehavior(Path output, JsonNode behavior) throws Exception {
        JsonNode observation = behavior.path("observations").get(0);
        try (URLClassLoader loader = new URLClassLoader(new URL[]{output.toUri().toURL()})) {
            Object actual = loader.loadClass("fixture.CharacterizationFixture").getDeclaredMethod("run").invoke(null);
            assertThat(actual.toString()).isEqualTo(observation.path("value").asText());
        }
    }

    private void assertSchemaValid(String version, Path document) throws Exception {
        JsonNode schemaNode = new GuardrailSchemaCatalog(mapper).resolve(version);
        var schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(schemaNode);
        assertThat(schema.validate(mapper.readTree(document.toFile())))
                .as("schema violations for %s", document).isEmpty();
    }

    private static void assertCompiles(String javaSource, Path output) throws Exception {
        Files.createDirectories(output);
        Path source = output.resolve("CharacterizationFixture.java");
        Files.writeString(source, javaSource);
        int exitCode = ToolProvider.getSystemJavaCompiler().run(
                null, null, null, "-d", output.toString(), source.toString());
        assertThat(exitCode).as("javac exit code for generated source").isZero();
    }

    private static Path corpus() throws Exception {
        return Path.of(CharacterizationFixtureContractTest.class.getResource("/characterization").toURI());
    }
}
