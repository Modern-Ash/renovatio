package org.shark.renovatio.domain.model;

import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.shark.renovatio.domain.model.DomainModel.*;

/** Pure, conservative projection from Semantic IR to the neutral business model. */
public final class SemanticDomainProjector {
    private static final Set<String> NON_RESOURCE_OPERATIONS = Set.of(
            "SELECT", "INSERT", "UPDATE", "DELETE", "FETCH", "OPEN", "CLOSE", "SET", "GET",
            "LINK", "RETURN", "ABEND", "ASKTIME", "FORMATTIME", "STARTBR", "READNEXT",
            "ENDBR", "READ", "WRITE", "REWRITE", "MERGE", "DECLARE", "COMMIT", "ROLLBACK",
            "READQ", "WRITEQ", "DELETEQ", "SEND", "RECEIVE", "DEFINE", "ASSIGN"
    );

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
            for (SemanticProgram.SemanticType type : program.types()) {
                String id = "entity:" + program.programId() + ":" + type.header().id();
                Kind kind = type.typeKind() == SemanticProgram.TypeKind.GROUP ? Kind.ENTITY : Kind.VALUE_OBJECT;
                nodes.add(new DomainNode(id, kind, type.symbol(), List.of(evidence(program, type.symbol())),
                        Origin.DETERMINISTIC, 0.9));
                relations.add(new DomainRelation("relation:" + aggregateId + ":" + id,
                        aggregateId, id, RelationKind.CONTAINS));
            }
            for (SemanticProgram.IoOperation io : program.ioOperations()) {
                String id = "boundary:" + program.programId() + ":" + io.header().id();
                String resource = io.resourceReference().orElse("");
                boolean persistentResource = (io.ioKind() == SemanticProgram.IoKind.DATABASE
                        || io.ioKind() == SemanticProgram.IoKind.FILE) && isPhysicalResourceName(resource);
                Kind kind = persistentResource ? Kind.REPOSITORY : Kind.EXTERNAL_SYSTEM;
                String name = io.resourceReference().orElse(io.operation());
                nodes.add(new DomainNode(id, kind, name, List.of(evidence(program, io.operation())),
                        Origin.DETERMINISTIC, persistentResource ? 0.8 : 0.5));
                relations.add(new DomainRelation("relation:" + useCaseId + ":" + id, useCaseId, id, RelationKind.USES));
            }
            for (SemanticProgram.UnclassifiedDataAccess access : program.unclassifiedDataAccesses()) {
                String id = "unresolved:" + program.programId() + ":" + access.header().id();
                nodes.add(new DomainNode(id, Kind.EXTERNAL_SYSTEM, access.subject(),
                        List.of(evidence(program, access.reason())), Origin.DETERMINISTIC, 0.3));
                relations.add(new DomainRelation("relation:" + useCaseId + ":" + id,
                        useCaseId, id, RelationKind.USES));
            }
        }
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, nodes, relations, List.of());
    }

    /** Conservative bridge for serialized provider analysis maps. */
    public DomainModel projectSerialized(String projectId, Map<String, Object> analysis) {
        List<DomainNode> nodes = new ArrayList<>();
        Object raw = analysis == null ? null : analysis.get("programs");
        if (raw instanceof List<?> programs) {
            for (Object value : programs) {
                if (!(value instanceof Map<?, ?> program)) continue;
                Object idValue = program.get("programId");
                if (!(idValue instanceof String id) || id.isBlank()) continue;
                String source = String.valueOf(program.containsKey("sourcePath") ? program.get("sourcePath") : "analysis:" + id);
                String hash = String.valueOf(program.containsKey("contentSha256") ? program.get("contentSha256") : "serialized-analysis");
                Evidence evidence = new Evidence(source, hash, "serialized program identity");
                nodes.add(new DomainNode("aggregate:" + id, Kind.AGGREGATE, id, List.of(evidence), Origin.DETERMINISTIC, 0.6));
                nodes.add(new DomainNode("use-case:" + id, Kind.USE_CASE, "Process " + id, List.of(evidence), Origin.DETERMINISTIC, 0.5));
            }
        }
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, nodes, List.of(), List.of());
    }

    private boolean isPhysicalResourceName(String value) {
        if (value == null || value.isBlank()) return false;
        return !NON_RESOURCE_OPERATIONS.contains(value.strip().toUpperCase());
    }

    private Evidence evidence(SemanticProgram program, String rationale) {
        return new Evidence(program.sourceProvenance().sourcePath(),
                program.sourceProvenance().contentSha256(), rationale);
    }
}
