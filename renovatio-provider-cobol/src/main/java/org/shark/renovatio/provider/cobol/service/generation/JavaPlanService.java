package org.shark.renovatio.provider.cobol.service.generation;

import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for planning Java code generation from COBOL structures.
 * This is the third stage of the generation pipeline.
 */
@Service
public class JavaPlanService {

    private static final Logger log = LoggerFactory.getLogger(JavaPlanService.class);

    /**
     * Create a generation plan for a COBOL program.
     *
     * @param classBase the base class name
     * @param metadata the COBOL program metadata
     * @param fields the projected field definitions
     * @return the generation plan
     */
    public GenerationPlan createPlan(String classBase, Map<String, Object> metadata,
                                     List<JavaProjectService.FieldDefinition> fields) {
        log.debug("Creating generation plan for class: {}", classBase);

        Map<String, Object> options = new HashMap<>();
        options.put("classBase", classBase);
        options.put("fields", fields);
        options.put("metadata", metadata);

        // Determine what needs to be generated
        boolean hasEntryPoints = hasEntryPoints(metadata);
        boolean hasCicsCommands = hasCicsCommands(metadata);

        List<GenerationStep> steps = new java.util.ArrayList<>();
        steps.add(new GenerationStep("generateDTO", "dto", Collections.emptyMap()));
        steps.add(new GenerationStep("generateServiceInterface", "interface", Collections.emptyMap()));
        steps.add(new GenerationStep("generateServiceImpl", "implementation", Collections.emptyMap()));

        if (hasCicsCommands) {
            steps.add(new GenerationStep("generateCicsController", "controller", Collections.emptyMap()));
        }

        return new GenerationPlan("java", null, steps, options);
    }

    /**
     * Check if the program has ENTRY points.
     *
     * @param metadata the COBOL program metadata
     * @return true if ENTRY points exist
     */
    public boolean hasEntryPoints(Map<String, Object> metadata) {
        if (metadata == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) metadata.get("entries");
        return entries != null && !entries.isEmpty();
    }

    /**
     * Check if the program uses CICS commands.
     *
     * @param metadata the COBOL program metadata
     * @return true if CICS commands exist
     */
    public boolean hasCicsCommands(Map<String, Object> metadata) {
        if (metadata == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Set<String> cics = (Set<String>) metadata.get("cicsCommands");
        return cics != null && !cics.isEmpty();
    }

    /**
     * Get CICS commands from metadata.
     *
     * @param metadata the COBOL program metadata
     * @return set of CICS commands, or empty set
     */
    public Set<String> getCicsCommands(Map<String, Object> metadata) {
        if (metadata == null) {
            return Collections.emptySet();
        }

        @SuppressWarnings("unchecked")
        Set<String> cics = (Set<String>) metadata.get("cicsCommands");
        return cics != null ? cics : Collections.emptySet();
    }

    /**
     * Record representing a generation plan.
     */
    public record GenerationPlan(
            String targetLanguage,
            CobolIntermediateModel sourceModel,
            List<GenerationStep> steps,
            Map<String, Object> options
    ) {
    }

    /**
     * Record representing a generation step.
     */
    public record GenerationStep(
            String name,
            String type,
            Map<String, Object> parameters
    ) {
    }
}