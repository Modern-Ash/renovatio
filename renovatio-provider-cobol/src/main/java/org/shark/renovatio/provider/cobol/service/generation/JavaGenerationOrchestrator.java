package org.shark.renovatio.provider.cobol.service.generation;

import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.domain.CobolProgram;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrator service that coordinates the 5-stage generation pipeline.
 * This replaces the monolithic JavaGenerationService.
 */
public class JavaGenerationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(JavaGenerationOrchestrator.class);

    private final JavaParseService parseService;
    private final JavaProjectService projectService;
    private final JavaPlanService planService;
    private final JavaRenderService renderService;
    private final JavaWriteService writeService;
    private final JavaGenerationService generationService;

    public JavaGenerationOrchestrator(JavaParseService parseService,
                                      JavaProjectService projectService,
                                      JavaPlanService planService,
                                      JavaRenderService renderService,
                                      JavaWriteService writeService) {
        this.parseService = parseService;
        this.projectService = projectService;
        this.planService = planService;
        this.renderService = renderService;
        this.writeService = writeService;
        this.generationService = null;
    }

    /** Compatibility bridge for the production path while the stages absorb the advanced emitters. */
    public JavaGenerationOrchestrator(JavaGenerationService generationService) {
        this.parseService = null;
        this.projectService = null;
        this.planService = null;
        this.renderService = null;
        this.writeService = null;
        this.generationService = generationService;
    }

    public StubResult generateInterfaceStubs(NqlQuery query, Workspace workspace,
                                             MigrationProfiles.EffectiveProfile effective,
                                             String expectedManifestHash) {
        return generationService.generateInterfaceStubs(query, workspace, effective, expectedManifestHash);
    }

    /**
     * Generate Java interface stubs from COBOL programs.
     *
     * @param query the NQL query
     * @param workspace the workspace
     * @return the stub generation result
     */
    public StubResult generateInterfaceStubs(NqlQuery query, Workspace workspace) {
        try {
            // Stage 1: Parse
            List<CobolProgram> programs = parseService.parse(query, workspace);

            Map<String, String> generatedFiles = new HashMap<>();

            for (CobolProgram program : programs) {
                Map<String, Object> metadata = parseService.extractMetadata(program);
                String fileName = (String) metadata.get("filePath");
                String baseName = Paths.get(fileName).getFileName().toString();
                String classBase = sanitizeClassName(toPascalCase(baseName));

                log.debug("Processing file: {}, classBase: {}", fileName, classBase);

                try {
                    // Stage 2: Project
                    List<JavaProjectService.FieldDefinition> fields = projectService.projectFields(metadata);
                    CobolIntermediateModel model = projectService.resolveIntermediateModel(metadata);
                    List<Map<String, Object>> entryPoints = projectService.extractEntryPoints(metadata);

                    // Stage 3: Plan
                    JavaPlanService.GenerationPlan plan = planService.createPlan(classBase, metadata, fields);

                    // Stage 4: Render
                    String dtoClass = renderService.renderDto(classBase, fields);
                    generatedFiles.put(classBase + "DTO.java", dtoClass);

                    String serviceInterface = renderService.renderServiceInterface(classBase, entryPoints);
                    generatedFiles.put(classBase + "Service.java", serviceInterface);

                    String serviceImpl = renderService.renderServiceImpl(classBase, entryPoints, fields, model);
                    generatedFiles.put(classBase + "ServiceImpl.java", serviceImpl);

                    if (planService.hasCicsCommands(metadata)) {
                        Set<String> cicsCommands = planService.getCicsCommands(metadata);
                        String controller = renderService.renderCicsController(classBase, cicsCommands);
                        generatedFiles.put(classBase + "CicsController.java", controller);
                    }

                    log.debug("Generated files for classBase: {}", classBase);
                } catch (Exception e) {
                    log.error("Error generating for classBase '{}': {}", classBase, e.getMessage());
                    throw e;
                }
            }

            // Stage 5: Write
            String outputPath = writeService.writeFiles(generatedFiles, workspace);

            log.info("Generated {} Java files in: {}", generatedFiles.size(), outputPath);

            boolean success = !generatedFiles.isEmpty();
            String message = success ?
                    "Generated " + generatedFiles.size() + " Java files in: " + outputPath :
                    "No Java files generated";

            StubResult result = new StubResult(success, message);
            result.setGeneratedCode(generatedFiles);
            return result;
        } catch (Exception e) {
            return new StubResult(false, "Stub generation failed: " + e.getMessage());
        }
    }

    private String sanitizeClassName(String className) {
        if (className == null || className.isEmpty()) {
            return className;
        }

        // Remove invalid characters and ensure valid Java identifier
        String sanitized = className.replaceAll("[^a-zA-Z0-9_]", "");

        // Ensure it starts with a letter
        if (!sanitized.isEmpty() && !Character.isLetter(sanitized.charAt(0))) {
            sanitized = "C" + sanitized;
        }

        return sanitized;
    }

    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // Remove file extension
        int dotIndex = input.lastIndexOf('.');
        if (dotIndex > 0) {
            input = input.substring(0, dotIndex);
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }
}
