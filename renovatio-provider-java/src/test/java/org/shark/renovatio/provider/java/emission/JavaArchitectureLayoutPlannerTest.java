package org.shark.renovatio.provider.java.emission;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.architecture.ArchitectureGraph;
import org.shark.renovatio.architecture.ArtifactLayoutPlanner;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.semantic.ir.SourceProvenance;
import org.shark.renovatio.semantic.ir.SourceSpan;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaArchitectureLayoutPlannerTest {
    private static final String REQUEST = "0".repeat(64);
    private static final String MODULE = "1".repeat(64);
    private final JavaArchitectureLayoutPlanner planner = new JavaArchitectureLayoutPlanner();

    @Test
    void preservesLegacyTransactionPathsAndProjectsHexagonalLayers() {
        SemanticProgram program = program("src/PAY-FILE.cob");
        List<ArchitectureGraph.Component> transaction = List.of(
                component("2", ArchitectureGraph.ComponentKind.SERVICE, program),
                component("3", ArchitectureGraph.ComponentKind.ENTITY, program));
        List<ArchitectureGraph.Component> hexagonal = List.of(
                component("4", ArchitectureGraph.ComponentKind.INBOUND_PORT, program),
                component("5", ArchitectureGraph.ComponentKind.USE_CASE, program),
                component("6", ArchitectureGraph.ComponentKind.ENTITY, program));

        assertEquals(MigrationProfile.Language.JAVA, planner.targetLanguage());
        assertEquals(List.of("PayDTO.java", "PayService.java", "PayServiceImpl.java"),
                paths(planner.plan(context(program, MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                        transaction))));
        assertEquals(List.of("modules/payments/domain/model/PayDTO.java",
                        "modules/payments/application/port/in/PayService.java",
                        "modules/payments/application/service/PayServiceImpl.java"),
                paths(planner.plan(context(program, MigrationProfile.ArchitectureStyle.HEXAGONAL, hexagonal))));
    }

    @Test
    void sanitizesFallbackNamesAndRequiresAtLeastOneComponent() {
        SemanticProgram common = program("src/program.cob");
        SemanticProgram numeric = program("src/123.cob");
        var service = component("7", ArchitectureGraph.ComponentKind.SERVICE, common);
        assertTrue(planner.plan(context(common, MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                List.of(service))).get(0).path().endsWith("CobolProgramDTO.java"));
        var numericService = component("8", ArchitectureGraph.ComponentKind.SERVICE, numeric);
        assertTrue(planner.plan(context(numeric, MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                List.of(numericService))).get(0).path().endsWith("Cobol123DTO.java"));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(context(common,
                MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT, List.of())));
    }

    private static ArtifactLayoutPlanner.LayoutContext context(SemanticProgram program,
            MigrationProfile.ArchitectureStyle style, List<ArchitectureGraph.Component> components) {
        return new ArtifactLayoutPlanner.LayoutContext(REQUEST, MODULE, "payments", program, style, components);
    }

    private static ArchitectureGraph.Component component(String digit, ArchitectureGraph.ComponentKind kind,
                                                          SemanticProgram program) {
        return new ArchitectureGraph.Component(digit.repeat(64), MODULE, program.programId(),
                Optional.of(program.header().id()), kind, kind.name());
    }

    private static SemanticProgram program(String path) {
        SourceSpan span = new SourceSpan(path, 1, 1, 2, 1);
        SourceProvenance provenance = new SourceProvenance(path, "9".repeat(64), "COBOL",
                Optional.empty(), List.of());
        return new SemanticProgram("1", SemanticProgram.Header.create("PAY001",
                SemanticProgram.NodeKind.PROGRAM, "program", span), "PAY001", provenance, List.of(), List.of(),
                List.of(), List.of(), new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()),
                List.of());
    }

    private static List<String> paths(List<ArtifactLayoutPlanner.PlannedArtifact> artifacts) {
        return artifacts.stream().map(ArtifactLayoutPlanner.PlannedArtifact::path).toList();
    }
}
