package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Minimal-impedance architecture: one service per source program. */
public final class TransactionScriptArchitectureProfile implements ArchitectureProfile {
    @Override
    public MigrationProfile.ArchitectureStyle style() {
        return MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT;
    }

    @Override
    public ProgramResult transform(ProgramContext context) {
        SemanticProgram program = context.program();
        List<ArchitectureGraph.Component> components = new ArrayList<>();
        List<ArchitectureGraph.Relation> relations = new ArrayList<>();
        String serviceId = componentId(context, program.header().id(), "service");
        components.add(new ArchitectureGraph.Component(serviceId, context.moduleId(), program.programId(),
                Optional.of(program.header().id()), ArchitectureGraph.ComponentKind.SERVICE,
                display(program.programId()) + " service"));

        for (SemanticProgram.ControlFlowNode node : program.controlFlow().nodes()) {
            String operationId = componentId(context, node.header().id(), "operation");
            components.add(new ArchitectureGraph.Component(operationId, context.moduleId(), program.programId(),
                    Optional.of(node.header().id()), ArchitectureGraph.ComponentKind.USE_CASE,
                    node.header().semanticRole()));
            relations.add(relation(context, serviceId, operationId, ArchitectureGraph.RelationKind.INVOKES));
        }
        for (SemanticProgram.SemanticType type : program.types()) {
            String valueId = componentId(context, type.header().id(), "model");
            components.add(new ArchitectureGraph.Component(valueId, context.moduleId(), program.programId(),
                    Optional.of(type.header().id()), type.typeKind() == SemanticProgram.TypeKind.GROUP
                    ? ArchitectureGraph.ComponentKind.ENTITY : ArchitectureGraph.ComponentKind.VALUE, type.symbol()));
            relations.add(relation(context, serviceId, valueId, ArchitectureGraph.RelationKind.USES));
        }
        for (SemanticProgram.IoOperation operation : program.ioOperations()) {
            String dependencyId = componentId(context, operation.header().id(), "io-dependency");
            String resource = operation.resourceReference().map(value -> " " + value).orElse("");
            components.add(new ArchitectureGraph.Component(dependencyId, context.moduleId(), program.programId(),
                    Optional.of(operation.header().id()), ArchitectureGraph.ComponentKind.OUTBOUND_PORT,
                    operation.operation() + resource));
            ArchitectureGraph.RelationKind kind = switch (operation.direction()) {
                case READ -> ArchitectureGraph.RelationKind.READS;
                case WRITE -> ArchitectureGraph.RelationKind.WRITES;
                default -> ArchitectureGraph.RelationKind.USES;
            };
            relations.add(relation(context, serviceId, dependencyId, kind));
        }
        for (SemanticProgram.UnclassifiedDataAccess access : program.unclassifiedDataAccesses()) {
            String unresolvedId = componentId(context, access.header().id(), "unresolved");
            components.add(new ArchitectureGraph.Component(unresolvedId, context.moduleId(), program.programId(),
                    Optional.of(access.header().id()), ArchitectureGraph.ComponentKind.UNRESOLVED, access.subject()));
            relations.add(relation(context, serviceId, unresolvedId, ArchitectureGraph.RelationKind.UNKNOWN));
        }
        return new ProgramResult(style(), components, relations, List.of());
    }

    static String componentId(ProgramContext context, String ownerId, String role) {
        return ArchitectureSupport.id(context.requestHash(), "COMPONENT", context.moduleId(), ownerId, role);
    }

    static ArchitectureGraph.Relation relation(ProgramContext context, String from, String to,
                                                ArchitectureGraph.RelationKind kind) {
        return new ArchitectureGraph.Relation(ArchitectureSupport.id(context.requestHash(), "RELATION",
                context.moduleId(), from + ":" + to, kind.name()), from, to, kind);
    }

    private static String display(String value) {
        return value.replace('_', ' ').replace('-', ' ');
    }
}
