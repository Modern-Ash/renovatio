package org.shark.renovatio.provider.cobol.translation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolModel;
import org.shark.renovatio.cobol.ir.annotated.AnnotationReview;
import org.shark.renovatio.cobol.ir.annotated.CobolAnnotation;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Test
    void projectsMoveAndComputeDataAccessesAndKeepsUnknownReferencesResidual() {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. ACCESS.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 SOURCE PIC 9(4).
                01 TARGET PIC 9(4).
                PROCEDURE DIVISION.
                MAIN.
                    MOVE SOURCE TO TARGET.
                    COMPUTE TARGET = SOURCE + UNKNOWN-VALUE.
                """;
        var model = models.parse(source);

        SemanticProgram program = projector.project(model, "access.cob", source.getBytes(),
                Optional.empty(), Optional.empty());

        assertTrue(program.sideEffects().stream().anyMatch(effect ->
                effect.effectKind() == SemanticProgram.EffectKind.STATE_READ
                        && effect.description().equals("READ SOURCE")));
        assertTrue(program.sideEffects().stream().anyMatch(effect ->
                effect.effectKind() == SemanticProgram.EffectKind.STATE_WRITE
                        && effect.description().equals("WRITE TARGET")));
        assertTrue(program.unclassifiedDataAccesses().stream().anyMatch(access ->
                access.subject().equals("UNKNOWN-VALUE") && access.observedOperation().equals("READ")));
    }

    @Test
    void provenanceIncludesOnlyAnnotationsThatProducedAcceptedSemanticIntent() throws Exception {
        Path fixture = fixture("data-intent-redefines");
        Path source = fixture.resolve("input.cob");
        byte[] bytes = Files.readAllBytes(source);
        var model = models.parse(Files.readString(source));
        var resolution = new AnnotatedContextResolver(new ObjectMapper()).resolve(
                new AnnotatedContextResolver.Request(Optional.empty(),
                        Optional.of(fixture.resolve("data-intent-redefines.annotated.json")), source), model);
        AnnotatedCobolContext acceptedContext = resolution.context().orElseThrow();
        CobolAnnotation accepted = acceptedContext.sidecar().annotations().get(0);
        String proposedId = "0".repeat(64);
        CobolAnnotation proposed = new CobolAnnotation(proposedId, accepted.nodeId(), accepted.nodeKind(),
                accepted.annotationFamily(), accepted.payload(), accepted.confidence(), accepted.provenance(),
                new AnnotationReview(AnnotationReview.ReviewState.PROPOSED, null, null, null));
        List<CobolAnnotation> annotations = new ArrayList<>(acceptedContext.sidecar().annotations());
        annotations.add(proposed);
        AnnotatedCobolModel sidecar = new AnnotatedCobolModel(acceptedContext.sidecar().schemaVersion(),
                acceptedContext.sidecar().baseIrVersion(), acceptedContext.sidecar().baseIrHash(), annotations);

        SemanticProgram program = projector.project(model, "fixtures/data-intent-redefines/input.cob", bytes,
                Optional.of("IBM"), Optional.of(new AnnotatedCobolContext(model, sidecar)));

        assertTrue(program.sourceProvenance().parentEvidenceHashes().contains(accepted.annotationId()));
        assertFalse(program.sourceProvenance().parentEvidenceHashes().contains(proposedId));
        assertEquals(2, program.sourceProvenance().parentEvidenceHashes().size());
    }

    @Test
    void classifiesDb2DirectionsWithoutTreatingCursorOperationsAsWrites() {
        String source = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SQLDIRECTION.
                PROCEDURE DIVISION.
                MAIN.
                    EXEC SQL SELECT COL FROM TAB END-EXEC.
                    EXEC SQL FETCH CURSOR-A INTO :VALUE-A END-EXEC.
                    EXEC SQL DECLARE CURSOR-A CURSOR FOR SELECT COL FROM TAB END-EXEC.
                    EXEC SQL OPEN CURSOR-A END-EXEC.
                    EXEC SQL CLOSE CURSOR-A END-EXEC.
                    EXEC SQL UPDATE TAB SET COL = 1 END-EXEC.
                """;
        var model = models.parse(source);

        SemanticProgram program = projector.project(model, "sql-direction.cob", source.getBytes(),
                Optional.empty(), Optional.empty());
        Map<String, SemanticProgram.Direction> directions = program.ioOperations().stream()
                .collect(Collectors.toMap(SemanticProgram.IoOperation::operation,
                        SemanticProgram.IoOperation::direction));

        assertEquals(SemanticProgram.Direction.READ, directions.get("SELECT"));
        assertEquals(SemanticProgram.Direction.READ, directions.get("FETCH"));
        assertEquals(SemanticProgram.Direction.UNKNOWN, directions.get("DECLARE"));
        assertEquals(SemanticProgram.Direction.UNKNOWN, directions.get("OPEN"));
        assertEquals(SemanticProgram.Direction.UNKNOWN, directions.get("CLOSE"));
        assertEquals(SemanticProgram.Direction.WRITE, directions.get("UPDATE"));
    }

    private static Path fixture(String id) throws Exception {
        return Path.of(CobolSemanticProjectorTest.class.getResource("/characterization/" + id).toURI());
    }
}
