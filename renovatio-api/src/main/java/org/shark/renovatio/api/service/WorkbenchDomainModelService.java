package org.shark.renovatio.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Change;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Comparison;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Diagnostic;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Suggestion;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.SuggestionRequest;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto.Version;
import org.shark.renovatio.api.entity.DomainSuggestionDecisionEntity;
import org.shark.renovatio.api.entity.ProjectDomainModelVersionEntity;
import org.shark.renovatio.api.repository.DomainSuggestionDecisionRepository;
import org.shark.renovatio.api.repository.ProjectDomainModelVersionRepository;
import org.shark.renovatio.api.repository.ProjectRepository;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.DomainModel.BusinessInvariant;
import org.shark.renovatio.domain.model.DomainModel.DomainNode;
import org.shark.renovatio.domain.model.DomainModel.DomainRelation;
import org.shark.renovatio.domain.model.DomainModel.Origin;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkbenchDomainModelService {
    private final ProjectRepository projects;
    private final ProjectDomainModelVersionRepository versions;
    private final DomainSuggestionDecisionRepository decisions;
    private final ObjectMapper json;

    public WorkbenchDomainModelService(ProjectRepository projects,
                                       ProjectDomainModelVersionRepository versions,
                                       DomainSuggestionDecisionRepository decisions,
                                       ObjectMapper json) {
        this.projects = projects;
        this.versions = versions;
        this.decisions = decisions;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public WorkbenchDomainModelDto read(String projectId) {
        requireProject(projectId);
        return view(projectId, latest(projectId));
    }

    @Transactional
    public WorkbenchDomainModelDto save(String projectId, long expectedRevision, DomainModel candidate) {
        requireProject(projectId);
        return saveInternal(projectId, expectedRevision, candidate);
    }

    @Transactional(readOnly = true)
    public List<Version> versions(String projectId) {
        requireProject(projectId);
        return versions.findByProjectIdOrderByRevisionDesc(projectId).stream()
                .map(value -> new Version(value.getRevision(), hash(value.getCanonicalHash()), value.getSavedAt()))
                .toList();
    }

    @Transactional
    public WorkbenchDomainModelDto restore(String projectId, long revision, long expectedRevision) {
        requireProject(projectId);
        ProjectDomainModelVersionEntity selected = versions.findByProjectIdAndRevision(projectId, revision)
                .orElseThrow(() -> new NotFoundException("DomainModel revision " + revision + " was not found"));
        return saveInternal(projectId, expectedRevision, deserialize(selected));
    }

    @Transactional(readOnly = true)
    public Comparison compare(String projectId, long from, long to) {
        requireProject(projectId);
        DomainModel before = deserialize(versions.findByProjectIdAndRevision(projectId, from)
                .orElseThrow(() -> new NotFoundException("DomainModel revision " + from + " was not found")));
        DomainModel after = deserialize(versions.findByProjectIdAndRevision(projectId, to)
                .orElseThrow(() -> new NotFoundException("DomainModel revision " + to + " was not found")));
        List<Change> added = new ArrayList<>();
        List<Change> removed = new ArrayList<>();
        List<Change> changed = new ArrayList<>();
        compare("node", before.nodes(), after.nodes(), DomainNode::id, added, removed, changed);
        compare("relation", before.relations(), after.relations(), DomainRelation::id, added, removed, changed);
        compare("invariant", before.invariants(), after.invariants(), BusinessInvariant::id, added, removed, changed);
        Comparator<Change> order = Comparator.comparing(Change::targetType).thenComparing(Change::targetId);
        added.sort(order); removed.sort(order); changed.sort(order);
        return new Comparison(List.copyOf(added), List.copyOf(removed), List.copyOf(changed));
    }

    @Transactional
    public WorkbenchDomainModelDto decide(String projectId, String suggestionId, SuggestionRequest request) {
        requireProject(projectId);
        String action = request.action() == null ? "" : request.action().toLowerCase(Locale.ROOT);
        if (!List.of("accepted", "edited", "rejected").contains(action)) {
            throw validation("INVALID_SUGGESTION_ACTION", suggestionId,
                    "Suggestion action must be accepted, edited or rejected");
        }
        ProjectDomainModelVersionEntity current = latest(projectId);
        long revision = current == null ? 0 : current.getRevision();
        if (request.expectedRevision() != revision) throw new RevisionConflictException(revision);

        var existing = decisions.findByProjectIdAndSuggestionId(projectId, suggestionId);
        if (existing.isPresent()) {
            if (existing.get().getAction().equals(action)) return view(projectId, current);
            throw new RevisionConflictException(revision);
        }

        DomainModel model = current == null ? empty(projectId) : deserialize(current);
        WorkbenchDomainModelDto result;
        if (suggestionId.startsWith("node:")) {
            result = decideNode(projectId, suggestionId, action, request, model, revision);
        } else if (suggestionId.startsWith("invariant:")) {
            result = decideInvariant(projectId, suggestionId, action, request, model, revision);
        } else {
            throw new NotFoundException("Suggestion was not found");
        }
        long decidedRevision = result.revision();
        decisions.save(new DomainSuggestionDecisionEntity(projectId, suggestionId, action,
                decidedRevision, LocalDateTime.now()));
        return read(projectId);
    }

    private WorkbenchDomainModelDto decideNode(String projectId, String suggestionId, String action,
                                                SuggestionRequest request, DomainModel model, long revision) {
        String nodeId = suggestionId.substring("node:".length());
        DomainNode selected = model.nodes().stream().filter(node -> node.id().equals(nodeId)
                && node.origin() == Origin.LLM).findFirst()
                .orElseThrow(() -> new NotFoundException("Suggestion was not found"));
        if (action.equals("accepted")) return view(projectId, latest(projectId));

        List<DomainNode> nodes = new ArrayList<>(model.nodes());
        if (action.equals("edited")) {
            DomainNode edited = request.editedNode();
            if (edited == null || !selected.id().equals(edited.id())) {
                throw validation("INVALID_EDIT", nodeId, "Edited node must preserve the suggestion id");
            }
            nodes.replaceAll(node -> node.id().equals(nodeId)
                    ? new DomainNode(edited.id(), edited.kind(), edited.name(), edited.properties(),
                    edited.evidence(), Origin.HUMAN, edited.confidence()) : node);
        } else {
            boolean referenced = model.relations().stream().anyMatch(relation -> relation.fromId().equals(nodeId)
                    || relation.toId().equals(nodeId))
                    || model.invariants().stream().anyMatch(invariant -> invariant.subjectId().equals(nodeId));
            if (referenced) throw validation("REFERENCED_NODE", nodeId,
                    "A referenced suggestion cannot be rejected until its relations and invariants are removed");
            nodes.removeIf(node -> node.id().equals(nodeId));
        }
        return saveInternal(projectId, revision, new DomainModel(model.schemaVersion(), model.projectId(),
                nodes, model.relations(), model.invariants()));
    }

    private WorkbenchDomainModelDto decideInvariant(String projectId, String suggestionId, String action,
                                                     SuggestionRequest request, DomainModel model, long revision) {
        String invariantId = suggestionId.substring("invariant:".length());
        BusinessInvariant selected = model.invariants().stream().filter(invariant -> invariant.id().equals(invariantId)
                && invariant.origin() == Origin.LLM).findFirst()
                .orElseThrow(() -> new NotFoundException("Suggestion was not found"));
        if (action.equals("accepted")) return view(projectId, latest(projectId));

        List<BusinessInvariant> invariants = new ArrayList<>(model.invariants());
        if (action.equals("edited")) {
            BusinessInvariant edited = request.editedInvariant();
            if (edited == null || !selected.id().equals(edited.id())) {
                throw validation("INVALID_EDIT", invariantId, "Edited invariant must preserve the suggestion id");
            }
            invariants.replaceAll(invariant -> invariant.id().equals(invariantId)
                    ? new BusinessInvariant(edited.id(), edited.subjectId(), edited.expression(), edited.evidence(),
                    Origin.HUMAN, edited.confidence()) : invariant);
        } else {
            invariants.removeIf(invariant -> invariant.id().equals(invariantId));
        }
        return saveInternal(projectId, revision, new DomainModel(model.schemaVersion(), model.projectId(),
                model.nodes(), model.relations(), invariants));
    }

    private WorkbenchDomainModelDto saveInternal(String projectId, long expectedRevision, DomainModel candidate) {
        ProjectDomainModelVersionEntity current = latest(projectId);
        long currentRevision = current == null ? 0 : current.getRevision();
        if (expectedRevision != currentRevision) throw new RevisionConflictException(currentRevision);
        if (candidate == null || !projectId.equals(candidate.projectId())) {
            throw validation("PROJECT_SCOPE", projectId, "DomainModel projectId must match the request path");
        }
        DomainModel normalized;
        try {
            normalized = new DomainModel(candidate.schemaVersion(), candidate.projectId(), candidate.nodes(),
                    candidate.relations(), candidate.invariants());
        } catch (IllegalArgumentException | NullPointerException error) {
            throw validation("INVALID_MODEL", projectId, error.getMessage());
        }
        if (current != null && current.getCanonicalHash().equals(normalized.canonicalHash())) return view(projectId, current);
        long nextRevision = currentRevision + 1;
        try {
            ProjectDomainModelVersionEntity saved = versions.saveAndFlush(new ProjectDomainModelVersionEntity(
                    projectId, nextRevision, normalized.canonicalHash(), serialize(normalized), LocalDateTime.now()));
            return view(projectId, saved);
        } catch (DataIntegrityViolationException error) {
            long latestRevision = latest(projectId) == null ? currentRevision : latest(projectId).getRevision();
            throw new RevisionConflictException(latestRevision);
        }
    }

    private WorkbenchDomainModelDto view(String projectId, ProjectDomainModelVersionEntity entity) {
        DomainModel model = entity == null ? empty(projectId) : deserialize(entity);
        Map<String, DomainSuggestionDecisionEntity> decided = decisions.findByProjectId(projectId).stream()
                .collect(Collectors.toMap(DomainSuggestionDecisionEntity::getSuggestionId, Function.identity()));
        List<Suggestion> suggestions = new ArrayList<>();
        model.nodes().stream().filter(node -> node.origin() == Origin.LLM).forEach(node ->
                suggestions.add(suggestion("node:" + node.id(), "node", node.id(), node.name(), decided)));
        model.invariants().stream().filter(invariant -> invariant.origin() == Origin.LLM).forEach(invariant ->
                suggestions.add(suggestion("invariant:" + invariant.id(), "invariant", invariant.id(),
                        invariant.expression(), decided)));
        suggestions.sort(Comparator.comparing(Suggestion::id));
        return new WorkbenchDomainModelDto(entity == null ? 0 : entity.getRevision(),
                hash(model.canonicalHash()), entity == null ? null : entity.getSavedAt(), model,
                diagnostics(model), List.copyOf(suggestions));
    }

    private Suggestion suggestion(String id, String targetType, String targetId, String name,
                                  Map<String, DomainSuggestionDecisionEntity> decided) {
        DomainSuggestionDecisionEntity decision = decided.get(id);
        return new Suggestion(id, targetType, targetId, name,
                decision == null ? "pending" : decision.getAction(),
                decision == null ? null : decision.getModelRevision(),
                decision == null ? null : decision.getDecidedAt());
    }

    private List<Diagnostic> diagnostics(DomainModel model) {
        List<Diagnostic> result = new ArrayList<>();
        model.nodes().forEach(node -> {
            if (node.evidence().isEmpty()) result.add(new Diagnostic("warning", "MISSING_EVIDENCE", node.id(),
                    "Domain node has no source evidence"));
            if (node.confidence() < 0.5) result.add(new Diagnostic("warning", "LOW_CONFIDENCE", node.id(),
                    "Domain node confidence is below 50%"));
        });
        model.invariants().forEach(invariant -> {
            if (invariant.evidence().isEmpty()) result.add(new Diagnostic("warning", "MISSING_EVIDENCE", invariant.id(),
                    "Business invariant has no source evidence"));
        });
        return List.copyOf(result);
    }

    private <T> void compare(String type, List<T> beforeValues, List<T> afterValues, Function<T, String> id,
                             List<Change> added, List<Change> removed, List<Change> changed) {
        Map<String, T> before = beforeValues.stream().collect(Collectors.toMap(id, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
        Map<String, T> after = afterValues.stream().collect(Collectors.toMap(id, Function.identity(),
                (left, right) -> left, LinkedHashMap::new));
        after.forEach((key, value) -> {
            if (!before.containsKey(key)) added.add(new Change(type, key, null, value));
            else if (!value.equals(before.get(key))) changed.add(new Change(type, key, before.get(key), value));
        });
        before.forEach((key, value) -> {
            if (!after.containsKey(key)) removed.add(new Change(type, key, value, null));
        });
    }

    private ProjectDomainModelVersionEntity latest(String projectId) {
        return versions.findFirstByProjectIdOrderByRevisionDesc(projectId).orElse(null);
    }

    private DomainModel empty(String projectId) {
        return new DomainModel(DomainModel.SCHEMA_VERSION, projectId, List.of(), List.of(), List.of());
    }

    private String serialize(DomainModel model) {
        try { return json.writeValueAsString(model); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to serialize DomainModel", error); }
    }

    private DomainModel deserialize(ProjectDomainModelVersionEntity entity) {
        try { return json.readValue(entity.getModelJson(), DomainModel.class); }
        catch (JsonProcessingException error) { throw new IllegalStateException("Unable to read DomainModel", error); }
    }

    private String hash(String value) { return "sha256:" + value; }

    private void requireProject(String projectId) {
        if (!projects.existsById(projectId)) throw new NotFoundException("Project was not found");
    }

    private ValidationException validation(String code, String targetId, String message) {
        return new ValidationException(List.of(new Diagnostic("error", code, targetId, message)));
    }

    public static final class RevisionConflictException extends RuntimeException {
        private final long currentRevision;
        public RevisionConflictException(long currentRevision) {
            super("Expected revision does not match the current DomainModel revision");
            this.currentRevision = currentRevision;
        }
        public long currentRevision() { return currentRevision; }
    }

    public static final class ValidationException extends RuntimeException {
        private final List<Diagnostic> diagnostics;
        public ValidationException(List<Diagnostic> diagnostics) {
            super("DomainModel validation failed");
            this.diagnostics = List.copyOf(diagnostics);
        }
        public List<Diagnostic> diagnostics() { return diagnostics; }
    }

    public static final class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }
}
