package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Java-friendly layered profiles used by the editable Architecture Canvas. */
public final class LayeredArchitectureProfile implements ArchitectureProfile {
    private final MigrationProfile.ArchitectureStyle style;

    public LayeredArchitectureProfile(MigrationProfile.ArchitectureStyle style) {
        if (!List.of(MigrationProfile.ArchitectureStyle.LAYERED_MVC,
                MigrationProfile.ArchitectureStyle.LAYERED,
                MigrationProfile.ArchitectureStyle.CLEAN).contains(style)) {
            throw new IllegalArgumentException("unsupported layered architecture style");
        }
        this.style = style;
    }

    @Override
    public MigrationProfile.ArchitectureStyle style() {
        return style;
    }

    @Override
    public ProgramResult transform(ProgramContext context) {
        SemanticProgram program = context.program();
        List<ArchitectureGraph.Component> components = new ArrayList<>();
        List<ArchitectureGraph.Relation> relations = new ArrayList<>();
        String controller = id(context, program.header().id(), "controller");
        String service = id(context, program.header().id(), "service");
        String model = id(context, program.header().id(), "model");
        components.add(component(context, controller, program.header().id(),
                ArchitectureGraph.ComponentKind.ADAPTER, program.programId() + " controller"));
        components.add(component(context, service, program.header().id(),
                ArchitectureGraph.ComponentKind.SERVICE, program.programId() + " service"));
        components.add(component(context, model, program.header().id(),
                ArchitectureGraph.ComponentKind.ENTITY, program.programId() + " model"));
        relations.add(TransactionScriptArchitectureProfile.relation(context, controller, service,
                ArchitectureGraph.RelationKind.INVOKES));
        relations.add(TransactionScriptArchitectureProfile.relation(context, service, model,
                ArchitectureGraph.RelationKind.USES));

        for (SemanticProgram.SemanticType type : program.types()) {
            String entity = id(context, type.header().id(), "model");
            components.add(component(context, entity, type.header().id(),
                    type.typeKind() == SemanticProgram.TypeKind.GROUP
                            ? ArchitectureGraph.ComponentKind.ENTITY : ArchitectureGraph.ComponentKind.VALUE,
                    type.symbol()));
            relations.add(TransactionScriptArchitectureProfile.relation(context, service, entity,
                    ArchitectureGraph.RelationKind.USES));
        }
        for (SemanticProgram.IoOperation operation : program.ioOperations()) {
            String repository = id(context, operation.header().id(), "repository");
            components.add(component(context, repository, operation.header().id(),
                    style == MigrationProfile.ArchitectureStyle.CLEAN
                            ? ArchitectureGraph.ComponentKind.OUTBOUND_PORT : ArchitectureGraph.ComponentKind.ADAPTER,
                    operation.operation() + " repository"));
            ArchitectureGraph.RelationKind relation = switch (operation.direction()) {
                case READ -> ArchitectureGraph.RelationKind.READS;
                case WRITE -> ArchitectureGraph.RelationKind.WRITES;
                default -> ArchitectureGraph.RelationKind.USES;
            };
            relations.add(TransactionScriptArchitectureProfile.relation(context, service, repository, relation));
        }
        return new ProgramResult(style, components, relations, List.of());
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
