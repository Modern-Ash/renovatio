package org.shark.renovatio.provider.cobol.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CobolSemanticProjectorTest {
    private final CobolIntermediateModelService models = new CobolIntermediateModelService();
    private final CobolSemanticProjector projector = new CobolSemanticProjector();

    @Test
    void projectsAcceptedDataIntentAndNeutralTypesDeterministically() throws Exception {
        Path fixture = fixture("data-intent-redefines");
        Path source = fixture.resolve("input.cob");
        byte[] bytes = Files.readAllBytes(source);
        var model = models.parse(Files.readString(source));
        var resolution = new AnnotatedContextResolver(new ObjectMapper()).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(),
                        Optional.of(fixture.resolve("data-intent-redefines.annotated.json")), source), model);
        assertTrue(resolution.context().isPresent());

        SemanticProgram first = projector.project(model, "fixtures/data-intent-redefines/input.cob", bytes,
                Optional.of("IBM"), resolution.context());
        SemanticProgram second = projector.project(model, "fixtures/data-intent-redefines/input.cob", bytes,
                Optional.of("IBM"), resolution.context());

        assertEquals(first, second);
        assertFalse(first.types().isEmpty());
        assertEquals(1, first.dataIntents().size());
        assertEquals(SemanticProgram.IntentKind.OVERLAPPING_STORAGE,
                first.dataIntents().get(0).intentKind());
        assertEquals(first.dataIntents().get(0).evidenceId(),
                first.dataIntents().get(0).header().semanticRole().substring("data-intent:".length()));
    }

    @Test
    void projectsControlFlowWithoutTargetNames() throws Exception {
        Path source = fixture("move-numeric").resolve("input.cob");
        byte[] bytes = Files.readAllBytes(source);
        var model = models.parse(Files.readString(source));
        SemanticProgram program = projector.project(model, "fixtures/move-numeric/input.cob", bytes,
                Optional.empty(), Optional.empty());
        assertEquals("1", program.schemaVersion());
        assertTrue(program.sourceProvenance().parentEvidenceHashes().size() >= 1);
        assertTrue(program.types().stream().noneMatch(type -> type.symbol().contains("java")));
    }

    private static Path fixture(String id) throws Exception {
        return Path.of(CobolSemanticProjectorTest.class.getResource("/characterization/" + id).toURI());
    }
}
