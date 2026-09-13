package org.shark.renovatio.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record WorkbenchAiDto(List<Agent> agents,
                             List<Prompt> prompts,
                             List<SlashCommand> slashCommands,
                             ContextSnapshot context,
                             ToolPolicy toolPolicy,
                             List<Item> items,
                             List<AuditEvent> auditTrail,
                             List<String> limits) {
    public record Agent(String id, String name, String purpose, String promptId, String promptVersion,
                        List<String> slashCommands, List<String> contextScopes) { }
    public record Prompt(String id, String version, String agentId, List<String> variables,
                         List<String> guardrails) { }
    public record SlashCommand(String command, String agentId, String description, String toolCall,
                               boolean mutates, boolean humanConfirmationRequired) { }
    public record ContextSnapshot(String projectId, String canonicalHash, List<ContextSource> sources) { }
    public record ContextSource(String scope, String reference, String hash, int itemCount, String status) { }
    public record ToolPolicy(boolean directFileWritesAllowed, boolean mutationsRequireHumanConfirmation,
                             List<String> allowedReadTools, List<String> mutatingTools) { }
    public record Item(String id, String category, String source, String status, BigDecimal confidence,
                       int evidenceCount, boolean llmFailed, String promptId, String promptVersion,
                       String question, String chosenOption, String rationale, List<String> evidence,
                       List<String> reviewActions, String approvalStatus, String targetType, String targetId,
                       List<String> options, String defaultOption, long revision) { }
    public record AuditEvent(String id, String suggestionId, String model, String promptId, String promptVersion,
                             String contextHash, String responseHash, List<String> toolCalls, String status,
                             boolean approvalRequired, String approvalStatus) { }
}
