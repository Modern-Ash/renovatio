package org.shark.renovatio.domain.model;

import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;

import static org.shark.renovatio.domain.model.DomainModel.*;

/** Pure, conservative projection from Semantic IR to the neutral business model. */
public final class SemanticDomainProjector {
    public DomainModel project(String projectId, List<SemanticProgram> programs) {
        if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId is required");
        List<DomainNode> nodes = new ArrayList<>();
        List<DomainRelation> relations = new ArrayList<>();
        for (SemanticProgram program : programs == null ? List.<SemanticProgram>of() : programs) {
            String aggregateId = "aggregate:" + program.programId();
            String useCaseId = "use-case:" + program.programId();
            Evidence evidence = evidence(program, "program header");
            nodes.add(new DomainNode(aggregateId, Kind.AGGREGATE, program.programId(), List.of(evidence), Origin.DETERMINISTIC, 1.0));
            nodes.add(new DomainNode(useCaseId, Kind.USE_CASE, "Process " + program.programId(), List.of(evidence), Origin.DETERMINISTIC, 0.8));
            relations.add(new DomainRelation("relation:" + aggregateId + ":" + useCaseId,
                    aggregateId, useCaseId, RelationKind.CONTAINS));
            for (SemanticProgram.IoOperation io : program.ioOperations()) {
                String id = "boundary:" + program.programId() + ":" + io.header().id();
                Kind kind = io.ioKind() == SemanticProgram.IoKind.DATABASE || io.ioKind() == SemanticProgram.IoKind.FILE
                        ? Kind.REPOSITORY : Kind.EXTERNAL_SYSTEM;
                nodes.add(new DomainNode(id, kind, io.operation(), List.of(evidence(program, io.operation())),
                        Origin.DETERMINISTIC, 0.7));
                relations.add(new DomainRelation("relation:" + useCaseId + ":" + id, useCaseId, id, RelationKind.USES));
            }
        }
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, nodes, relations, List.of());
    }

    private Evidence evidence(SemanticProgram program, String rationale) {
        return new Evidence(program.sourceProvenance().sourcePath(),
                program.sourceProvenance().contentSha256(), rationale);
    }
}
