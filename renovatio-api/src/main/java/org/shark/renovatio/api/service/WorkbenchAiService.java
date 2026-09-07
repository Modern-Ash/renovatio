package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.WorkbenchAiDto;
import org.shark.renovatio.api.dto.WorkbenchArchitectureCanvasDto;
import org.shark.renovatio.api.dto.WorkbenchDomainModelDto;
import org.shark.renovatio.api.dto.WorkbenchShadowImpactDto;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.llm.decision.DecisionSuggestionService;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class WorkbenchAiService {
    private static final String PROMPT_VERSION = "v1";
    private static final List<WorkbenchAiDto.Agent> AGENTS = List.of(
            agent("discovery", "Discovery", "Summarizes selected COBOL/JCL evidence and unresolved parser diagnostics.",
                    "workbench.discovery.v1", List.of("/analyze-program"), List.of("selection", "file", "project")),
            agent("domain-architect", "Domain Architect", "Extracts and explains candidate DomainModel elements with source provenance.",
                    "workbench.domain-architect.v1", List.of("/extract-domain"), List.of("selection", "file", "domainModel")),
            agent("architecture", "Architecture", "Proposes target architecture options from the current DomainModel and profile.",
                    "workbench.architecture.v1", List.of("/propose-architecture"), List.of("project", "domainModel", "architecture")),
            agent("naming", "Naming", "Reviews package, class, and symbol naming consistency without changing generated files.",
                    "workbench.naming.v1", List.of("/propose-architecture"), List.of("domainModel", "architecture")),
            agent("equivalence", "Equivalence", "Explains equivalence gaps and safe next checks from persisted evidence.",
                    "workbench.equivalence.v1", List.of("/run-equivalence"), List.of("project", "shadowImpact", "equivalence")),
            agent("review", "Review", "Surfaces confidence limits, impacted files, and required human approvals before mutation.",
                    "workbench.review.v1", List.of("/analyze-program", "/extract-domain", "/propose-architecture", "/run-equivalence"),
                    List.of("project", "file", "domainModel", "architecture", "shadowImpact")));
    private static final List<WorkbenchAiDto.SlashCommand> COMMANDS = List.of(
            new WorkbenchAiDto.SlashCommand("/analyze-program", "discovery",
                    "Explain the selected COBOL/JCL program, symbols, diagnostics, and unresolved evidence.",
                    "GET /api/projects/{projectId}/workbench/source-explorer", false, false),
            new WorkbenchAiDto.SlashCommand("/extract-domain", "domain-architect",
                    "Draft DomainModel suggestions from selected source evidence for explicit accept/edit/reject review.",
                    "GET /api/projects/{projectId}/workbench/domain-model", false, true),
            new WorkbenchAiDto.SlashCommand("/propose-architecture", "architecture",
                    "Propose target architecture/profile adjustments and impacted artifact paths.",
                    "GET /api/projects/{projectId}/workbench/architecture/canvas", false, true),
            new WorkbenchAiDto.SlashCommand("/run-equivalence", "equivalence",
                    "Review persisted equivalence evidence and identify checks to run outside the AI response.",
                    "GET /api/projects/{projectId}/workbench/equivalence", false, true));

    private final DecisionLayerService decisions;
    private final WorkbenchSourceExplorerService sourceExplorer;
    private final WorkbenchDomainModelService domainModels;
    private final WorkbenchArchitectureCanvasService architectureCanvas;
    private final WorkbenchShadowImpactService shadowImpact;

    public WorkbenchAiService(DecisionLayerService decisions,
                              WorkbenchSourceExplorerService sourceExplorer,
                              WorkbenchDomainModelService domainModels,
                              WorkbenchArchitectureCanvasService architectureCanvas,
                              WorkbenchShadowImpactService shadowImpact) {
        this.decisions = decisions;
        this.sourceExplorer = sourceExplorer;
        this.domainModels = domainModels;
        this.architectureCanvas = architectureCanvas;
        this.shadowImpact = shadowImpact;
    }

    public WorkbenchAiDto summary(String projectId) {
        WorkbenchAiDto.ContextSnapshot context = context(projectId);
        List<DecisionPoint> decisionPoints;
        try {
            decisionPoints = decisions.decisions(projectId, null, null, null);
        } catch (RuntimeException exception) {
            decisionPoints = List.of();
        }
        List<WorkbenchAiDto.Item> items = decisionPoints.stream()
                .map(this::item)
                .toList();
        List<WorkbenchAiDto.AuditEvent> audit = decisionPoints.stream()
                .map(decision -> audit(decision, context.canonicalHash()))
                .toList();
        return new WorkbenchAiDto(AGENTS, prompts(), COMMANDS, context, toolPolicy(), items, audit,
                List.of("AI suggestions are proposals only; final files are never written directly.",
                        "Every mutating tool call requires an explicit human confirmation path.",
                        "Responses are reproducible from prompt id, prompt version, context hash, and recorded tool calls.",
                        "Low-confidence or failed LLM suggestions stay visible with their failure category."));
    }

    private WorkbenchAiDto.Item item(DecisionPoint value) {
        return new WorkbenchAiDto.Item(value.id(), value.category().name(), value.source().name(),
                value.status().name(), value.confidence(), value.evidence().size(), value.llmFailed(),
                promptId(value.category()), PROMPT_VERSION, value.question(), value.chosenOption(),
                value.rationale(), value.evidence(), reviewActions(value), approvalStatus(value));
    }

    private WorkbenchAiDto.AuditEvent audit(DecisionPoint decision, String contextHash) {
        String responseHash = "sha256:" + MigrationProfiles.sha256(MigrationProfiles.canonical(Map.of(
                "decisionId", decision.id(),
                "chosenOption", decision.chosenOption(),
                "confidence", decision.confidence(),
                "rationale", decision.rationale(),
                "llmFailed", decision.llmFailed())));
        return new WorkbenchAiDto.AuditEvent("audit:" + decision.id(), decision.id(), "offline-governed",
                promptId(decision.category()), PROMPT_VERSION, contextHash, responseHash,
                toolCalls(decision.category()), decision.status().name(), true, approvalStatus(decision));
    }

    private static List<String> reviewActions(DecisionPoint decision) {
        return decision.status() == DecisionPoint.Status.CONFIRMED
                ? List.of("view-history")
                : List.of("accept", "edit", "reject");
    }

    private static String approvalStatus(DecisionPoint decision) {
        return decision.status() == DecisionPoint.Status.CONFIRMED ? "approved"
                : decision.status() == DecisionPoint.Status.OVERRIDDEN ? "edited"
                : "pending-human-review";
    }

    private WorkbenchAiDto.ContextSnapshot context(String projectId) {
        List<WorkbenchAiDto.ContextSource> sources = new ArrayList<>();
        sources.add(new WorkbenchAiDto.ContextSource("project", projectId, "sha256:" +
                MigrationProfiles.sha256(MigrationProfiles.canonical(projectId)), 1, "ready"));
        try {
            WorkbenchSourceExplorerDto source = sourceExplorer.summary(projectId);
            sources.add(new WorkbenchAiDto.ContextSource("source", "workbench/source-explorer",
                    hash(source.files()), source.files().size(), source.files().isEmpty() ? "empty" : "ready"));
        } catch (Exception exception) {
            sources.add(new WorkbenchAiDto.ContextSource("source", "workbench/source-explorer", "", 0, "error"));
        }
        try {
            WorkbenchDomainModelDto domain = domainModels.read(projectId);
            sources.add(new WorkbenchAiDto.ContextSource("domainModel", "workbench/domain-model",
                    domain.canonicalHash(), domain.model().nodes().size() + domain.model().invariants().size(), "ready"));
        } catch (Exception exception) {
            sources.add(new WorkbenchAiDto.ContextSource("domainModel", "workbench/domain-model", "", 0, "error"));
        }
        try {
            WorkbenchArchitectureCanvasDto architecture = architectureCanvas.read(projectId);
            sources.add(new WorkbenchAiDto.ContextSource("architecture", "workbench/architecture/canvas",
                    architecture.canonicalHash(), architecture.manifest().size(), "ready"));
        } catch (Exception exception) {
            sources.add(new WorkbenchAiDto.ContextSource("architecture", "workbench/architecture/canvas", "", 0, "error"));
        }
        try {
            WorkbenchShadowImpactDto impact = shadowImpact.summary(projectId);
            sources.add(new WorkbenchAiDto.ContextSource("shadowImpact", "workbench/shadow-impact",
                    impact.canonicalHash(), impact.artifactImpacts().size() + impact.sourceImpacts().size(), "ready"));
        } catch (Exception exception) {
            sources.add(new WorkbenchAiDto.ContextSource("shadowImpact", "workbench/shadow-impact", "", 0, "error"));
        }
        String canonicalHash = "sha256:" + MigrationProfiles.sha256(MigrationProfiles.canonical(sources));
        return new WorkbenchAiDto.ContextSnapshot(projectId, canonicalHash, List.copyOf(sources));
    }

    private static String hash(Object value) {
        return "sha256:" + MigrationProfiles.sha256(MigrationProfiles.canonical(value));
    }

    private static List<WorkbenchAiDto.Prompt> prompts() {
        return AGENTS.stream().map(agent -> new WorkbenchAiDto.Prompt(agent.promptId(), PROMPT_VERSION,
                agent.id(), variables(agent.id()),
                List.of("Return proposals only; do not write files.",
                        "Cite source evidence and affected files when available.",
                        "Expose confidence, uncertainty, and tool-call history.",
                        "Route mutating actions to human accept/edit/reject controls."))).toList();
    }

    private static WorkbenchAiDto.Agent agent(String id, String name, String purpose, String promptId,
                                             List<String> slashCommands, List<String> contextScopes) {
        return new WorkbenchAiDto.Agent(id, name, purpose, promptId, PROMPT_VERSION, slashCommands, contextScopes);
    }

    private static List<String> variables(String agentId) {
        return switch (agentId) {
            case "discovery" -> List.of("projectId", "selectedFile", "sourceHash", "diagnostics");
            case "domain-architect" -> List.of("projectId", "selectedEvidence", "domainHash", "sourceRefs");
            case "architecture", "naming" -> List.of("projectId", "domainHash", "architectureHash", "manifest");
            case "equivalence" -> List.of("projectId", "shadowHash", "equivalenceEvidence", "releaseBlocks");
            default -> List.of("projectId", "contextHash", "suggestionId", "affectedFiles");
        };
    }

    private static WorkbenchAiDto.ToolPolicy toolPolicy() {
        return new WorkbenchAiDto.ToolPolicy(false, true,
                List.of("source-explorer.read", "domain-model.read", "architecture-canvas.read",
                        "shadow-impact.read", "equivalence.read"),
                List.of("domain-suggestion.accept", "domain-suggestion.edit", "domain-suggestion.reject",
                        "architecture-profile.save", "architecture-profile.restore"));
    }

    private static String promptId(DecisionPoint.Category category) {
        return DecisionSuggestionService.promptId(category);
    }

    private static List<String> toolCalls(DecisionPoint.Category category) {
        return switch (category) {
            case ARCHITECTURE, NAMING -> List.of("architecture-canvas.read", "shadow-impact.read");
            case DATA_SHAPE, PERSISTENCE -> List.of("source-explorer.read", "domain-model.read");
            case BATCH -> List.of("source-explorer.read", "equivalence.read");
            default -> List.of("source-explorer.read");
        };
    }
}
