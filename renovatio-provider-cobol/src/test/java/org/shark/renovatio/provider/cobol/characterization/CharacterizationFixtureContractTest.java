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
import org.shark.renovatio.provider.cobol.translation.CobolSemanticProjector;
import org.shark.renovatio.provider.cobol.translation.AnnotatedContextResolver;
import org.shark.renovatio.provider.java.OpenRewriteRunner;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.profile.MigrationProfiles;

import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class CharacterizationFixtureContractTest {
    private static final List<String> FIXTURES = List.of(
            "move-numeric", "move-alphanumeric-boundaries", "compute-decimal-sign", "if-nested",
            "evaluate-level-88", "perform-simple-nested", "goto-reducible", "goto-irreducible",
            "redefines-overlap", "odo-valid-boundary", "odo-invalid-count", "unsupported-construct",
            "data-intent-redefines");
    /* Only fixtures exercised end-to-end by today's production translator may be admitted here. */
    private static final Set<String> SUPPORTED = Set.of("move-numeric", "data-intent-redefines");

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
                Path annotatedSidecar = fixture.resolve(fixtureId + ".annotated.json");
                if (Files.exists(annotatedSidecar)) {
                    String annotatedFirst = translateAnnotated(cobol,
                            fixture.resolve("translation-input.java"), annotatedSidecar);
                    String annotatedSecond = translateAnnotated(cobol,
                            fixture.resolve("translation-input.java"), annotatedSidecar);
                    assertThat(annotatedFirst)
                            .as("independent annotated translations for %s", fixtureId)
                            .isEqualTo(annotatedSecond);
                    assertThat(annotatedFirst)
                            .as("committed annotated golden for %s", fixtureId)
                            .isEqualTo(Files.readString(fixture.resolve("expected-annotated.java")));
                    String neutral = translateAnnotatedNeutral(cobol,
                            fixture.resolve("translation-input.java"), annotatedSidecar);
                    assertThat(neutral)
                            .as("neutral F2 data-intent bytes for %s", fixtureId)
                            .isEqualTo(annotatedFirst);
                    Path annotatedOutput = compilationOutput.resolve(fixtureId + "-annotated");
                    assertCompiles(annotatedFirst, annotatedOutput);
                    assertBehavior(annotatedOutput, behavior);
                }
            } else {
                assertThat(actionCount).isPositive();
                assertThat(fixture.resolve("expected.java")).doesNotExist();
                assertThat(fixture.resolve("translation-input.java")).doesNotExist();
            }
        }
    }

    @Test
    void defaultF1EnvelopeIsByteCompatibleAcrossAllThirteenFixtures() throws Exception {
        var effective = new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), List.of());
        assertThat(effective.profile()).isEqualTo(MigrationProfiles.defaults());
        assertThat(effective.resolvedDecisions()).hasSize(7);

        for (String fixtureId : FIXTURES) {
            Path fixture = corpus().resolve(fixtureId);
            Map<String, byte[]> baselineFirst = generatedFiles(fixtureId, fixture, false, effective);
            Map<String, byte[]> baselineSecond = generatedFiles(fixtureId, fixture, false, effective);
            Map<String, byte[]> f1First = generatedFiles(fixtureId, fixture, true, effective);
            Map<String, byte[]> f1Second = generatedFiles(fixtureId, fixture, true, effective);

            assertSameFiles(fixtureId + " baseline repeat", baselineFirst, baselineSecond);
            assertSameFiles(fixtureId + " F1 repeat", f1First, f1Second);
            assertSameFiles(fixtureId + " baseline versus default F1", baselineFirst, f1First);
        }
    }

    private Map<String, byte[]> generatedFiles(String fixtureId, Path fixture, boolean f1,
                                                MigrationProfiles.EffectiveProfile effective) throws Exception {
        if (!SUPPORTED.contains(fixtureId)) return Map.of();
        if (f1) {
            assertThat(effective.profileHash()).hasSize(64);
            assertThat(effective.appliedDecisionIds()).isEmpty();
        }
        Map<String, byte[]> generated = new TreeMap<>();
        Path cobol = fixture.resolve("input.cob");
        Path javaStub = fixture.resolve("translation-input.java");
        generated.put("CharacterizationFixture.java", translate(cobol, javaStub).getBytes(StandardCharsets.UTF_8));
        Path annotatedSidecar = fixture.resolve(fixtureId + ".annotated.json");
        if (Files.exists(annotatedSidecar)) {
            generated.put("CharacterizationFixture.annotated.java",
                    translateAnnotated(cobol, javaStub, annotatedSidecar).getBytes(StandardCharsets.UTF_8));
        }
        return generated;
    }

    private static void assertSameFiles(String label, Map<String, byte[]> expected, Map<String, byte[]> actual) {
        assertThat(actual.keySet()).as(label + " file keys").containsExactlyElementsOf(expected.keySet());
        expected.forEach((path, bytes) -> assertThat(actual.get(path)).as(label + " bytes for " + path)
                .containsExactly(bytes));
    }

    private String translate(Path cobol, Path javaStub) throws Exception {
        CobolIntermediateModel model = modelService.parse(Files.readString(cobol));
        return transpiler.enrichServiceImplementation(Files.readString(javaStub), model);
    }

    private String translateAnnotated(Path cobol, Path javaStub, Path sidecar) throws Exception {
        CobolIntermediateModel model = modelService.parse(Files.readString(cobol));
        AnnotatedContextResolver.Resolution resolution = new AnnotatedContextResolver(mapper).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.of(sidecar), cobol), model);
        assertThat(resolution.diagnostics()).as("sidecar diagnostics for %s", sidecar).isEmpty();
        assertThat(resolution.context()).as("sidecar %s must be valid", sidecar).isPresent();
        var ignored = new ArrayList<org.shark.renovatio.provider.cobol.guardrail.ManualActionItem>();
        return transpiler.enrichServiceImplementation(Files.readString(javaStub),
                resolution.context().orElseThrow(), ignored::addAll);
    }

    private String translateAnnotatedNeutral(Path cobol, Path javaStub, Path sidecar) throws Exception {
        byte[] bytes = Files.readAllBytes(cobol);
        CobolIntermediateModel model = modelService.parse(new String(bytes, StandardCharsets.UTF_8));
        AnnotatedContextResolver.Resolution resolution = new AnnotatedContextResolver(mapper).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.of(sidecar), cobol), model);
        assertThat(resolution.diagnostics()).as("sidecar diagnostics for %s", sidecar).isEmpty();
        var semantic = new CobolSemanticProjector().project(model, "input.cob", bytes,
                Optional.empty(), resolution.context());
        var ignored = new ArrayList<org.shark.renovatio.provider.cobol.guardrail.ManualActionItem>();
        return transpiler.enrichServiceImplementation(Files.readString(javaStub),
                resolution.context().orElseThrow(), cobol.toString(), ignored::addAll,
                semantic.dataIntents());
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
