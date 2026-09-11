package org.shark.renovatio.application.capability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Versioned application-owned capability catalog consumed by API, CLI, MCP and Workbench.
 */
public final class SurfaceCapabilityRegistry {
    public static final String CONTRACT_VERSION = "2026-09-11.ac09";
    public static final String DOCUMENT_ID = "renovatio.surface-capabilities";

    private static final List<String> SURFACES = List.of("api", "cli", "mcp", "workbench");
    private static final Set<String> SUPPORTED_OPERATIONS = Set.of(
            "analyze", "plan", "preview", "apply", "diff", "metrics", "reference-pipeline");

    public CapabilityDocument current() {
        return new CapabilityDocument(
                DOCUMENT_ID,
                CONTRACT_VERSION,
                "application",
                Instant.parse("2026-09-11T00:00:00Z"),
                SURFACES,
                List.of(
                        supported("cobol.analyze", "Analyze COBOL workspaces", "analyze", "cobol", "java"),
                        supported("cobol.metrics", "Summarize COBOL migration metrics", "metrics", "cobol", "java"),
                        supported("java.plan", "Plan Java modernization work", "plan", "java", "java"),
                        supported("java.apply", "Apply approved Java modernization work", "apply", "java", "java"),
                        supported("java.diff", "Preview Java modernization diffs", "diff", "java", "java"),
                        supported("reference-pipeline", "Run the verified COBOL-to-Java reference path", "reference-pipeline", "cobol", "java"),
                        preview("node.target", "Generate Node target artifacts", "apply", "cobol", "node"),
                        preview("jcl.batch", "Model JCL batch orchestration", "plan", "jcl", "java"),
                        planned("python.target", "Generate Python target artifacts", "apply", "cobol", "python")
                )
        );
    }

    public Map<String, Object> asMap() {
        return current().toMap();
    }

    public boolean isSupported(String operation) {
        return SUPPORTED_OPERATIONS.contains(operation);
    }

    private static CapabilityDescriptor supported(String id, String summary, String operation, String source, String target) {
        return descriptor(id, summary, operation, source, target, "stable",
                Map.of("api", "supported", "cli", "supported", "mcp", "supported", "workbench", "supported"));
    }

    private static CapabilityDescriptor preview(String id, String summary, String operation, String source, String target) {
        return descriptor(id, summary, operation, source, target, "experimental",
                Map.of("api", "experimental", "cli", "experimental", "mcp", "experimental", "workbench", "planned"));
    }

    private static CapabilityDescriptor planned(String id, String summary, String operation, String source, String target) {
        return descriptor(id, summary, operation, source, target, "planned",
                Map.of("api", "planned", "cli", "planned", "mcp", "planned", "workbench", "planned"));
    }

    private static CapabilityDescriptor descriptor(String id, String summary, String operation, String source,
            String target, String maturity, Map<String, String> surfaces) {
        return new CapabilityDescriptor(id, summary, operation, source, target, maturity,
                List.of("workspacePath", "projectId"), orderedSurfaces(surfaces),
                "project-member",
                List.of("accepted", "planned", "running", "succeeded", "failed", "unavailable"),
                List.of("VALIDATION_FAILED", "CAPABILITY_UNAVAILABLE", "AUTHORIZATION_DENIED", "STALE_INPUT"),
                List.of("sourceTreeHash", "profileHash", "manifestHash", "equivalenceHash"),
                Map.of(
                        "llm", id.contains("ai") ? "experimental" : "planned",
                        "persistence", id.contains("reference") || id.contains("apply") ? "supported" : "planned",
                        "equivalence", id.contains("reference") ? "supported" : "planned"
                ));
    }

    private static Map<String, String> orderedSurfaces(Map<String, String> values) {
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String surface : SURFACES) {
            ordered.put(surface, values.getOrDefault(surface, "planned"));
        }
        return ordered;
    }

    public record CapabilityDocument(String id, String version, String owner, Instant generatedAt,
                                     List<String> surfaces, List<CapabilityDescriptor> capabilities) {
        public Map<String, Object> toMap() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", id);
            value.put("version", version);
            value.put("owner", owner);
            value.put("generatedAt", generatedAt.toString());
            value.put("surfaces", surfaces);
            List<Map<String, Object>> entries = new ArrayList<>();
            for (CapabilityDescriptor capability : capabilities) {
                entries.add(capability.toMap());
            }
            value.put("capabilities", entries);
            return value;
        }
    }

    public record CapabilityDescriptor(String id, String summary, String operation, String sourceLanguage,
                                       String targetLanguage, String maturity, List<String> requiredInputs,
                                       Map<String, String> surfaces, String authorization,
                                       List<String> states, List<String> errors, List<String> manifestHashes,
                                       Map<String, String> runtime) {
        public Map<String, Object> toMap() {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", id);
            value.put("summary", summary);
            value.put("operation", operation);
            value.put("sourceLanguage", sourceLanguage);
            value.put("targetLanguage", targetLanguage);
            value.put("maturity", maturity);
            value.put("requiredInputs", requiredInputs);
            value.put("surfaces", surfaces);
            value.put("authorization", authorization);
            value.put("states", states);
            value.put("errors", errors);
            value.put("manifestHashes", manifestHashes);
            value.put("runtime", runtime);
            return value;
        }
    }
}
