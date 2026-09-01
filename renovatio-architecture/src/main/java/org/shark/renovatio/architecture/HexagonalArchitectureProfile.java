package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Source-evidence-only ports-and-adapters transformation. */
public final class HexagonalArchitectureProfile implements ArchitectureProfile {
    public static final String FALLBACK_CODE = "ARCHITECTURE_FALLBACK_UNSAFE_CONTROL_FLOW";
    private final TransactionScriptArchitectureProfile fallback = new TransactionScriptArchitectureProfile();

    @Override
    public MigrationProfile.ArchitectureStyle style() {
        return MigrationProfile.ArchitectureStyle.HEXAGONAL;
    }

    @Override
    public ProgramResult transform(ProgramContext context) {
        if (unsafe(context.program())) {
            ProgramResult transaction = fallback.transform(context);
            var diagnostic = new ArchitectureResult.Diagnostic(FALLBACK_CODE, context.program().programId(),
                    "HEXAGONAL could not be proven from the source control-flow graph; using TRANSACTION_SCRIPT",
                    context.program().sourceProvenance().parentEvidenceHashes());
            return new ProgramResult(MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT,
                    transaction.components(), transaction.relations(), List.of(diagnostic));
        }

        SemanticProgram program = context.program();
        List<ArchitectureGraph.Component> components = new ArrayList<>();
        List<ArchitectureGraph.Relation> relations = new ArrayList<>();
        String inbound = id(context, program.header().id(), "inbound-port");
        String useCase = id(context, program.header().id(), "use-case");
        components.add(component(context, inbound, program.header().id(),
                ArchitectureGraph.ComponentKind.INBOUND_PORT, program.programId() + " inbound port"));
        components.add(component(context, useCase, program.header().id(),
                ArchitectureGraph.ComponentKind.USE_CASE, program.programId() + " use case"));
        relations.add(TransactionScriptArchitectureProfile.relation(context, inbound, useCase,
                ArchitectureGraph.RelationKind.INVOKES));

        for (SemanticProgram.SemanticType type : program.types()) {
            String entity = id(context, type.header().id(), "domain-model");
            components.add(component(context, entity, type.header().id(),
                    type.typeKind() == SemanticProgram.TypeKind.GROUP
                            ? ArchitectureGraph.ComponentKind.ENTITY : ArchitectureGraph.ComponentKind.VALUE,
                    type.symbol()));
            relations.add(TransactionScriptArchitectureProfile.relation(context, useCase, entity,
                    ArchitectureGraph.RelationKind.USES));
        }
        for (SemanticProgram.IoOperation operation : program.ioOperations()) {
            String port = id(context, operation.header().id(), "outbound-port");
            String adapter = id(context, operation.header().id(), "outbound-adapter");
            components.add(component(context, port, operation.header().id(),
                    ArchitectureGraph.ComponentKind.OUTBOUND_PORT, operation.operation() + " port"));
            components.add(component(context, adapter, operation.header().id(),
                    ArchitectureGraph.ComponentKind.ADAPTER, operation.operation() + " adapter"));
            ArchitectureGraph.RelationKind access = switch (operation.direction()) {
                case READ -> ArchitectureGraph.RelationKind.READS;
                case WRITE -> ArchitectureGraph.RelationKind.WRITES;
                default -> ArchitectureGraph.RelationKind.USES;
            };
            relations.add(TransactionScriptArchitectureProfile.relation(context, useCase, port, access));
            relations.add(TransactionScriptArchitectureProfile.relation(context, adapter, port,
                    ArchitectureGraph.RelationKind.IMPLEMENTS));
        }
        for (SemanticProgram.UnclassifiedDataAccess access : program.unclassifiedDataAccesses()) {
            String unresolved = id(context, access.header().id(), "unresolved");
            components.add(component(context, unresolved, access.header().id(),
                    ArchitectureGraph.ComponentKind.UNRESOLVED, access.subject()));
            relations.add(TransactionScriptArchitectureProfile.relation(context, useCase, unresolved,
                    ArchitectureGraph.RelationKind.UNKNOWN));
        }
        return new ProgramResult(style(), components, relations, List.of());
    }

    private static boolean unsafe(SemanticProgram program) {
        return program.controlFlow().edges().stream()
                .anyMatch(value -> value.edgeKind() == SemanticProgram.EdgeKind.UNKNOWN);
    }

    private static String id(ProgramContext context, String ownerId, String role) {
        return TransactionScriptArchitectureProfile.componentId(context, ownerId, role);
    }

    private static ArchitectureGraph.Component component(ProgramContext context, String id, String semanticId,
                                                         ArchitectureGraph.ComponentKind kind, String name) {
        return new ArchitectureGraph.Component(id, context.moduleId(), context.program().programId(),
                Optional.of(semanticId), kind, name);
    }
}
