package org.modernash.renovatio.provider.cobol.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.*;
import org.modernash.renovatio.architecture.ArchitectureRequest;
import org.modernash.renovatio.architecture.ArchitectureResult;
import org.modernash.renovatio.architecture.ArchitectureTransformer;
import org.modernash.renovatio.architecture.ArtifactLayoutPlanner;
import org.modernash.renovatio.architecture.GroupingConfiguration;
import org.modernash.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.modernash.renovatio.cobol.ir.model.CobolDataItem;
import org.modernash.renovatio.provider.cobol.service.generation.FilePortGenerator;
import org.modernash.renovatio.provider.cobol.service.generation.ProgramPortGenerator;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolStatement;
import org.modernash.renovatio.cobol.ir.model.EvaluateStatement;
import org.modernash.renovatio.cobol.ir.model.IfStatement;
import org.modernash.renovatio.cobol.ir.model.InitializeStatement;
import org.modernash.renovatio.cobol.ir.model.Level88Condition;
import org.modernash.renovatio.cobol.ir.model.PerformStatement;
import org.modernash.renovatio.cobol.ir.model.SetConditionStatement;
import org.modernash.renovatio.cobol.ir.model.SimpleStatement;
import org.modernash.renovatio.cobol.recipes.CobolDataVerbValueResolver;
import org.modernash.renovatio.shared.emission.TargetEmitterRegistry;
import org.modernash.renovatio.decisions.DecisionResolver;
import org.modernash.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.modernash.renovatio.provider.cobol.translation.CobolSemanticProjector;
import org.modernash.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.modernash.renovatio.provider.cobol.translation.AnnotatedContextResolver;
import org.modernash.renovatio.provider.cobol.translation.AnnotationActionItemFactory;
import org.modernash.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.modernash.renovatio.provider.cobol.guardrail.ManualActionItemWriter;
import org.modernash.renovatio.shared.domain.StubResult;
import org.modernash.renovatio.shared.domain.Workspace;
import org.modernash.renovatio.shared.emission.EmittedArtifacts;
import org.modernash.renovatio.shared.emission.TargetModel;
import org.modernash.renovatio.shared.nql.NqlQuery;
import org.modernash.renovatio.profile.EffectiveProfileResolver;
import org.modernash.renovatio.profile.MigrationProfiles;
import org.modernash.renovatio.architecture.java.JavaArchitectureLayoutPlanner;
import org.modernash.renovatio.architecture.java.JavaArchitectureSourceLayout;
import org.modernash.renovatio.semantic.ir.SemanticProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
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

    private static final Logger log = LoggerFactory.getLogger(JavaGenerationService.class);
    private static final String BUSINESS_LOGIC_PLACEHOLDER = "// TODO: Implement COBOL business logic";
    public static final String ACTION_ITEM_OUTPUT_DIRECTORY_METADATA_KEY = "actionItemOutputDir";

    private final CobolParsingService parsingService;
    private final TemplateCodeGenerationService templateService;
    private final CobolIntermediateModelService intermediateModelService;
    private final CobolSemanticTranspiler semanticTranspiler;
    private final AnnotatedContextResolver annotatedContextResolver;
    private final AnnotationActionItemFactory annotationActionItemFactory;
    private final ManualActionItemWriter manualActionItemWriter;
    private final GeneratedArtifactTreeWriter artifactTreeWriter = new GeneratedArtifactTreeWriter();
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
                                 boolean useEmitterRegistry) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler, objectMapper,
                useEmitterRegistry, new TargetEmitterRegistry(List.of()), null);
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper,
                                 boolean useEmitterRegistry,
                                 TargetEmitterRegistry emitterRegistry,
                                 EffectiveProfileResolver effectiveProfileResolver) {
        this(parsingService, templateService, intermediateModelService, semanticTranspiler, objectMapper,
                useEmitterRegistry, emitterRegistry, effectiveProfileResolver,
                List.of(new JavaArchitectureLayoutPlanner()));
    }

    public JavaGenerationService(CobolParsingService parsingService,
                                 TemplateCodeGenerationService templateService,
                                 CobolIntermediateModelService intermediateModelService,
                                 CobolSemanticTranspiler semanticTranspiler,
                                 ObjectMapper objectMapper,
                                 boolean useEmitterRegistry,
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
        return generateInterfaceStubs(query, workspace, effective, null);
    }

    /** Applies only when the workspace still projects to the manifest returned by preview. */
    public StubResult generateInterfaceStubs(NqlQuery query, Workspace workspace,
                                             MigrationProfiles.EffectiveProfile effective,
                                             String expectedManifestHash) {
        try {
            Path root = workspaceRoot(workspace);
            ArchitecturePreparation preparation = prepareArchitecture(query, workspace, effective);
            ArchitectureResult architecture = preparation.architecture();
            if (expectedManifestHash != null && !expectedManifestHash.equals(architecture.manifestHash())) {
                throw new ManifestChangedException(expectedManifestHash, architecture.manifestHash());
            }
            Map<String, String> generatedFiles = new LinkedHashMap<>();
            Map<String, ManualActionItem> actionItems = new LinkedHashMap<>();
            for (ArchitectureResult.ArchitectedProgram architected : architecture.programs()) {
                Path source = preparation.sourceByProgram().get(architected.programId());
                StubResult emitted = emitProjected(architected.targetModel(),
                        semantic -> renderInterfaceStubs(query, workspace, semantic, source, false, actionItems));
                if (!emitted.isSuccess()) return emitted;
                if (emitted.getGeneratedCode() != null) {
                    emitted.getGeneratedCode().forEach((path, content) -> putArtifact(generatedFiles, path, content));
                }
            }
            String outputPath = resolveOutputDir(workspace).toString();
            boolean explicitOutput = workspace.getMetadata() != null
                    && workspace.getMetadata().get("outputDir") != null
                    && !workspace.getMetadata().get("outputDir").toString().isBlank();
            if (!generatedFiles.isEmpty()
                    && (effective.profile().target().language()
                    == org.modernash.renovatio.profile.MigrationProfile.Language.JAVA || explicitOutput)) {
                outputPath = writeGeneratedFilesToDisk(generatedFiles, workspace);
            }
            if (effective.profile().target().language() == org.modernash.renovatio.profile.MigrationProfile.Language.JAVA) {
                Path actionItemRoot = explicitOutput ? resolveActionItemOutputDir(workspace) : root;
                manualActionItemWriter.write(actionItemRoot.resolve(ManualActionItemWriter.DEFAULT_REPORT),
                        actionItems.values());
            }
            StubResult result = new StubResult(!generatedFiles.isEmpty(), generatedFiles.isEmpty()
                    ? "No target files generated"
                    : "Generated " + generatedFiles.size() + " target files in: " + outputPath);
            result.setGeneratedCode(generatedFiles);
            result.setTargetLanguage(effective.profile().target().language().name());
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("outputPath", outputPath);
            metadata.put("generatedFileCount", generatedFiles.size());
            metadata.put("generatedFiles", generatedFiles.keySet().stream().sorted().toList());
            metadata.put("manifestHash", architecture.manifestHash());
            metadata.put("manualActionItems", actionItems.values().stream().sorted().toList());
            result.setMetadata(metadata);
            return result;
        } catch (TargetEmitterRegistry.TargetEmitterUnavailableException unavailable) {
            throw unavailable;
        } catch (Exception exception) {
            StubResult failure = new StubResult(false, "Stub generation failed: " + exception.getMessage());
            failure.setGeneratedCode(Map.of());
            return failure;
        }
    }

    public static final class ManifestChangedException extends IllegalStateException {
        public static final String CODE = "ARCHITECTURE_MANIFEST_CHANGED";
        public ManifestChangedException(String expected, String actual) {
            super(CODE + ": expected=" + expected + ", actual=" + actual + "; run preview again");
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
        EmittedArtifacts emitted = emitterRegistry.emit(targetModel, (ignoredModel, ignoredProfile) -> {
            StubResult result = generation.apply(semantic);
            resultReference.set(result);
            EmittedArtifacts raw = result.isSuccess() && result.getGeneratedCode() != null
                    ? EmittedArtifacts.fromUtf8(result.getGeneratedCode())
                    : new EmittedArtifacts(List.of());
            return raw;
        });
        StubResult result = resultReference.get();
        if (result != null && !result.isSuccess()) {
            // Report why generation failed instead of an empty-output manifest mismatch.
            result.setTargetLanguage(targetModel.profile().target().language().name());
            return result;
        }
        if (targetModel.targetLanguage() == org.modernash.renovatio.profile.MigrationProfile.Language.JAVA) {
            emitted = applyManifest(targetModel, emitted);
        }
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
            String key = fileName.toLowerCase(Locale.ROOT);
            if (expectedByFileName.putIfAbsent(key, path) != null) {
                throw new TargetManifestMismatchException("manifest contains duplicate Java file name " + fileName);
            }
        }
        Map<String, String> rebased = new LinkedHashMap<>();
        emitted.artifacts().forEach(artifact -> {
            String fileName = Path.of(artifact.path()).getFileName().toString();
            String planned = expectedByFileName.remove(fileName.toLowerCase(Locale.ROOT));
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

    private StubResult renderInterfaceStubs(NqlQuery query, Workspace workspace) {
        return renderInterfaceStubs(query, workspace, null, null, true, null);
    }

    private StubResult renderInterfaceStubs(NqlQuery query, Workspace workspace,
                                                     SemanticProgram semanticProgram) {
        return renderInterfaceStubs(query, workspace, semanticProgram, null, true, null);
    }

    private StubResult renderInterfaceStubs(NqlQuery query, Workspace workspace,
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
            List<org.modernash.renovatio.provider.cobol.domain.CobolProgram> programs = (List<org.modernash.renovatio.provider.cobol.domain.CobolProgram>)
                    ((Map<String, Object>) analyzeResult.getData()).get("programs");

            Map<String, String> generatedFiles = new LinkedHashMap<>();
            Map<String, ManualActionItem> actionItems = new LinkedHashMap<>();

            for (org.modernash.renovatio.provider.cobol.domain.CobolProgram program : programs) {
                Map<String, Object> metadata = program.getMetadata();
                String fileName = (String) metadata.get("filePath");
                Path cobolPath = Path.of(fileName).toAbsolutePath().normalize();
                String sourceReference = sourceReference(workspace, cobolPath);
                if (selectedSource != null && !selectedSource.toAbsolutePath().normalize().equals(cobolPath)) continue;
                String baseName = cobolPath.getFileName().toString();
                // Clean and sanitize the class base name
                String classBase = sanitizeClassName(toPascalCase(baseName));

                log.debug("Processing COBOL artifact");

                try {
                    CobolIntermediateModel model = resolveIntermediateModel(metadata);
                    SemanticProgram currentSemantic = semanticProgram;
                    AnnotatedContextResolver.Resolution annotatedResolution = annotatedContextResolver.resolve(
                            new AnnotatedContextResolver.Request(Optional.empty(), Optional.empty(), cobolPath), model);
                    annotatedResolution.diagnostics().stream()
                            .map(diagnostic -> annotationActionItemFactory.toResolutionDiagnostic(
                                    diagnostic, sourceReference, model.getProgramId()))
                            .forEach(item -> actionItems.putIfAbsent(item.id(), item));

                    // Generate DTO class for data structures
                    String dtoClass = generateDataTransferObject(classBase, metadata, model);
                    if (annotatedResolution.context().isPresent()) {
                        dtoClass = semanticTranspiler.enrichServiceImplementation(dtoClass,
                                annotatedResolution.context().orElseThrow(),
                                sourceReference,
                                items -> items.forEach(item -> actionItems.putIfAbsent(item.id(), item)),
                                currentSemantic == null ? null : currentSemantic.dataIntents());
                    }
                    putArtifact(generatedFiles, classBase + "DTO.java", dtoClass);
                    // Generate service interface
                    String serviceInterface = generateServiceInterface(classBase, metadata);
                    putArtifact(generatedFiles, classBase + "Service.java", serviceInterface);
                    // Generate implementation template
                    List<FilePortGenerator.FilePort> filePorts = FilePortGenerator.ports(classBase,
                            currentSemantic != null
                                    ? org.modernash.renovatio.architecture.java.JavaArchitectureLayoutPlanner
                                            .fileResources(currentSemantic)
                                    : referencedFiles(model));
                    for (FilePortGenerator.FilePort port : filePorts) {
                        putArtifact(generatedFiles, port.portType() + ".java", FilePortGenerator.port(port, model));
                        putArtifact(generatedFiles, port.adapterType() + ".java",
                                FilePortGenerator.inMemoryAdapter(port, model));
                    }
                    List<ProgramPortGenerator.ProgramPort> programPorts = ProgramPortGenerator.ports(classBase,
                            currentSemantic != null
                                    ? org.modernash.renovatio.architecture.java.JavaArchitectureLayoutPlanner
                                            .calledPrograms(currentSemantic)
                                    : calledPrograms(model));
                    for (ProgramPortGenerator.ProgramPort port : programPorts) {
                        putArtifact(generatedFiles, port.portType() + ".java", ProgramPortGenerator.port(port));
                        putArtifact(generatedFiles, port.adapterType() + ".java", ProgramPortGenerator.adapter(port));
                    }
                    List<InjectedPort> injected = new ArrayList<>();
                    filePorts.forEach(port -> injected.add(new InjectedPort(port.portType(), port.fieldName(),
                            port.adapterType())));
                    programPorts.forEach(port -> injected.add(new InjectedPort(port.portType(), port.fieldName(),
                            port.adapterType())));
                    String serviceImpl = generateServiceImplementation(classBase, metadata, injected);
                    collectUntranslatedStatements(model, sourceReference, actionItems);
                    serviceImpl = translateServiceImplementation(serviceImpl, model,
                            annotatedResolution.context().orElse(null), sourceReference, currentSemantic, actionItems);
                    log.debug("Generated service implementation; characterCount={}", serviceImpl.length());
                    putArtifact(generatedFiles, classBase + "ServiceImpl.java", serviceImpl);

                    @SuppressWarnings("unchecked")
                    Set<String> cics = (Set<String>) metadata.get("cicsCommands");
                    if (cics != null && !cics.isEmpty() && model != null && model.usesCics()) {
                        putArtifact(generatedFiles, classBase + "CicsController.java", screenController(classBase));
                    } else if (cics != null && !cics.isEmpty()) {
                        Map<String, Object> tmplData = new HashMap<>();
                        tmplData.put("className", classBase + "CicsController");
                        List<Map<String, String>> transactions = new ArrayList<>();
                        for (String command : cics) {
                            Map<String, String> tx = new HashMap<>();
                            tx.put("name", command);
                            tx.put("method", javaMethodName(command));
                            transactions.add(tx);
                        }
                        tmplData.put("transactions", transactions);
                        String controller = templateService.generateCicsController(tmplData);
                        putArtifact(generatedFiles, classBase + "CicsController.java", controller);
                    }
                } catch (Exception e) {
                    log.debug("Artifact generation failed; exceptionType={}", e.getClass().getSimpleName());
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

            log.debug("Generated artifact count={}", generatedFiles.size());

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

    private static String sourceReference(Workspace workspace, Path source) {
        Path root = workspaceRoot(workspace);
        return source.startsWith(root)
                ? root.relativize(source).toString().replace('\\', '/')
                : source.toString().replace('\\', '/');
    }

    private String translateServiceImplementation(String javaSource, CobolIntermediateModel model,
                                                  AnnotatedCobolContext annotated, String sourceFile,
                                                  SemanticProgram semantic, Map<String, ManualActionItem> actionItems) {
        String translated = semanticTranspiler.enrichServiceImplementation(javaSource, model, annotated,
                sourceFile,
                items -> items.forEach(item -> actionItems.putIfAbsent(item.id(), item)),
                semantic == null ? null : semantic.dataIntents());
        boolean hasExecutableStatements = model.getParagraphs().values().stream()
                .anyMatch(paragraph -> !paragraph.statements().isEmpty());
        if (!hasExecutableStatements && translated != null) {
            translated = translated.replace(BUSINESS_LOGIC_PLACEHOLDER,
                    "// No executable COBOL statements");
        }
        if (translated == null || translated.contains(BUSINESS_LOGIC_PLACEHOLDER)) {
            throw new IllegalStateException("semantic translation left the COBOL business-logic placeholder for "
                    + model.getProgramId());
        }
        return translated;
    }

    private void collectUntranslatedStatements(CobolIntermediateModel model, String sourceFile,
                                               Map<String, ManualActionItem> actionItems) {
        model.getParagraphs().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            int[] statementIndex = {0};
            collectUntranslatedStatements(entry.getValue().statements(), model, sourceFile,
                    model.getProgramId(), entry.getKey(), statementIndex, actionItems);
        });
        collectPerformCycles(model, sourceFile, actionItems);
    }

    /**
     * Detects recursive PERFORM cycles over the paragraph call graph (including THRU ranges) and
     * emits a stable error action item per cyclic strongly-connected component, so generation never
     * recurses infinitely over recursive PERFORM calls.
     */
    private void collectPerformCycles(CobolIntermediateModel model, String sourceFile,
                                      Map<String, ManualActionItem> actionItems) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        for (String paragraph : model.getParagraphs().keySet()) {
            List<String> targets = new ArrayList<>();
            for (CobolStatement statement : model.getParagraphs().get(paragraph).statements()) {
                collectPerformTargets(statement, model, targets);
            }
            if (!targets.isEmpty()) {
                graph.put(paragraph, List.copyOf(targets));
            }
        }
        for (List<String> component : tarjanCyclicComponents(graph)) {
            ManualActionItem item = annotationActionItemFactory.toPerformCycle(
                    sourceFile, model.getProgramId(), component.get(0), component);
            actionItems.putIfAbsent(item.id(), item);
        }
    }

    private void collectPerformTargets(CobolStatement statement, CobolIntermediateModel model,
                                       List<String> out) {
        if (statement instanceof PerformStatement perform) {
            if (!perform.isInline() && perform.paragraph() != null && !perform.paragraph().isBlank()) {
                String start = perform.paragraph();
                if (!out.contains(start)) {
                    out.add(start);
                }
                if (perform.throughParagraph() != null && !perform.throughParagraph().equals(start)) {
                    for (String name : performRangeNames(start, perform.throughParagraph(), model)) {
                        if (!out.contains(name)) {
                            out.add(name);
                        }
                    }
                }
            }
            for (CobolStatement nested : perform.inlineBody()) {
                collectPerformTargets(nested, model, out);
            }
        } else if (statement instanceof IfStatement ifStatement) {
            for (CobolStatement nested : ifStatement.thenStatements()) {
                collectPerformTargets(nested, model, out);
            }
            for (CobolStatement nested : ifStatement.elseStatements()) {
                collectPerformTargets(nested, model, out);
            }
        } else if (statement instanceof EvaluateStatement evaluation) {
            for (EvaluateStatement.EvaluateWhenBranch branch : evaluation.branches()) {
                for (CobolStatement nested : branch.statements()) {
                    collectPerformTargets(nested, model, out);
                }
            }
        } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.CallStatement call) {
            for (CobolStatement nested : call.exceptionStatements()) {
                collectPerformTargets(nested, model, out);
            }
            for (CobolStatement nested : call.normalStatements()) {
                collectPerformTargets(nested, model, out);
            }
        } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.FileOperationStatement file) {
            for (CobolStatement nested : file.exceptionStatements()) {
                collectPerformTargets(nested, model, out);
            }
            for (CobolStatement nested : file.normalStatements()) {
                collectPerformTargets(nested, model, out);
            }
        }
    }

    private static List<String> performRangeNames(String start, String through, CobolIntermediateModel model) {
        List<String> names = new ArrayList<>();
        boolean capture = false;
        for (String name : model.getParagraphs().keySet()) {
            if (name.equals(start)) {
                capture = true;
            }
            if (capture) {
                names.add(name);
            }
            if (capture && name.equals(through)) {
                break;
            }
        }
        if (!names.contains(through)) {
            names = new ArrayList<>();
            names.add(start);
            if (through != null && !names.contains(through)) {
                names.add(through);
            }
        }
        return names;
    }

    private static List<List<String>> tarjanCyclicComponents(Map<String, List<String>> graph) {
        List<List<String>> cyclic = new ArrayList<>();
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Deque<String> stack = new ArrayDeque<>();
        int[] nextIndex = {0};
        for (String vertex : new TreeSet<>(graph.keySet())) {
            if (!index.containsKey(vertex)) {
                strongConnect(vertex, graph, index, lowlink, onStack, stack, nextIndex, cyclic);
            }
        }
        return cyclic;
    }

    private static void strongConnect(String vertex, Map<String, List<String>> graph,
                                      Map<String, Integer> index, Map<String, Integer> lowlink,
                                      Map<String, Boolean> onStack, Deque<String> stack,
                                      int[] nextIndex, List<List<String>> cyclic) {
        index.put(vertex, nextIndex[0]);
        lowlink.put(vertex, nextIndex[0]);
        nextIndex[0]++;
        stack.push(vertex);
        onStack.put(vertex, Boolean.TRUE);
        for (String next : graph.getOrDefault(vertex, List.of())) {
            if (!index.containsKey(next)) {
                strongConnect(next, graph, index, lowlink, onStack, stack, nextIndex, cyclic);
                lowlink.put(vertex, Math.min(lowlink.get(vertex), lowlink.get(next)));
            } else if (Boolean.TRUE.equals(onStack.get(next))) {
                lowlink.put(vertex, Math.min(lowlink.get(vertex), index.get(next)));
            }
        }
        if (lowlink.get(vertex).equals(index.get(vertex))) {
            List<String> component = new ArrayList<>();
            String current;
            do {
                current = stack.pop();
                onStack.put(current, Boolean.FALSE);
                component.add(current);
            } while (!current.equals(vertex));
            boolean isCycle = component.size() > 1
                    || graph.getOrDefault(vertex, List.of()).contains(vertex);
            if (isCycle) {
                component.sort(String::compareTo);
                cyclic.add(component);
            }
        }
    }

    private void collectUntranslatedStatements(List<CobolStatement> statements, CobolIntermediateModel model,
                                               String sourceFile,
                                               String programId, String paragraph, int[] statementIndex,
                                               Map<String, ManualActionItem> actionItems) {
        for (CobolStatement statement : statements) {
            int currentIndex = statementIndex[0]++;
            if (statement instanceof SimpleStatement simple
                    && simple.kind() == SimpleStatement.Kind.UNTRANSLATED) {
                ManualActionItem item = annotationActionItemFactory.toUntranslatedStatement(
                        simple, sourceFile, programId, paragraph, currentIndex);
                actionItems.putIfAbsent(item.id(), item);
            } else if (statement instanceof InitializeStatement initialize) {
                List<String> unknownTargets = initialize.targets().stream()
                        .filter(target -> modelDataItem(model, target)
                                .flatMap(CobolDataVerbValueResolver::initialValue).isEmpty())
                        .toList();
                if (!unknownTargets.isEmpty()) {
                    ManualActionItem item = annotationActionItemFactory.toUnsupportedStatement(
                            initialize.sourceText(), "INITIALIZE",
                            "INITIALIZE targets are not elementary data items: " + unknownTargets,
                            sourceFile, programId, paragraph, currentIndex);
                    actionItems.putIfAbsent(item.id(), item);
                }
            } else if (statement instanceof SetConditionStatement setCondition) {
                List<String> unknownConditions = setCondition.conditionNames().stream()
                        .filter(condition -> !hasRepresentableConditionValue(
                                model, condition, setCondition.value()))
                        .toList();
                if (!unknownConditions.isEmpty()) {
                    ManualActionItem item = annotationActionItemFactory.toUnsupportedStatement(
                            setCondition.sourceText(), "SET_CONDITION",
                            "SET targets are not declared or have no representable level-88 value: "
                                    + unknownConditions,
                            sourceFile, programId, paragraph, currentIndex);
                    actionItems.putIfAbsent(item.id(), item);
                }
            } else if (statement instanceof IfStatement conditional) {
                collectUntranslatedStatements(conditional.thenStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
                collectUntranslatedStatements(conditional.elseStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
            } else if (statement instanceof EvaluateStatement evaluation) {
                evaluation.branches().forEach(branch -> collectUntranslatedStatements(branch.statements(), model,
                        sourceFile, programId, paragraph, statementIndex, actionItems));
            } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.CallStatement call) {
                collectUntranslatedStatements(call.exceptionStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
                collectUntranslatedStatements(call.normalStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
            } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.FileOperationStatement file) {
                collectUntranslatedStatements(file.exceptionStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
                collectUntranslatedStatements(file.normalStatements(), model, sourceFile,
                        programId, paragraph, statementIndex, actionItems);
            }
        }
    }

    private Optional<CobolDataItem> modelDataItem(CobolIntermediateModel model, String name) {
        return model.getDataItems().stream()
                .filter(item -> item.name().equalsIgnoreCase(name))
                .findFirst();
    }

    private boolean hasRepresentableConditionValue(CobolIntermediateModel model, String name, boolean value) {
        for (CobolDataItem item : model.getDataItems()) {
            for (Level88Condition condition : item.level88Conditions()) {
                if (condition.name().equalsIgnoreCase(name)) {
                    return CobolDataVerbValueResolver.conditionValue(item, condition, value).isPresent();
                }
            }
        }
        return false;
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
        return generateDataTransferObject(cleanClassName, programData, resolveIntermediateModel(programData));
    }

    private String generateDataTransferObject(String cleanClassName, Map<String, Object> programData,
                                              CobolIntermediateModel model) {
        // Asegurar que el cleanClassName esté completamente limpio
        String sanitizedClassName = sanitizeClassName(cleanClassName);
        String className = sanitizedClassName + "DTO";

        log.debug("Generating data-transfer object");

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Data Transfer Object generated from COBOL program: $L\n", sanitizedClassName);


        Map<String, String> irTypes = irFieldTypes(model);
        Map<String, Integer> tables = tableSizes(model);
        Set<String> views = redefinesViews(model);
        Map<String, Integer> textLengths = new HashMap<>();
        if (model != null) {
            for (CobolDataItem item : model.getDataItems()) {
                if (item.getPicType() != null && item.getPicType().category()
                        != org.modernash.renovatio.cobol.runtime.PicType.Category.NUMERIC) {
                    textLengths.putIfAbsent(toCamelCase(item.getName()), item.getPicType().digits());
                }
            }
        }
        Map<String, String> declared = new LinkedHashMap<>();
        for (Map<String, Object> item : resolveDtoFields(programData)) {
            String fieldName = (String) item.get("name");
            String javaType = (String) item.get("javaType");
            if (fieldName != null && javaType != null && !views.contains(fieldName)) {
                String type = irTypes.getOrDefault(fieldName, javaType);
                addDtoField(classBuilder, fieldName, type, tables.getOrDefault(fieldName, 0),
                        textLengths.getOrDefault(fieldName, 0));
                declared.put(fieldName, type);
            }
        }
        for (Map<String, Object> item : copybookOnlyFields(programData, model)) {
            if (views.contains((String) item.get("name"))) continue;
            addDtoField(classBuilder, (String) item.get("name"), (String) item.get("javaType"),
                    tables.getOrDefault((String) item.get("name"), 0),
                    textLengths.getOrDefault((String) item.get("name"), 0));
            declared.put((String) item.get("name"), (String) item.get("javaType"));
        }
        // Record fields the data-item scan missed (e.g. BMS "02 X-L COMP PIC S9(4).", USAGE before PIC).
        if (model != null) {
            for (org.modernash.renovatio.cobol.ir.model.CobolRecordLayout layout : model.getRecordLayouts().values()) {
                for (org.modernash.renovatio.cobol.ir.model.CobolRecordLayout.Field field : layout.fields()) {
                    String name = field.filler() ? null : toCamelCase(field.name());
                    if (!layout.composable() || name == null || declared.containsKey(name) || views.contains(name)) {
                        continue;
                    }
                    String type = org.modernash.renovatio.cobol.ir.context.CobolTypeMapper.javaType(field.picType());
                    addDtoField(classBuilder, name, type, tables.getOrDefault(name, 0),
                            "String".equals(type) ? field.length() : 0);
                    declared.put(name, type);
                }
            }
        }
        org.modernash.renovatio.provider.cobol.service.generation.RecordAccessorGenerator
                .addTo(classBuilder, model, declared, this::toCamelCase);
        classBuilder.addMethod(initialValuesConstructor(model, declared));
        if (model != null && model.endsFromPerformedParagraphs()) {
            org.modernash.renovatio.provider.cobol.service.generation.ScreenTypesGenerator.addProgramExit(classBuilder);
        }
        if (model != null && model.usesCics()) {
            org.modernash.renovatio.provider.cobol.service.generation.ScreenTypesGenerator.addScreenTypes(classBuilder);
        }

        TypeSpec classSpec = classBuilder.build();

        JavaFile javaFile = JavaFile.builder("org.modernash.renovatio.generated.cobol", classSpec)
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

        log.debug("Generating service interface");

        ClassName dtoClass = ClassName.get("org.modernash.renovatio.generated.cobol", dtoName);

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

        JavaFile javaFile = JavaFile.builder("org.modernash.renovatio.generated.cobol", interfaceSpec)
                .build();

        return javaFile.toString();
    }

    /**
     * Generates a service implementation template
     */
    private String generateServiceImplementation(String cleanClassName, Map<String, Object> programData) {
        return generateServiceImplementation(cleanClassName, programData, List.<InjectedPort>of());
    }

    /**
     * One final field per file port, a constructor receiving them (the injection point) and a
     * no-argument constructor wiring the in-memory adapters, so the service also runs standalone.
     */
    /** An outbound port the service depends on, with the adapter used for standalone wiring. */
    private record InjectedPort(String portType, String fieldName, String adapterType) { }

    private static void addFilePorts(TypeSpec.Builder classBuilder, List<InjectedPort> ports) {
        if (ports.isEmpty()) {
            return;
        }
        MethodSpec.Builder injecting = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(ClassName.get("org.springframework.beans.factory.annotation", "Autowired"));
        List<String> defaults = new ArrayList<>();
        for (InjectedPort port : ports) {
            ClassName type = ClassName.get(FilePortGenerator.PACKAGE, port.portType());
            classBuilder.addField(FieldSpec.builder(type, port.fieldName(), Modifier.PRIVATE, Modifier.FINAL).build());
            injecting.addParameter(type, port.fieldName());
            injecting.addStatement("this.$L = $L", port.fieldName(), port.fieldName());
            defaults.add("new " + port.adapterType() + "()");
        }
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Standalone wiring with in-memory file adapters and default program adapters.\n")
                .addStatement("this($L)", String.join(", ", defaults))
                .build());
        classBuilder.addMethod(injecting.build());
    }

    /**
     * Distinct files the program performs I/O on (SELECT names, upper case, sorted) — the same
     * set the semantic projection reports, used when no semantic program is available.
     */
    static List<String> referencedFiles(CobolIntermediateModel model) {
        java.util.TreeSet<String> files = new java.util.TreeSet<>();
        if (model != null) {
            model.getParagraphs().values().forEach(paragraph -> collectFiles(paragraph.statements(), files));
        }
        return List.copyOf(files);
    }

    /** Distinct statically named CALL targets (upper case, sorted), as the semantic projection reports them. */
    static List<String> calledPrograms(CobolIntermediateModel model) {
        java.util.TreeSet<String> programs = new java.util.TreeSet<>();
        if (model != null) {
            model.getParagraphs().values().forEach(paragraph -> collectCalls(paragraph.statements(), programs));
        }
        return List.copyOf(programs);
    }

    private static void collectCalls(List<CobolStatement> statements, java.util.Set<String> programs) {
        for (CobolStatement statement : statements) {
            if (statement instanceof org.modernash.renovatio.cobol.ir.model.CallStatement call) {
                if (!call.dynamic()) programs.add(call.target().toUpperCase(Locale.ROOT));
                collectCalls(call.exceptionStatements(), programs);
                collectCalls(call.normalStatements(), programs);
            } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.FileOperationStatement file) {
                collectCalls(file.exceptionStatements(), programs);
                collectCalls(file.normalStatements(), programs);
            } else if (statement instanceof IfStatement branch) {
                collectCalls(branch.thenStatements(), programs);
                collectCalls(branch.elseStatements(), programs);
            } else if (statement instanceof EvaluateStatement evaluation) {
                evaluation.branches().forEach(branch -> collectCalls(branch.statements(), programs));
            } else if (statement instanceof PerformStatement perform && perform.isInline()) {
                collectCalls(perform.inlineBody(), programs);
            }
        }
    }

    private static void collectFiles(List<CobolStatement> statements, java.util.Set<String> files) {
        for (CobolStatement statement : statements) {
            if (statement instanceof org.modernash.renovatio.cobol.ir.model.FileOperationStatement file) {
                files.add(file.fileName().toUpperCase(Locale.ROOT));
                collectFiles(file.exceptionStatements(), files);
                collectFiles(file.normalStatements(), files);
            } else if (statement instanceof org.modernash.renovatio.cobol.ir.model.CallStatement call) {
                collectFiles(call.exceptionStatements(), files);
                collectFiles(call.normalStatements(), files);
            } else if (statement instanceof IfStatement branch) {
                collectFiles(branch.thenStatements(), files);
                collectFiles(branch.elseStatements(), files);
            } else if (statement instanceof EvaluateStatement evaluation) {
                evaluation.branches().forEach(branch -> collectFiles(branch.statements(), files));
            } else if (statement instanceof PerformStatement perform && perform.isInline()) {
                collectFiles(perform.inlineBody(), files);
            }
        }
    }

    private String generateServiceImplementation(String cleanClassName, Map<String, Object> programData,
                                                 List<InjectedPort> filePorts) {
        // Asegurar que el cleanClassName esté completamente limpio
        String sanitizedClassName = sanitizeClassName(cleanClassName);
        String className = sanitizedClassName + "ServiceImpl";
        String interfaceName = sanitizedClassName + "Service";
        String dtoName = sanitizedClassName + "DTO";

        log.debug("Generating service implementation");

        ClassName interfaceClass = ClassName.get("org.modernash.renovatio.generated.cobol", interfaceName);
        ClassName dtoClass = ClassName.get("org.modernash.renovatio.generated.cobol", dtoName);
        CobolIntermediateModel model = resolveIntermediateModel(programData);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceClass)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addJavadoc("Implementation of $L\n", interfaceName)
                .addJavadoc("Generated from COBOL program: $L\n", sanitizedClassName);
        addFilePorts(classBuilder, filePorts);

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

        JavaFile javaFile = JavaFile.builder("org.modernash.renovatio.generated.cobol", classSpec)
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
        Map<String, String> irTypes = irFieldTypes(model);

        for (Map<String, Object> field : resolveDtoFields(programData)) {
            String name = field != null ? (String) field.get("name") : null;
            if (name == null || name.isBlank()) {
                continue;
            }
            if (tableSizes(model).containsKey(name) || redefinesViews(model).contains(name)) {
                continue; // OCCURS arrays and REDEFINES views have no plain field to check
            }
            String javaType = field.get("javaType") != null ? field.get("javaType").toString() : "String";
            validations.putIfAbsent(name, new FieldValidation(name, irTypes.getOrDefault(name, javaType)));
        }

        if (model != null) {
            for (CobolDataItem item : model.getDataItems()) {
                if (item == null) {
                    continue;
                }
                String camel = toCamelCase(item.getName());
                FieldValidation validation = validations.get(camel);
                if (validation != null) {
                    // The DTO field type is the contract validate() checks against; the IR only
                    // contributes PICTURE limits (length, digits, sign).
                    validation.applyPicture(item.getPicture());
                }
            }
        }

        return new ArrayList<>(validations.values());
    }

    /**
     * DTO fields for a program: LINKAGE items when it has ENTRY points, WORKING-STORAGE items
     * otherwise. Anonymous FILLER items are not addressable from PROCEDURE DIVISION and are
     * skipped, and a repeated name keeps its first declaration, so the DTO always compiles.
     * The DTO and its {@code validate()} both read this list, so they cannot disagree.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> resolveDtoFields(Map<String, Object> programData) {
        if (programData == null) {
            return List.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        List<Map<String, Object>> source = entries != null && !entries.isEmpty()
                ? (List<Map<String, Object>>) programData.get("linkageItems")
                : (List<Map<String, Object>>) programData.get("dataItems");
        if (source == null) {
            return List.of();
        }
        Map<String, Map<String, Object>> unique = new LinkedHashMap<>();
        for (Map<String, Object> item : source) {
            Object name = item == null ? null : item.get("name");
            if (!(name instanceof String fieldName) || fieldName.isBlank()
                    || fieldName.equalsIgnoreCase("filler")) {
                continue;
            }
            unique.putIfAbsent(fieldName, item);
        }
        return new ArrayList<>(unique.values());
    }

    /**
     * Default constructor applying WORKING-STORAGE VALUE clauses, so a fresh DTO starts in the
     * state the COBOL program starts in (e.g. END-OF-FILE = 'N' before the read loop).
     */
    private MethodSpec initialValuesConstructor(CobolIntermediateModel model, Map<String, String> declared) {
        MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
        if (model == null) {
            return constructor.build();
        }
        Set<String> initialized = new java.util.HashSet<>();
        Map<String, Integer> tables = tableSizes(model);
        for (CobolDataItem item : model.getDataItems()) {
            String raw = model.getInitialValues().get(item.getName().toUpperCase(Locale.ROOT));
            String field = toCamelCase(item.getName());
            String type = declared.get(field);
            if (type == null || item.getPicType() == null || !initialized.add(field)) {
                continue;
            }
            if (raw == null) {
                // No VALUE clause: start as GnuCOBOL does, numerics zero and alphanumerics spaces.
                raw = "String".equals(type) ? "SPACES" : "ZERO";
            }
            String literal = initialLiteral(raw, type, item.getPicType().digits());
            if (literal != null && tables.containsKey(field)) {
                constructor.addStatement("java.util.Arrays.fill(this.$L, $L)", field, literal);
            } else if (literal != null) {
                constructor.addStatement("this.$L = $L", field, literal);
            }
        }
        return constructor.build();
    }

    private static String initialLiteral(String raw, String type, int length) {
        String upper = raw.toUpperCase(Locale.ROOT);
        boolean quoted = raw.startsWith("'") || raw.startsWith("\"");
        boolean zero = upper.startsWith("ZERO");
        if ("String".equals(type)) {
            String text;
            if (quoted) {
                char quote = raw.charAt(0);
                text = raw.substring(1, raw.length() - 1).replace(String.valueOf(quote) + quote, String.valueOf(quote));
            } else if (upper.startsWith("SPACE")) {
                text = "";
            } else if (zero) {
                text = "0".repeat(length);
            } else if (upper.startsWith("LOW-VALUE")) {
                text = "\u0000".repeat(length);
            } else if (upper.startsWith("HIGH-VALUE")) {
                text = "\u00FF".repeat(length);
            } else {
                text = raw;
            }
            text = text.length() >= length ? text.substring(0, length) : text + " ".repeat(length - text.length());
            return CodeBlock.of("$S", text).toString();
        }
        String number = zero ? "0" : raw.replace("+", "");
        if (!number.matches("-?[0-9]*\\.?[0-9]+")) {
            return null;
        }
        java.math.BigDecimal value = new java.math.BigDecimal(number);
        return switch (type) {
            case "Integer" -> String.valueOf(value.intValue());
            case "Long" -> value.longValue() + "L";
            case "BigDecimal" -> "new java.math.BigDecimal(\"" + value.setScale(Math.max(value.scale(), 0),
                    java.math.RoundingMode.DOWN).toPlainString() + "\")";
            default -> null;
        };
    }

    /**
     * Java type of every elementary IR data item, keyed by DTO field name. The IR type mapper is
     * the single source of truth for field types: the DTO, its validate() and the translated
     * PROCEDURE DIVISION statements must all agree, or the generated service does not compile.
     */
    private Map<String, String> irFieldTypes(CobolIntermediateModel model) {
        Map<String, String> types = new HashMap<>();
        if (model == null) {
            return types;
        }
        for (CobolDataItem item : model.getDataItems()) {
            if (item != null && item.getPicture() != null && item.getJavaType() != null) {
                types.putIfAbsent(toCamelCase(item.getName()), item.getJavaType());
            }
        }
        return types;
    }

    /**
     * Elementary data items the COBOL IR knows (it expands COPY members) that the program
     * metadata does not, e.g. a COMMAREA field declared in a copybook. PROCEDURE DIVISION code
     * reads and writes them, so the DTO must carry them for the translated service to compile.
     * Programs with ENTRY points keep their LINKAGE-only contract.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copybookOnlyFields(Map<String, Object> programData,
                                                        CobolIntermediateModel model) {
        if (model == null || programData == null) {
            return List.of();
        }
        List<Map<String, Object>> entries = (List<Map<String, Object>>) programData.get("entries");
        if (entries != null && !entries.isEmpty()) {
            return List.of();
        }
        Set<String> declared = new java.util.HashSet<>();
        for (Map<String, Object> item : resolveDtoFields(programData)) {
            declared.add((String) item.get("name"));
        }
        List<Map<String, Object>> extra = new ArrayList<>();
        for (CobolDataItem item : model.getDataItems()) {
            if (item == null || item.getPicture() == null || item.getName().equalsIgnoreCase("FILLER")) {
                continue;
            }
            String name = toCamelCase(item.getName());
            // Java names are case-sensitive: TRAN-CAT-CD (tranCatCd) and TRANCAT-CD (trancatCd) differ.
            if (name == null || !declared.add(name)) {
                continue;
            }
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("name", name);
            field.put("javaType", item.getJavaType());
            extra.add(field);
        }
        return extra;
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
     * Screen handler of an online program: one call runs one CICS task, from the screen request
     * (key, COMMAREA, received maps) to the screen response (screen sent, next program or
     * transaction and COMMAREA).
     */
    private static String screenController(String classBase) {
        ClassName dto = ClassName.get("org.modernash.renovatio.generated.cobol", classBase + "DTO");
        ClassName service = ClassName.get("org.modernash.renovatio.generated.cobol", classBase + "Service");
        TypeSpec controller = TypeSpec.classBuilder(classBase + "CicsController")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Screen handler for online program $L.\n", classBase)
                .addField(service, "service", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(service, "service")
                        .addStatement("this.service = service")
                        .build())
                .addMethod(MethodSpec.methodBuilder("handle")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(dto.nestedClass("ScreenResponse"))
                        .addParameter(dto.nestedClass("ScreenRequest"), "request")
                        .addJavadoc("Runs one task for {@code request} and returns the resulting screen and navigation.\n")
                        .addStatement("$T input = new $T()", dto, dto)
                        .addStatement("input.setScreenRequest(request)")
                        .addStatement("return service.process(input).getScreenResponse()")
                        .build())
                .build();
        return JavaFile.builder("org.modernash.renovatio.generated.cobol", controller).build().toString();
    }

    /** DTO names of the modeled REDEFINES views: they are accessors over their base, not fields. */
    private Set<String> redefinesViews(CobolIntermediateModel model) {
        Set<String> views = new java.util.HashSet<>();
        if (model != null) {
            model.getRedefinesViews().values().stream()
                    .filter(org.modernash.renovatio.cobol.ir.model.CobolRedefinesView::supported)
                    .forEach(view -> views.add(toCamelCase(view.name())));
        }
        return views;
    }

    /** Size of every supported fixed OCCURS table, keyed by DTO field name. */
    private Map<String, Integer> tableSizes(CobolIntermediateModel model) {
        Map<String, Integer> sizes = new HashMap<>();
        if (model != null) {
            model.getTableDimensions().forEach((name, size) -> {
                if (size > 0) sizes.put(toCamelCase(name), size);
            });
        }
        return sizes;
    }

    /** A scalar field, or for an OCCURS item an array with 1-based indexed accessors (COBOL subscripts). */
    private void addDtoField(TypeSpec.Builder classBuilder, String fieldName, String javaType, int occurs) {
        addDtoField(classBuilder, fieldName, javaType, occurs, 0);
    }

    private void addDtoField(TypeSpec.Builder classBuilder, String fieldName, String javaType, int occurs,
                             int textLength) {
        if (occurs <= 0) {
            addFieldToClass(classBuilder, fieldName, javaType, textLength);
            return;
        }
        ClassName elementType = getClassNameForType(javaType);
        classBuilder.addField(FieldSpec.builder(ArrayTypeName.of(elementType), fieldName, Modifier.PRIVATE)
                .initializer("new $T[$L]", elementType, occurs)
                .build());
        String accessor = capitalizeForAccessor(fieldName);
        classBuilder.addMethod(MethodSpec.methodBuilder("get" + accessor)
                .addModifiers(Modifier.PUBLIC)
                .returns(elementType)
                .addParameter(int.class, "index")
                .addJavadoc("Element {@code index} (1-based, as the COBOL subscript) of the OCCURS $L table.\n", occurs)
                .addStatement("return $L[index - 1]", fieldName)
                .build());
        classBuilder.addMethod(MethodSpec.methodBuilder("set" + accessor)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(int.class, "index")
                .addParameter(elementType, "value")
                .addStatement("$L[index - 1] = $L", fieldName, fitTextExpression("value", javaType, textLength))
                .build());
    }

    /**
     * Adds a field with getter and setter to a class builder
     */
    private void addFieldToClass(TypeSpec.Builder classBuilder, String fieldName, String javaType) {
        addFieldToClass(classBuilder, fieldName, javaType, 0);
    }

    /**
     * Field with getter and setter; an alphanumeric field of known length stores exactly that many
     * characters (left-justified, space-filled or truncated), as a COBOL MOVE does.
     */
    private void addFieldToClass(TypeSpec.Builder classBuilder, String fieldName, String javaType, int textLength) {
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
                .addStatement("this.$L = $L", fieldName, fitTextExpression(fieldName, javaType, textLength))
                .build();
        classBuilder.addMethod(setter);
    }

    /** {@code value} fitted to {@code length} characters for a String field, unchanged otherwise. */
    private static String fitTextExpression(String value, String javaType, int length) {
        if (!"String".equals(javaType) || length <= 0) {
            return value;
        }
        return CodeBlock.of("$L == null ? $S : String.format($S, $L)", value, " ".repeat(length),
                "%-" + length + "." + length + "s", value).toString();
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


        // Limpiar caracteres especiales que no son válidos en nombres de clase Java (incluyendo apostrofes)
        // Primero quitar la extensión del archivo si existe
        String withoutExtension = input.replaceAll("\\.(cob|cobol|cbl|cpy)$", "");

        // Limpiar todos los caracteres especiales incluyendo apostrofes, guiones, espacios, etc.
        String cleaned = withoutExtension.replaceAll("[^a-zA-Z0-9]", " ");

        // Dividir por espacios múltiples y procesar cada parte
        String[] parts = cleaned.trim().split("\\s+");
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
            log.debug("Using the default generated type name");
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

        return finalResult;
    }

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false",
            "null", "var", "yield", "record", "sealed", "permits");

    /**
     * Lower-cases a CICS command into a valid Java method name, escaping reserved words.
     */
    private String javaMethodName(String command) {
        String name = command.toLowerCase(Locale.ROOT);
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
            name = "tx" + name;
        }
        if (JAVA_KEYWORDS.contains(name)) {
            return name + "_";
        }
        return name;
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

    private Path resolveActionItemOutputDir(Workspace workspace) {
        if (workspace.getMetadata() != null) {
            Object configured = workspace.getMetadata().get(ACTION_ITEM_OUTPUT_DIRECTORY_METADATA_KEY);
            if (configured != null && !configured.toString().isBlank()) {
                Path requested = Paths.get(configured.toString());
                return requested.isAbsolute()
                    ? requested.normalize()
                    : Paths.get(workspace.getPath()).resolve(requested).normalize();
            }
        }
        return resolveOutputDir(workspace);
    }
}
