package org.shark.renovatio.provider.cobol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.*;
import org.shark.renovatio.architecture.ArchitectureRequest;
import org.shark.renovatio.architecture.ArchitectureResult;
import org.shark.renovatio.architecture.ArchitectureTransformer;
import org.shark.renovatio.architecture.ArtifactLayoutPlanner;
import org.shark.renovatio.architecture.GroupingConfiguration;
import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.core.service.TargetEmitterRegistry;
import org.shark.renovatio.decisions.DecisionResolver;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticProjector;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.provider.cobol.translation.AnnotatedContextResolver;
import org.shark.renovatio.provider.cobol.translation.AnnotationActionItemFactory;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItemWriter;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.profile.EffectiveProfileResolver;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.provider.java.emission.JavaArchitectureLayoutPlanner;
import org.shark.renovatio.provider.java.emission.JavaArchitectureSourceLayout;
import org.shark.renovatio.provider.java.emission.JavaEmitter;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Java code generation service using JavaPoet
 * Generates Java classes, DTOs, and interface stubs from COBOL structures
 */
@Service
public class JavaGenerationService {

    private final CobolParsingService parsingService;
    private final TemplateCodeGenerationService templateService;
    private final CobolIntermediateModelService intermediateModelService;
    private final CobolSemanticTranspiler semanticTranspiler;
    private final AnnotatedContextResolver annotatedContextResolver;
    private final AnnotationActionItemFactory annotationActionItemFactory;
    private final ManualActionItemWriter manualActionItemWriter;
    private final GeneratedArtifactTreeWriter artifactTreeWriter = new GeneratedArtifactTreeWriter();
    private final boolean registryRouting;
    private final TargetEmitterRegistry emitterRegistry;
    private final EffectiveProfileResolver effectiveProfileResolver;
    private final CobolSemanticProjector semanticProjector = new CobolSemanticProjector();
    private final ArchitectureTransformer architectureTransformer;
    private final ArchitectureTransformer architectureTransformerWithoutLayout = new ArchitectureTransformer();

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler,
                new ObjectMapper().findAndRegisterModules(), false, new TargetEmitterRegistry(List.of()), null);
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler, objectMapper, false,
                new TargetEmitterRegistry(List.of()), null);
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper,
                                 boolean registryRouting) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler, objectMapper,
                registryRouting, new TargetEmitterRegistry(List.of()), null);
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper,
                                 boolean registryRouting,
                                 TargetEmitterRegistry emitterRegistry,
                                 EffectiveProfileResolver effectiveProfileResolver) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler, objectMapper,
                registryRouting, emitterRegistry, effectiveProfileResolver,
                List.of(new JavaArchitectureLayoutPlanner()));
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper,
                                 boolean registryRouting,
                                 TargetEmitterRegistry emitterRegistry,
                                 EffectiveProfileResolver effectiveProfileResolver,
                                 List<ArtifactLayoutPlanner> layoutPlanners) {
        this.parsingService = parsingService;
        this.templateService = templateService;
        this.intermediateModelService = intermediateModelService;
        this.semanticTranspiler = semanticTranspiler;
        this.annotatedContextResolver = new AnnotatedContextResolver(objectMapper);
        this.annotationActionItemFactory = new AnnotationActionItemFactory();
        this.manualActionItemWriter = new ManualActionItemWriter(objectMapper);
        this.registryRouting = registryRouting;
        this.emitterRegistry = Objects.requireNonNull(emitterRegistry, "emitterRegistry");
        this.effectiveProfileResolver = effectiveProfileResolver == null
                ? ignored -> defaultEffectiveProfile() : effectiveProfileResolver;
        this.architectureTransformer = new ArchitectureTransformer(
                List.copyOf(Objects.requireNonNull(layoutPlanners, "layoutPlanners")));
    }

    /**
     * Generates Java interface stubs for COBOL programs
     */
    public StubResult generateInterfaceStubs(NqlQuery query, Workspace workspace) {
        if (!registryRouting) {
            return generateInterfaceStubsLegacy(query, workspace);
        }
        return generateInterfaceStubs(query, workspace, effectiveProfile(workspace));
    }

    public MigrationProfiles.EffectiveProfile effectiveProfile(Workspace workspace) {
        String projectId = workspace == null ? null : workspace.getId();
        return Objects.requireNonNull(effectiveProfileResolver.resolve(projectId), "effective profile");
    }

    /**
     * Builds the same canonical architecture result consumed by generation without invoking an emitter or
     * writing generated artifacts to the workspace.
     */
    public ArchitectureResult previewArchitecture(NqlQuery query, Workspace workspace) {
        return previewArchitecture(query, workspace, effectiveProfile(workspace));
    }

    public ArchitectureResult previewArchitecture(NqlQuery query, Workspace workspace,
                                                  MigrationProfiles.EffectiveProfile effective) {
        return prepareArchitecture(query, workspace, effective).architecture();
    }

    /** Routes an effective F1 target envelope through the F2 target registry. */
    public StubResult generateInterfaceStubs(NqlQuery query, Workspace workspace,
                                             MigrationProfiles.EffectiveProfile effective) {
        try {
            Path root = workspaceRoot(workspace);
            ArchitecturePreparation preparation = prepareArchitecture(query, workspace, effective);
            ArchitectureResult architecture = preparation.architecture();
            Map<String, String> generatedFiles = new LinkedHashMap<>();
            Map<String, ManualActionItem> actionItems = new LinkedHashMap<>();
            for (ArchitectureResult.ArchitectedProgram architected : architecture.programs()) {
                Path source = preparation.sourceByProgram().get(architected.programId());
                StubResult emitted = emitProjected(architected.targetModel(),
                        semantic -> generateInterfaceStubsLegacy(query, workspace, semantic, source, false, actionItems));
                if (!emitted.isSuccess()) return emitted;
                if (emitted.getGeneratedCode() != null) {
                    emitted.getGeneratedCode().forEach((path, content) -> putArtifact(generatedFiles, path, content));
                }
            }
            String outputPath = resolveOutputDir(workspace).toString();
            if (effective.profile().target().language() == org.shark.renovatio.profile.MigrationProfile.Language.JAVA) {
                manualActionItemWriter.write(root.resolve(ManualActionItemWriter.DEFAULT_REPORT), actionItems.values());
            }
            boolean explicitOutput = workspace.getMetadata() != null
                    && workspace.getMetadata().get("outputDir") != null
                    && !workspace.getMetadata().get("outputDir").toString().isBlank();
            if (!generatedFiles.isEmpty()
                    && (effective.profile().target().language()
                    == org.shark.renovatio.profile.MigrationProfile.Language.JAVA || explicitOutput)) {
                outputPath = writeGeneratedFilesToDisk(generatedFiles, workspace);
            }
            StubResult result = new StubResult(!generatedFiles.isEmpty(), generatedFiles.isEmpty()
                    ? "No target files generated"
                    : "Generated " + generatedFiles.size() + " target files in: " + outputPath);
            result.setGeneratedCode(generatedFiles);
            result.setTargetLanguage(effective.profile().target().language().name());
            return result;
        } catch (TargetEmitterRegistry.TargetEmitterUnavailableException unavailable) {
            throw unavailable;
        } catch (Exception exception) {
            return new StubResult(false, "Stub generation failed: " + exception.getMessage());
        }
    }

    private ArchitecturePreparation prepareArchitecture(NqlQuery query, Workspace workspace,
                                                        MigrationProfiles.EffectiveProfile effective) {
        try {
            Path root = workspaceRoot(workspace);
            if (!Files.isDirectory(root)) {
                throw new ArchitecturePreviewException("WORKSPACE_NOT_FOUND",
                        "Workspace directory not found or inaccessible: " + root);
            }
            List<Path> sources = parsingService.findCobolSourceFiles(root).stream().sorted().toList();
            if (sources.isEmpty()) {
                throw new ArchitecturePreviewException("COBOL_SOURCE_NOT_FOUND",
                        "No COBOL source files found");
            }
            Map<String, Path> sourceByProgram = new LinkedHashMap<>();
            Map<String, List<String>> copybooksByProgram = new LinkedHashMap<>();
            List<SemanticProgram> semanticPrograms = new ArrayList<>();
            for (Path source : sources) {
                SemanticProgram semantic = semanticProgram(source, query, workspace);
                Path previous = sourceByProgram.putIfAbsent(semantic.programId(), source);
                if (previous != null) {
                    throw new ArchitecturePreviewException("DUPLICATE_SEMANTIC_PROGRAM",
                            "Duplicate semantic program " + semantic.programId());
                }
                semanticPrograms.add(semantic);
                copybooksByProgram.put(semantic.programId(), parsingService.extractCopybookReferences(source));
            }
            ArchitectureResult result = architecture(semanticPrograms,
                    Objects.requireNonNull(effective, "effective profile"), true, copybooksByProgram);
            return new ArchitecturePreparation(result,
                    Collections.unmodifiableMap(new LinkedHashMap<>(sourceByProgram)));
        } catch (ArchitecturePreviewException exception) {
            throw exception;
        } catch (ArchitectureTransformer.ArchitectureStyleNotActiveException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ArchitecturePreviewException("ARCHITECTURE_PREVIEW_FAILED", exception.getMessage(), exception);
        }
    }

    private static Path workspaceRoot(Workspace workspace) {
        if (workspace == null || workspace.getPath() == null || workspace.getPath().isBlank()) {
            throw new ArchitecturePreviewException("WORKSPACE_NOT_FOUND", "Workspace path is required");
        }
        return Paths.get(workspace.getPath()).toAbsolutePath().normalize();
    }

    private record ArchitecturePreparation(ArchitectureResult architecture, Map<String, Path> sourceByProgram) {
        private ArchitecturePreparation {
            Objects.requireNonNull(architecture, "architecture");
            Objects.requireNonNull(sourceByProgram, "sourceByProgram");
        }
    }

    public static final class ArchitecturePreviewException extends IllegalStateException {
        private final String code;

        public ArchitecturePreviewException(String code, String message) {
            super(message);
            this.code = Objects.requireNonNull(code, "code");
        }

        public ArchitecturePreviewException(String code, String message, Throwable cause) {
            super(message, cause);
            this.code = Objects.requireNonNull(code, "code");
        }

        public String code() {
            return code;
        }
    }

    /** Wraps an existing Java-producing route in the F2 target selection boundary. */
    public StubResult emitThroughRegistry(Path source, NqlQuery query, Workspace workspace,
                                          MigrationProfiles.EffectiveProfile effective,
                                          Function<SemanticProgram, StubResult> generation) {
        try {
            SemanticProgram semantic = semanticProgram(source, query, workspace);
            return emitProjected(semantic, effective,
                    Map.of(semantic.programId(), parsingService.extractCopybookReferences(source)), generation);
        } catch (TargetEmitterRegistry.TargetEmitterUnavailableException unavailable) {
            throw unavailable;
        } catch (Exception exception) {
            return new StubResult(false, "Target emission failed: " + exception.getMessage());
        }
    }

    /** Routes a standalone copybook through a data-section-aware semantic projection. */
    public StubResult emitCopybookThroughRegistry(Path source, NqlQuery query, Workspace workspace,
                                                  MigrationProfiles.EffectiveProfile effective,
                                                  Function<SemanticProgram, StubResult> generation) {
        try {
            SemanticProgram semantic = copybookSemanticProgram(source, query, workspace);
            return emitProjected(semantic, effective, Map.of(semantic.programId(), List.of()), generation);
        } catch (TargetEmitterRegistry.TargetEmitterUnavailableException unavailable) {
            throw unavailable;
        } catch (Exception exception) {
            return new StubResult(false, "Target emission failed: " + exception.getMessage());
        }
    }

    private StubResult emitProjected(SemanticProgram semantic, MigrationProfiles.EffectiveProfile effective,
                                     Map<String, List<String>> programCopybooks,
                                     Function<SemanticProgram, StubResult> generation) {
        TargetModel targetModel = architecture(List.of(semantic), effective, false, programCopybooks)
                .programs().get(0).targetModel();
        return emitProjected(targetModel, generation);
    }

    private StubResult emitProjected(TargetModel targetModel, Function<SemanticProgram, StubResult> generation) {
        SemanticProgram semantic = targetModel.semanticProgram();
        AtomicReference<StubResult> resultReference = new AtomicReference<>();
        JavaEmitter javaEmitter = new JavaEmitter((ignoredModel, ignoredProfile) -> {
            StubResult result = generation.apply(semantic);
            resultReference.set(result);
            EmittedArtifacts raw = result.isSuccess() && result.getGeneratedCode() != null
                    ? EmittedArtifacts.fromUtf8(result.getGeneratedCode())
                    : new EmittedArtifacts(List.of());
            return raw;
        });
        EmittedArtifacts emitted = emitterRegistry.emit(targetModel, javaEmitter);
        if (targetModel.targetLanguage() == org.shark.renovatio.profile.MigrationProfile.Language.JAVA) {
            emitted = applyManifest(targetModel, emitted);
        }
        StubResult result = resultReference.get();
        if (result == null) {
            result = new StubResult(!emitted.artifacts().isEmpty(), emitted.artifacts().isEmpty()
                    ? "No target files generated" : "Generated " + emitted.artifacts().size() + " target files");
        }
        if (result.isSuccess()) result.setGeneratedCode(emitted.utf8TextByPath());
        result.setTargetLanguage(targetModel.profile().target().language().name());
        return result;
    }

    static EmittedArtifacts applyManifest(TargetModel targetModel, EmittedArtifacts emitted) {
        List<String> expected = targetModel.targetStructure().artifactPaths();
        if (expected.isEmpty()) return emitted;
        Map<String, String> expectedByFileName = new LinkedHashMap<>();
        for (String path : expected) {
            String fileName = Path.of(path).getFileName().toString();
            if (expectedByFileName.putIfAbsent(fileName, path) != null) {
                throw new TargetManifestMismatchException("manifest contains duplicate Java file name " + fileName);
            }
        }
        Map<String, String> rebased = new LinkedHashMap<>();
        emitted.artifacts().forEach(artifact -> {
            String fileName = Path.of(artifact.path()).getFileName().toString();
            String planned = expectedByFileName.remove(fileName);
            if (planned == null) throw new TargetManifestMismatchException(
                    "unexpected emitted path " + artifact.path());
            rebased.put(planned, artifact.utf8Text());
        });
        if (!expectedByFileName.isEmpty()) throw new TargetManifestMismatchException(
                "missing emitted paths " + expectedByFileName.values());
        return EmittedArtifacts.fromUtf8(JavaArchitectureSourceLayout.align(rebased));
    }

    public static final class TargetManifestMismatchException extends IllegalStateException {
        public static final String CODE = "TARGET_MANIFEST_MISMATCH";

        public TargetManifestMismatchException(String detail) {
            super(CODE + ": " + detail);
        }
    }

    private ArchitectureResult architecture(List<SemanticProgram> programs,
                                            MigrationProfiles.EffectiveProfile effective,
                                            boolean standardJavaLayout) {
        return architecture(programs, effective, standardJavaLayout, Map.of());
    }

    private ArchitectureResult architecture(List<SemanticProgram> programs,
                                            MigrationProfiles.EffectiveProfile effective,
                                            boolean standardJavaLayout,
                                            Map<String, List<String>> programCopybooks) {
        GroupingConfiguration grouping = GroupingConfiguration.fromExtensions(effective.profile().extensions());
        List<String> evidence = programs.stream()
                .flatMap(value -> value.sourceProvenance().parentEvidenceHashes().stream())
                .distinct().sorted().toList();
        ArchitectureTransformer transformer = standardJavaLayout
                ? architectureTransformer : architectureTransformerWithoutLayout;
        return transformer.transform(ArchitectureRequest.create(programs, effective, grouping,
                programCopybooks, evidence));
    }

    public MigrationProfiles.EffectiveProfile defaultEffectiveProfile() {
        return new DecisionResolver().resolve(MigrationProfiles.emptyOverlay(), List.of());
    }

    private StubResult generateInterfaceStubsLegacy(NqlQuery query, Workspace workspace) {
        return generateInterfaceStubsLegacy(query, workspace, null, null, true, null);
    }

    private StubResult generateInterfaceStubsLegacy(NqlQuery query, Workspace workspace,
                                                     SemanticProgram semanticProgram) {
        return generateInterfaceStubsLegacy(query, workspace, semanticProgram, null, true, null);
    }

    private StubResult generateInterfaceStubsLegacy(NqlQuery query, Workspace workspace,
                                                     SemanticProgram semanticProgram, Path selectedSource,
                                                     boolean persist,
                                                     Map<String, ManualActionItem> collectedActionItems) {
        try {
            // Parse COBOL programs first
            var analyzeResult = parsingService.analyzeCOBOL(query, workspace);
            if (!analyzeResult.isSuccess()) {
                return new StubResult(false, "Failed to analyze COBOL: " + analyzeResult.getMessage());
            }

            @SuppressWarnings("unchecked")
            List<org.shark.renovatio.provider.cobol.domain.CobolProgram> programs = (List<org.shark.renovatio.provider.cobol.domain.CobolProgram>)
                    ((Map<String, Object>) analyzeResult.getData()).get("programs");

            Map<String, String> generatedFiles = new LinkedHashMap<>();
            Map<String, ManualActionItem> actionItems = new LinkedHashMap<>();

            for (org.shark.renovatio.provider.cobol.domain.CobolProgram program : programs) {
                Map<String, Object> metadata = program.getMetadata();
                String fileName = (String) metadata.get("filePath");
                Path cobolPath = Path.of(fileName).toAbsolutePath().normalize();
                if (selectedSource != null && !selectedSource.toAbsolutePath().normalize().equals(cobolPath)) continue;
                String baseName = cobolPath.getFileName().toString();
                // Clean and sanitize the class base name
                String classBase = sanitizeClassName(toPascalCase(baseName));

                System.out.println("DEBUG: Processing file: " + fileName + ", baseName: " + baseName);
                System.out.println("DEBUG: Generated classBase: " + classBase);

                try {
                    CobolIntermediateModel model = resolveIntermediateModel(metadata);
                    SemanticProgram currentSemantic = semanticProgram;
                    AnnotatedContextResolver.Resolution annotatedResolution = annotatedContextResolver.resolve(
                            new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cobolPath), model);
                    annotatedResolution.diagnostics().stream()
                            .map(diagnostic -> annotationActionItemFactory.toResolutionDiagnostic(
                                    diagnostic, fileName, model.getProgramId()))
                            .forEach(item -> actionItems.putIfAbsent(item.id(), item));

                    // Generate DTO class for data structures
                    String dtoClass = generateDataTransferObject(classBase, metadata);
                    if (annotatedResolution.context().isPresent()) {
                        dtoClass = semanticTranspiler.enrichServiceImplementation(dtoClass,
                                annotatedResolution.context().orElseThrow(),
                                fileName,
                                items -> items.forEach(item -> actionItems.putIfAbsent(item.id(), item)),
                                currentSemantic == null ? null : currentSemantic.dataIntents());
                    }
                    putArtifact(generatedFiles, classBase + "DTO.java", dtoClass);
                    // Generate service interface
                    String serviceInterface = generateServiceInterface(classBase, metadata);
                    putArtifact(generatedFiles, classBase + "Service.java", serviceInterface);
                    // Generate implementation template
                    String serviceImpl = generateServiceImplementation(classBase, metadata);
                    if (annotatedResolution.context().isPresent()) {
                        serviceImpl = semanticTranspiler.enrichServiceImplementation(serviceImpl,
                                annotatedResolution.context().orElseThrow(),
                                fileName,
                                items -> items.forEach(item -> actionItems.putIfAbsent(item.id(), item)),
                                currentSemantic == null ? null : currentSemantic.dataIntents());
                    } else {
                        serviceImpl = semanticTranspiler.enrichServiceImplementation(serviceImpl, model);
                    }
                    // DEBUG: print generated service implementation for verification
                    System.out.println("Generated Service Implementation (" + classBase + "):\n" + serviceImpl);
                    putArtifact(generatedFiles, classBase + "ServiceImpl.java", serviceImpl);

                    @SuppressWarnings("unchecked")
                    Set<String> cics = (Set<String>) metadata.get("cicsCommands");
                    if (cics != null && !cics.isEmpty()) {
                        Map<String, Object> tmplData = new HashMap<>();
                        tmplData.put("className", classBase + "CicsController");
                        tmplData.put("transactions", cics);
                        String controller = templateService.generateCicsController(tmplData);
                        putArtifact(generatedFiles, classBase + "CicsController.java", controller);
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Error generating for classBase '" + classBase + "': " + e.getMessage());
                    throw e;
                }
            }

            if (collectedActionItems != null) {
                actionItems.forEach(collectedActionItems::putIfAbsent);
            }
            if (persist) manualActionItemWriter.write(Paths.get(workspace.getPath())
                    .resolve(ManualActionItemWriter.DEFAULT_REPORT), actionItems.values());

            // Write generated files to disk
            String outputPath = persist ? writeGeneratedFilesToDisk(generatedFiles, workspace)
                    : resolveOutputDir(workspace).toString();

            // Debug: print generated keys
            System.out.println("Claves generadas: " + generatedFiles.keySet());
            System.out.println("Archivos escritos en: " + outputPath);

            boolean success = !generatedFiles.isEmpty();
            String message = success ?
                    "Generated " + generatedFiles.size() + " Java files in: " + outputPath :
                    "No Java files generated";

            StubResult result = new StubResult(success, message);
            result.setGeneratedCode(generatedFiles);
            java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
            metadata.put("outputPath", outputPath);
            metadata.put("generatedFileCount", generatedFiles.size());
            metadata.put("generatedFiles", generatedFiles.keySet().stream().sorted().toList());
            result.setMetadata(metadata);
            return result;
        } catch (Exception e) {
            return new StubResult(false, "Stub generation failed: " + e.getMessage());
        }
    }

    private static void putArtifact(Map<String, String> artifacts, String path, String content) {
        String existing = artifacts.putIfAbsent(path, content);
        if (existing != null && !existing.equals(content)) {
            throw new IllegalArgumentException("duplicate artifact path: " + path);
        }
    }

    private SemanticProgram semanticProgram(Path source, NqlQuery query, Workspace workspace) throws Exception {
        Path root = Paths.get(workspace.getPath()).toAbsolutePath().normalize();
        Path normalizedSource = source.toAbsolutePath().normalize();
        String relative = root.relativize(normalizedSource).toString().replace('\\', '/');
        byte[] bytes = Files.readAllBytes(normalizedSource);
        CobolIntermediateModel model = intermediateModelService.parse(normalizedSource);
        AnnotatedContextResolver.Resolution annotated = annotatedContextResolver.resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), normalizedSource), model);
        return semanticProjector.project(model, relative, bytes,
                Optional.ofNullable(resolveDialect(query, workspace)), annotated.context());
    }

    /**
     * Projects every COBOL source in a workspace into the target-neutral semantic IR.
     * This is intentionally exposed as a read-only analysis boundary so API consumers
     * can reuse the same semantic programs that generation uses.
     */
    public List<SemanticProgram> semanticPrograms(NqlQuery query, Workspace workspace) throws Exception {
        Path root = workspaceRoot(workspace);
        List<SemanticProgram> result = new ArrayList<>();
        for (Path source : parsingService.findCobolSourceFiles(root).stream().sorted().toList()) {
            result.add(semanticProgram(source, query, workspace));
        }
        return List.copyOf(result);
    }

    private SemanticProgram copybookSemanticProgram(Path source, NqlQuery query, Workspace workspace) throws Exception {
        Path root = Paths.get(workspace.getPath()).toAbsolutePath().normalize();
        Path normalizedSource = source.toAbsolutePath().normalize();
        String relative = root.relativize(normalizedSource).toString().replace('\\', '/');
        byte[] bytes = Files.readAllBytes(normalizedSource);
        String copybook = new String(bytes, StandardCharsets.UTF_8);
        String programId = normalizedSource.getFileName().toString().replaceFirst("\\.[^.]+$", "")
                .replaceAll("[^A-Za-z0-9-]", "-");
        String projectionSource = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. %s.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                %s
                PROCEDURE DIVISION.
                COPYBOOK-PROJECTION.
                    STOP RUN.
                """.formatted(programId, copybook);
        CobolIntermediateModel model = intermediateModelService.parse(projectionSource);
        AnnotatedContextResolver.Resolution annotated = annotatedContextResolver.resolve(
                new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), normalizedSource), model);
        return semanticProjector.project(model, relative, bytes,
                Optional.ofNullable(resolveDialect(query, workspace)), annotated.context());
    }

    private String resolveDialect(NqlQuery query, Workspace workspace) {
        Object value = query != null && query.getParameters() != null ? query.getParameters().get("dialect") : null;
        if (value == null && workspace != null && workspace.getMetadata() != null) {
            value = workspace.getMetadata().get("dialect");
        }
        return value == null ? null : value.toString();
    }

    /**
     * Generates a Java DTO class from COBOL data structures
     */
    private String generateDataTransferObject(String cleanClassName, Map<String, Object> programData) {
        // Asegurar que el cleanClassName esté completamente limpio
        String sanitizedClassName = sanitizeClassName(cleanClassName);
        String className = sanitizedClassName + "DTO";

        System.out.println("DEBUG: generateDataTransferObject - original: '" + cleanClassName + "', sanitized: '" + sanitizedClassName + "', final: '" + className + "'");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Data Transfer Object generated from COBOL program: $L\n", sanitizedClassName);

        // Add default constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build());

        // Check if there are ENTRY points - if so, use linkageItems instead of dataItems
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        List<Map<String, Object>> dataItems;
        
        if (entries != null && !entries.isEmpty()) {
            // Use linkage section items for programs with ENTRY points
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> linkageItems = (List<Map<String, Object>>) programData.get("linkageItems");
            dataItems = linkageItems != null ? linkageItems : new java.util.ArrayList<>();
            System.out.println("DEBUG: Using linkageItems for DTO generation, count: " + dataItems.size());
        } else {
            // Use working-storage items for regular programs
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> wsItems = (List<Map<String, Object>>) programData.get("dataItems");
            dataItems = wsItems != null ? wsItems : new java.util.ArrayList<>();
            System.out.println("DEBUG: Using dataItems for DTO generation, count: " + dataItems.size());
        }

        if (dataItems != null) {
            for (Map<String, Object> item : dataItems) {
                String fieldName = (String) item.get("name");
                String javaType = (String) item.get("javaType");

                if (fieldName != null && javaType != null) {
                    addFieldToClass(classBuilder, fieldName, javaType);
                }
            }
        }

        TypeSpec classSpec = classBuilder.build();

        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", classSpec)
                .build();

        return javaFile.toString();
    }

    /**
     * Generates a service interface for COBOL program functionality
     */
    private String generateServiceInterface(String cleanClassName, Map<String, Object> programData) {
        // Asegurar que el cleanClassName esté completamente limpio
        String sanitizedClassName = sanitizeClassName(cleanClassName);
        String interfaceName = sanitizedClassName + "Service";
        String dtoName = sanitizedClassName + "DTO";

        System.out.println("DEBUG: generateServiceInterface - original: '" + cleanClassName + "', sanitized: '" + sanitizedClassName + "'");

        ClassName dtoClass = ClassName.get("org.shark.renovatio.generated.cobol", dtoName);

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Service interface for COBOL program: $L\n", sanitizedClassName);

        // Check if there are ENTRY points
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        
        if (entries != null && !entries.isEmpty()) {
            // Generate a method for each ENTRY point
            for (Map<String, Object> entry : entries) {
                String entryName = (String) entry.get("name");
                if (entryName != null && !entryName.isEmpty()) {
                    String methodName = toCamelCase(entryName);
                    MethodSpec entryMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(dtoClass, "input")
                            .returns(dtoClass)
                            .addJavadoc("COBOL ENTRY point: $L\n", entryName)
                            .addJavadoc("@param input Input data structure\n")
                            .addJavadoc("@return Processed output data structure\n")
                            .build();
                    interfaceBuilder.addMethod(entryMethod);
                }
            }
            // Always include a default process method as a generic entry point
            MethodSpec processMethod = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(dtoClass, "input")
                    .returns(dtoClass)
                    .addJavadoc("Process the COBOL program logic with given input\n")
                    .addJavadoc("@param input Input data structure\n")
                    .addJavadoc("@return Processed output data structure\n")
                    .build();
            interfaceBuilder.addMethod(processMethod);
        } else {
            // Add default process method if no ENTRY points
            MethodSpec processMethod = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                    .addParameter(dtoClass, "input")
                    .returns(dtoClass)
                    .addJavadoc("Process the COBOL program logic with given input\n")
                    .addJavadoc("@param input Input data structure\n")
                    .addJavadoc("@return Processed output data structure\n")
                    .build();
            interfaceBuilder.addMethod(processMethod);
        }

        // Add validation method
        MethodSpec validateMethod = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoClass, "input")
                .returns(boolean.class)
                .addJavadoc("Validate input data structure\n")
                .addJavadoc("@param input Input data to validate\n")
                .addJavadoc("@return true if valid, false otherwise\n")
                .build();

        interfaceBuilder.addMethod(validateMethod);

        TypeSpec interfaceSpec = interfaceBuilder.build();

        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", interfaceSpec)
                .build();

        return javaFile.toString();
    }

    /**
     * Generates a service implementation template
     */
    private String generateServiceImplementation(String cleanClassName, Map<String, Object> programData) {
        // Asegurar que el cleanClassName esté completamente limpio
        String sanitizedClassName = sanitizeClassName(cleanClassName);
        String className = sanitizedClassName + "ServiceImpl";
        String interfaceName = sanitizedClassName + "Service";
        String dtoName = sanitizedClassName + "DTO";

        System.out.println("DEBUG: generateServiceImplementation - original: '" + cleanClassName + "', sanitized: '" + sanitizedClassName + "'");

        ClassName interfaceClass = ClassName.get("org.shark.renovatio.generated.cobol", interfaceName);
        ClassName dtoClass = ClassName.get("org.shark.renovatio.generated.cobol", dtoName);
        CobolIntermediateModel model = resolveIntermediateModel(programData);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceClass)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addJavadoc("Implementation of $L\n", interfaceName)
                .addJavadoc("Generated from COBOL program: $L\n", sanitizedClassName);

        // Check if there are ENTRY points
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        
        if (entries != null && !entries.isEmpty()) {
            // Generate implementation for each ENTRY point
            for (Map<String, Object> entry : entries) {
                String entryName = (String) entry.get("name");
                if (entryName != null && !entryName.isEmpty()) {
                    String methodName = toCamelCase(entryName);
                    MethodSpec entryMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addParameter(dtoClass, "input")
                            .returns(dtoClass)
                            .addStatement("// TODO: Implement COBOL business logic for ENTRY $L", entryName)
                            .addStatement("$T out = new $T()", dtoClass, dtoClass)
                            .addStatement("// Placeholder setter to be replaced by semantic transpiler if available")
                            .addStatement("out.setResult(null)")
                            .addStatement("return out")
                            .build();
                    classBuilder.addMethod(entryMethod);
                }
            }
        } else {
            // Add default process method if no ENTRY points
            MethodSpec processMethod = MethodSpec.methodBuilder("process")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class)
                    .addParameter(dtoClass, "input")
                    .returns(dtoClass)
                    .addStatement("// TODO: Implement COBOL business logic")
                    .addStatement("// Original COBOL program: $L", cleanClassName)
                    .addStatement("$T output = new $T()", dtoClass, dtoClass)
                    .addStatement("return output")
                    .build();
            classBuilder.addMethod(processMethod);
        }

        // Implement validate method
        MethodSpec validateMethod = buildValidateMethod(dtoClass, programData, model);

        classBuilder.addMethod(validateMethod);

        TypeSpec classSpec = classBuilder.build();

        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", classSpec)
                .build();

        return javaFile.toString();
    }

    private MethodSpec buildValidateMethod(ClassName dtoClass,
                                           Map<String, Object> programData,
                                           CobolIntermediateModel model) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoClass, "input")
                .returns(boolean.class);

        methodBuilder.addStatement("if (input == null) { return false; }");

        List<FieldValidation> validations = buildFieldValidations(programData, model);
        if (validations.isEmpty()) {
            methodBuilder.addStatement("return true");
            return methodBuilder.build();
        }

        for (FieldValidation field : validations) {
            addValidationStatements(methodBuilder, field);
        }

        methodBuilder.addStatement("return true");
        return methodBuilder.build();
    }

    private void addValidationStatements(MethodSpec.Builder methodBuilder, FieldValidation field) {
        String accessor = "input." + getterName(field.fieldName) + "()";
        switch (field.javaType) {
            case "String" -> addStringValidation(methodBuilder, field, accessor);
            case "Integer", "Long" -> addIntegerValidation(methodBuilder, field, accessor);
            case "BigDecimal" -> addBigDecimalValidation(methodBuilder, field, accessor);
            default -> methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        }
    }

    private void addStringValidation(MethodSpec.Builder methodBuilder, FieldValidation field, String accessor) {
        CodeBlock.Builder condition = CodeBlock.builder();
        condition.add("$L == null", accessor);
        if (field.maxLength != null && field.maxLength > 0) {
            condition.add(" || $L.length() > $L", accessor, field.maxLength);
        } else {
            condition.add(" || $L.isBlank()", accessor);
        }
        methodBuilder.addStatement("if ($L) { return false; }", condition.build());
    }

    private void addIntegerValidation(MethodSpec.Builder methodBuilder, FieldValidation field, String accessor) {
        methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        if (!field.allowsNegative) {
            methodBuilder.addStatement("if ($L < 0) { return false; }", accessor);
        }
        if (field.precision != null && field.precision > 0) {
            int scale = field.scale != null ? field.scale : 0;
            int digits = field.precision - scale;
            if (digits > 0) {
                methodBuilder.addStatement(
                        "if (String.valueOf(Math.abs($L)).length() > $L) { return false; }",
                        accessor,
                        digits);
            }
        }
    }

    private void addBigDecimalValidation(MethodSpec.Builder methodBuilder, FieldValidation field, String accessor) {
        methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        if (!field.allowsNegative) {
            methodBuilder.addStatement("if ($L.signum() < 0) { return false; }", accessor);
        }
        if (field.scale != null) {
            methodBuilder.addStatement("if ($L.scale() > $L) { return false; }", accessor, field.scale);
        }
        if (field.precision != null) {
            methodBuilder.addStatement("if ($L.precision() > $L) { return false; }", accessor, field.precision);
            if (field.scale != null) {
                int integerDigits = field.precision - field.scale;
                if (integerDigits > 0) {
                    methodBuilder.addStatement(
                            "if ($L.precision() - $L.scale() > $L) { return false; }",
                            accessor,
                            accessor,
                            integerDigits);
                }
            }
        }
    }

    private List<FieldValidation> buildFieldValidations(Map<String, Object> programData, CobolIntermediateModel model) {
        Map<String, FieldValidation> validations = new LinkedHashMap<>();

        for (Map<String, Object> field : resolveDtoFields(programData)) {
            String name = field != null ? (String) field.get("name") : null;
            if (name == null || name.isBlank()) {
                continue;
            }
            String javaType = field.get("javaType") != null ? field.get("javaType").toString() : "String";
            validations.putIfAbsent(name, new FieldValidation(name, javaType));
        }

        if (model != null) {
            for (CobolDataItem item : model.getDataItems()) {
                if (item == null) {
                    continue;
                }
                String camel = toCamelCase(item.getName());
                FieldValidation validation = validations.get(camel);
                if (validation != null) {
                    validation.applyPicture(item.getPicture());
                    if (item.getJavaType() != null) {
                        validation.javaType = item.getJavaType();
                    }
                }
            }
        }

        return new ArrayList<>(validations.values());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveDtoFields(Map<String, Object> programData) {
        if (programData == null) {
            return List.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        if (entries != null && !entries.isEmpty()) {
            List<Map<String, Object>> linkage = (List<Map<String, Object>>) programData.get("linkageItems");
            return linkage != null ? linkage : List.of();
        }
        List<Map<String, Object>> dataItems = (List<Map<String, Object>>) programData.get("dataItems");
        return dataItems != null ? dataItems : List.of();
    }

    private String getterName(String fieldName) {
        String cleaned = fieldName == null ? "" : fieldName.trim();
        if (cleaned.isEmpty()) {
            return "get";
        }
        return "get" + capitalizeForAccessor(cleaned);
    }

    private String capitalizeForAccessor(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static final class FieldValidation {
        private final String fieldName;
        private String javaType;
        private Integer maxLength;
        private Integer precision;
        private Integer scale;
        private boolean allowsNegative = true;

        private FieldValidation(String fieldName, String javaType) {
            this.fieldName = fieldName;
            this.javaType = javaType == null ? "String" : javaType;
        }

        private void applyPicture(String picture) {
            if (picture == null || picture.isBlank()) {
                return;
            }
            String normalized = picture.toUpperCase(Locale.ROOT).replace(" ", "");
            int compIndex = normalized.indexOf("COMP");
            if (compIndex >= 0) {
                normalized = normalized.substring(0, compIndex);
            }

            if (normalized.startsWith("X") || normalized.startsWith("A")) {
                Integer length = extractLength(normalized);
                if (length != null) {
                    this.maxLength = length;
                }
                return;
            }

            if (normalized.contains("9")) {
                this.allowsNegative = normalized.contains("S");
                int vIndex = normalized.indexOf('V');
                String before = vIndex >= 0 ? normalized.substring(0, vIndex) : normalized;
                String after = vIndex >= 0 ? normalized.substring(vIndex + 1) : "";
                before = before.replace("S", "");
                after = after.replace("S", "");
                int beforeDigits = countSymbol(before, '9');
                int afterDigits = countSymbol(after, '9');
                int totalDigits = beforeDigits + afterDigits;
                if (totalDigits > 0) {
                    this.precision = totalDigits;
                }
                if (afterDigits > 0) {
                    this.scale = afterDigits;
                }
            }
        }

        private Integer extractLength(String pattern) {
            Matcher grouped = Pattern.compile("[XA]\\((\\d+)\\)").matcher(pattern);
            if (grouped.find()) {
                return Integer.parseInt(grouped.group(1));
            }
            long simple = pattern.chars().filter(ch -> ch == 'X' || ch == 'A').count();
            return simple > 0 ? (int) simple : null;
        }

        private int countSymbol(String part, char symbol) {
            if (part == null || part.isEmpty()) {
                return 0;
            }
            Matcher grouped = Pattern.compile(symbol + "\\((\\d+)\\)").matcher(part);
            int total = 0;
            while (grouped.find()) {
                total += Integer.parseInt(grouped.group(1));
            }
            String stripped = part.replaceAll(symbol + "\\(\\d+\\)", "");
            for (int i = 0; i < stripped.length(); i++) {
                if (stripped.charAt(i) == symbol) {
                    total++;
                }
            }
            return total;
        }
    }

    private CobolIntermediateModel resolveIntermediateModel(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object sourcePath = metadata.get("filePath");
        if (sourcePath instanceof String pathStr && !pathStr.isBlank()) {
            Path path = Paths.get(pathStr);
            if (Files.exists(path)) {
                return intermediateModelService.parse(path);
            }
        }
        Object rawSource = metadata.get("source");
        if (rawSource instanceof String source && !source.isBlank()) {
            return intermediateModelService.parse(source);
        }
        return null;
    }

    /**
     * Adds a field with getter and setter to a class builder
     */
    private void addFieldToClass(TypeSpec.Builder classBuilder, String fieldName, String javaType) {
        ClassName fieldType = getClassNameForType(javaType);
        // Add field
        FieldSpec field = FieldSpec.builder(fieldType, fieldName)
                .addModifiers(Modifier.PRIVATE)
                .build();
        classBuilder.addField(field);
        // Add getter
        String getterName = getterName(fieldName);
        MethodSpec getter = MethodSpec.methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return $L", fieldName)
                .build();
        classBuilder.addMethod(getter);
        // Add setter
        String setterName = "set" + capitalizeForAccessor(fieldName);
        MethodSpec setter = MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$L = $L", fieldName, fieldName)
                .build();
        classBuilder.addMethod(setter);
    }

    /**
     * Maps Java type string to ClassName
     */
    private ClassName getClassNameForType(String javaType) {
        switch (javaType) {
            case "String":
                return ClassName.get(String.class);
            case "Integer":
                return ClassName.get(Integer.class);
            case "Long":
                return ClassName.get(Long.class);
            case "BigDecimal":
                return ClassName.get(BigDecimal.class);
            default:
                return ClassName.get(Object.class);
        }
    }

    /**
     * Converts string to PascalCase (preserva mayúsculas en siglas y nombres COBOL, separa por punto también)
     */
    private String toPascalCase(String input) {
        if (input == null || input.isEmpty()) return "CobolProgram";

        System.out.println("DEBUG: toPascalCase input: '" + input + "'");

        // Limpiar caracteres especiales que no son válidos en nombres de clase Java (incluyendo apostrofes)
        // Primero quitar la extensión del archivo si existe
        String withoutExtension = input.replaceAll("\\.(cob|cobol|cbl|cpy)$", "");

        // Limpiar todos los caracteres especiales incluyendo apostrofes, guiones, espacios, etc.
        String cleaned = withoutExtension.replaceAll("[^a-zA-Z0-9]", " ");
        System.out.println("DEBUG: after cleaning: '" + cleaned + "'");

        // Dividir por espacios múltiples y procesar cada parte
        String[] parts = cleaned.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        System.out.println("DEBUG: parts array: " + java.util.Arrays.toString(parts));

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // Ignorar palabras comunes que no aportan valor al nombre de clase
            if (part.equalsIgnoreCase("cob") || part.equalsIgnoreCase("cobol") ||
                    part.equalsIgnoreCase("cbl") || part.equalsIgnoreCase("cpy") ||
                    part.equalsIgnoreCase("program") || part.equalsIgnoreCase("file")) {
                System.out.println("DEBUG: skipping common word: '" + part + "'");
                continue;
            }

            System.out.println("DEBUG: processing part: '" + part + "'");
            // Capitalizar primera letra y hacer el resto lowercase
            result.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase());
            }
        }

        // Si el resultado está vacío, usar un nombre por defecto
        String finalResult = result.toString();
        if (finalResult.isEmpty()) {
            System.out.println("DEBUG: empty result, using default");
            finalResult = "CobolProgram";
        }

        // Asegurar que el nombre comience con una letra
        if (!Character.isLetter(finalResult.charAt(0))) {
            finalResult = "Cobol" + finalResult;
        }

        // Validación final para asegurar que el nombre sea válido para Java
        // Solo permitir letras, números y guiones bajos, debe comenzar con letra
        finalResult = finalResult.replaceAll("[^A-Za-z0-9]", "");
        if (finalResult.isEmpty() || !Character.isLetter(finalResult.charAt(0))) {
            finalResult = "CobolProgram";
        }

        System.out.println("DEBUG: toPascalCase final output: '" + finalResult + "'");
        return finalResult;
    }

    /**
     * Converts string to camelCase (preserva mayúsculas en siglas y nombres COBOL)
     */
    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) return input;

        // Limpiar caracteres especiales que no son válidos en nombres de variable Java
        String cleaned = input.replaceAll("[^a-zA-Z0-9\\s\\-_]", "");

        String[] parts = cleaned.split("[-_\\s]+");
        if (parts.length == 0) return "field";

        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            result.append(parts[i].substring(0, 1).toUpperCase());
            if (parts[i].length() > 1) {
                result.append(parts[i].substring(1).toLowerCase());
            }
        }

        // Si el resultado está vacío o comienza con número, agregar prefijo
        String finalResult = result.toString();
        if (finalResult.isEmpty() || Character.isDigit(finalResult.charAt(0))) {
            return "field" + capitalize(finalResult);
        }

        return finalResult;
    }

    /**
     * Capitalizes first letter of string
     */
    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1).toLowerCase();
    }

    /**
     * Sanitizes the class name to ensure it is a valid Java identifier
     */
    private String sanitizeClassName(String className) {
        if (className == null || className.isEmpty()) return "CobolProgram";

        System.out.println("DEBUG: sanitizeClassName input: '" + className + "'");

        // Limpiar caracteres especiales que no son válidos en nombres de clase Java
        String sanitized = className.replaceAll("[^a-zA-Z0-9_$]", " ");

        // Dividir por espacios múltiples y procesar cada parte
        String[] parts = sanitized.trim().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            // Ignorar palabras comunes que no aportan valor al nombre de clase
            if (part.equalsIgnoreCase("cob") || part.equalsIgnoreCase("cobol") ||
                    part.equalsIgnoreCase("cbl") || part.equalsIgnoreCase("cpy") ||
                    part.equalsIgnoreCase("program") || part.equalsIgnoreCase("file")) {
                continue;
            }

            // Capitalizar primera letra y hacer el resto lowercase
            result.append(part.substring(0, 1).toUpperCase());
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase());
            }
        }

        // Si el resultado está vacío, usar un nombre por defecto
        String finalResult = result.toString();
        if (finalResult.isEmpty()) {
            finalResult = "CobolProgram";
        }

        // Asegurar que el nombre comience con una letra
        if (!Character.isLetter(finalResult.charAt(0))) {
            finalResult = "Cobol" + finalResult;
        }

        // Validación final para asegurar que el nombre sea válido para Java
        // Solo permitir letras, números y guiones bajos, debe comenzar con letra
        finalResult = finalResult.replaceAll("[^A-Za-z0-9]", "");
        if (finalResult.isEmpty() || !Character.isLetter(finalResult.charAt(0))) {
            finalResult = "CobolProgram";
        }

        System.out.println("DEBUG: sanitizeClassName output: '" + finalResult + "'");
        return finalResult;
    }

    /**
     * Writes the generated Java files to disk
     */
    private String writeGeneratedFilesToDisk(Map<String, String> generatedFiles, Workspace workspace) {
        try {
            Path outputDir = resolveOutputDir(workspace).toAbsolutePath().normalize();
            return artifactTreeWriter.write(generatedFiles, outputDir).toString();

        } catch (Exception e) {
            throw new IllegalStateException("Could not persist generated artifacts: " + e.getMessage(), e);
        }
    }

    private Path resolveOutputDir(Workspace workspace) {
        Path workspacePath = Paths.get(workspace.getPath());
        if (workspace.getMetadata() != null) {
            Object outputDir = workspace.getMetadata().get("outputDir");
            if (outputDir != null && !outputDir.toString().isBlank()) {
                Path requested = Paths.get(outputDir.toString());
                if (requested.isAbsolute()) {
                    return requested.normalize();
                }
                return workspacePath.resolve(requested).normalize();
            }
        }
        return workspacePath.resolve("generated-java-stubs").normalize();
    }
}
