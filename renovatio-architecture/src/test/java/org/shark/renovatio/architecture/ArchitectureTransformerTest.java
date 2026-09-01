package org.shark.renovatio.architecture;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class ArchitectureTransformerTest {
    private final ArchitectureTransformer transformer = new ArchitectureTransformer();

    @Test
    void producesDistinctDeterministicTransactionAndHexagonalGraphs() {
        SemanticProgram program = program(false, true, true);
        ArchitectureResult transaction = transformer.transform(request(program,
                MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT));
        ArchitectureResult hexagonal = transformer.transform(request(program,
                MigrationProfile.ArchitectureStyle.HEXAGONAL));

        assertEquals(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                transaction.programs().get(0).effectiveStyle());
        assertTrue(transaction.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.SERVICE));
        assertFalse(transaction.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.INBOUND_PORT));

        assertEquals(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                hexagonal.programs().get(0).effectiveStyle());
        assertTrue(hexagonal.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.INBOUND_PORT));
        assertTrue(hexagonal.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.OUTBOUND_PORT));
        assertTrue(hexagonal.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.ADAPTER));
        assertNotEquals(transaction.graph(), hexagonal.graph());
        assertEquals(hexagonal, transformer.transform(request(program,
                MigrationProfile.ArchitectureStyle.HEXAGONAL)));
        assertEquals(hexagonal.requestHash(), hexagonal.programs().get(0).targetModel()
                .targetStructure().requestHash());
    }

    @Test
    void fallsBackOnlyTheUnsafeProgramAndKeepsRequestedStyle() {
        SemanticProgram safe = program("SAFE", "src/safe.cob", '1', false, false, false);
        SemanticProgram unsafe = program("UNSAFE", "src/unsafe.cob", '2', true, false, false);
        ArchitectureRequest request = ArchitectureRequest.create(List.of(unsafe, safe),
                ArchitectureFixtures.effective(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                        MigrationProfile.ModuleGrouping.BY_PROGRAM), GroupingConfiguration.empty(), Map.of(), List.of());

        ArchitectureResult result = transformer.transform(request);

        assertEquals(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                result.programs().stream().filter(value -> value.programId().equals("SAFE")).findFirst().orElseThrow()
                        .effectiveStyle());
        var fallback = result.programs().stream().filter(value -> value.programId().equals("UNSAFE"))
                .findFirst().orElseThrow();
        assertEquals(MigrationProfile.ArchitectureStyle.HEXAGONAL, fallback.requestedStyle());
        assertEquals(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT, fallback.effectiveStyle());
        assertEquals(List.of(HexagonalArchitectureProfile.FALLBACK_CODE),
                fallback.targetModel().targetStructure().diagnosticCodes());
        assertEquals(1, result.diagnostics().size());
        assertEquals("UNSAFE", result.diagnostics().get(0).programId());
    }

    @Test
    void leavesUnknownAccessUnresolvedAndDoesNotInventAPort() {
        SemanticProgram program = program(false, false, true);
        ArchitectureResult result = transformer.transform(request(program,
                MigrationProfile.ArchitectureStyle.HEXAGONAL));
        assertTrue(result.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.UNRESOLVED));
        assertFalse(result.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.OUTBOUND_PORT));
        assertFalse(result.graph().components().stream()
                .anyMatch(value -> value.kind() == ArchitectureGraph.ComponentKind.ADAPTER));
    }

    @Test
    void rejectsInactiveAndDuplicateProfiles() {
        var error = assertThrows(ArchitectureTransformer.ArchitectureStyleNotActiveException.class,
                () -> transformer.transform(request(program(false, false, false),
                        MigrationProfile.ArchitectureStyle.LAYERED_MVC)));
        assertEquals("ARCHITECTURE_STYLE_NOT_ACTIVE", error.code());
        assertEquals(MigrationProfile.ArchitectureStyle.LAYERED_MVC, error.requestedStyle());
        assertEquals(List.of(MigrationProfile.ArchitectureStyle.HEXAGONAL,
                MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT), error.activeStyles());
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureTransformer(new ModuleGroupingResolver(),
                List.of(new TransactionScriptArchitectureProfile(), new TransactionScriptArchitectureProfile())));
    }

    @Test
    void carriesTheCanonicalPlannerManifestIntoEachTargetModel() {
        ArtifactLayoutPlanner planner = new ArtifactLayoutPlanner() {
            @Override public MigrationProfile.Language targetLanguage() { return MigrationProfile.Language.JAVA; }
            @Override public List<PlannedArtifact> plan(LayoutContext context) {
                return List.of(new PlannedArtifact("generated/Pay.java", context.components().get(0).id(), "service"));
            }
        };
        ArchitectureResult result = new ArchitectureTransformer(List.of(planner)).transform(request(
                program(false, false, false), MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT));

        assertEquals(List.of("generated/Pay.java"), result.programs().get(0).targetModel()
                .targetStructure().artifactPaths());
        assertEquals(result.manifest().artifacts().stream().map(ArtifactManifest.Artifact::id).toList(),
                result.programs().get(0).artifactIds());
        assertEquals(result.manifest().artifacts().stream().map(ArtifactManifest.Artifact::path).toList(),
                result.programs().get(0).targetModel().targetStructure().artifactPaths());
        assertThrows(IllegalArgumentException.class, () -> new ArchitectureTransformer(List.of(planner, planner)));
    }

    private static ArchitectureRequest request(SemanticProgram program,
                                               MigrationProfile.ArchitectureStyle style) {
        return ArchitectureRequest.create(List.of(program), ArchitectureFixtures.effective(style,
                MigrationProfile.ModuleGrouping.BY_PROGRAM), GroupingConfiguration.empty(), Map.of(), List.of());
    }

    private static SemanticProgram program(boolean unknownFlow, boolean io, boolean unresolved) {
        return program("PAY001", "src/pay.cob", '3', unknownFlow, io, unresolved);
    }

    private static SemanticProgram program(String id, String path, char hash, boolean unknownFlow,
                                           boolean io, boolean unresolved) {
        SourceSpan programSpan = new SourceSpan(path, 1, 1, 10, 9);
        SourceProvenance provenance = new SourceProvenance(path, String.valueOf(hash).repeat(64),
                "COBOL", Optional.empty(), List.of("e".repeat(64)));
        var typeSpan = new SourceSpan(path, 2, 1, 2, 9);
        var type = new SemanticProgram.SemanticType(SemanticProgram.Header.create(id,
                SemanticProgram.NodeKind.TYPE, "record:PAYMENT", typeSpan), "PAYMENT",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());

        List<SemanticProgram.IoOperation> operations = io ? List.of(new SemanticProgram.IoOperation(
                SemanticProgram.Header.create(id, SemanticProgram.NodeKind.IO_OPERATION, "file:READ", 
                        new SourceSpan(path, 4, 1, 4, 9)), SemanticProgram.IoKind.FILE, "READ",
                Optional.of("PAYMENTS"), SemanticProgram.Direction.READ, List.of())) : List.of();
        List<SemanticProgram.UnclassifiedDataAccess> residual = unresolved ? List.of(
                new SemanticProgram.UnclassifiedDataAccess(SemanticProgram.Header.create(id,
                        SemanticProgram.NodeKind.UNCLASSIFIED_DATA_ACCESS, "access:UNKNOWN",
                        new SourceSpan(path, 5, 1, 5, 9)), "UNKNOWN-STORE", "LOOKUP",
                        "insufficient evidence", List.of())) : List.of();

        SemanticProgram.ControlFlow flow = flow(id, path, unknownFlow);
        return new SemanticProgram("1", SemanticProgram.Header.create(id, SemanticProgram.NodeKind.PROGRAM,
                "program", programSpan), id, provenance, List.of(type), List.of(), List.of(), operations,
                flow, residual);
    }

    private static SemanticProgram.ControlFlow flow(String id, String path, boolean unknown) {
        var first = new SemanticProgram.ControlFlowNode(SemanticProgram.Header.create(id,
                SemanticProgram.NodeKind.CONTROL_FLOW_NODE, "paragraph:START",
                new SourceSpan(path, 6, 1, 6, 9)));
        if (!unknown) return new SemanticProgram.ControlFlow(Optional.of(first.header().id()), List.of(first), List.of());
        var second = new SemanticProgram.ControlFlowNode(SemanticProgram.Header.create(id,
                SemanticProgram.NodeKind.CONTROL_FLOW_NODE, "paragraph:END",
                new SourceSpan(path, 7, 1, 7, 9)));
        var edge = new SemanticProgram.ControlFlowEdge(SemanticProgram.Header.create(id,
                SemanticProgram.NodeKind.CONTROL_FLOW_EDGE, "edge:UNKNOWN",
                new SourceSpan(path, 8, 1, 8, 9)), first.header().id(), second.header().id(),
                SemanticProgram.EdgeKind.UNKNOWN);
        return new SemanticProgram.ControlFlow(Optional.of(first.header().id()), List.of(first, second), List.of(edge));
    }
}
