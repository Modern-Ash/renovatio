package org.shark.renovatio.provider.cobol.pipeline;

import org.shark.renovatio.architecture.ArchitectureRequest;
import org.shark.renovatio.architecture.ArchitectureResult;
import org.shark.renovatio.architecture.ArchitectureTransformer;
import org.shark.renovatio.architecture.GroupingConfiguration;
import org.shark.renovatio.architecture.java.JavaArchitectureLayoutPlanner;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.domain.model.DomainModel;
import org.shark.renovatio.domain.model.SemanticDomainProjector;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionItem;
import org.shark.renovatio.provider.cobol.guardrail.ManualActionSeverity;
import org.shark.renovatio.provider.cobol.service.generation.JavaGenerationOrchestrator;
import org.shark.renovatio.provider.cobol.service.generation.JavaWriteService;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticProjector;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Production-backed COBOL-to-Java reference pipeline. */
public class CobolPipelineOrchestrator implements PipelineOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(CobolPipelineOrchestrator.class);
    private static final String GENERATED_PACKAGE = "src/main/java/org/shark/renovatio/generated/cobol";

    private final CobolParsingService parsingService;
    private final JavaGenerationOrchestrator generationOrchestrator;
    private final MavenBuildService buildService;
    private final EquivalenceChecker equivalenceChecker;
    private final CobolIntermediateModelService intermediateModelService;
    private final CobolSemanticProjector semanticProjector;
    private final SemanticDomainProjector domainProjector;
    private final ArchitectureTransformer architectureTransformer;

    public CobolPipelineOrchestrator(CobolParsingService parsingService,
                                     JavaGenerationOrchestrator generationOrchestrator,
                                     CobolSemanticTranspiler semanticTranspiler,
                                     MavenBuildService buildService,
                                     EquivalenceChecker equivalenceChecker) {
        this.parsingService = java.util.Objects.requireNonNull(parsingService, "parsingService");
        this.generationOrchestrator = java.util.Objects.requireNonNull(generationOrchestrator,
            "generationOrchestrator");
        java.util.Objects.requireNonNull(semanticTranspiler, "semanticTranspiler");
        this.buildService = java.util.Objects.requireNonNull(buildService, "buildService");
        this.equivalenceChecker = java.util.Objects.requireNonNull(equivalenceChecker,
            "equivalenceChecker");
        this.intermediateModelService = new CobolIntermediateModelService();
        this.semanticProjector = new CobolSemanticProjector();
        this.domainProjector = new SemanticDomainProjector();
        this.architectureTransformer = new ArchitectureTransformer(
            List.of(new JavaArchitectureLayoutPlanner()));
    }

    @Override
    public PipelineResult execute(PipelineRequest request) {
        Instant started = Instant.now();
        PipelineContext context = new PipelineContext();
        SemanticGapTracker gaps = new SemanticGapTracker();
        log.info("Starting pipeline for fixture: {}", request.fixtureId());

        context.discover = executeStage("discover", () -> {
            String actualHash = SourceSnapshot.capture(request.fixtureDir());
            if (!request.sourceSnapshotHash().equals(actualHash)) {
                gaps.recordGap(ActionItem.blocking("STALE_SOURCE", request.fixtureDir().toString(), "",
                    "Source snapshot is stale: expected " + request.sourceSnapshotHash()
                        + " but found " + actualHash,
                    "Create a new pipeline request from the changed fixture before executing"));
                throw new IllegalStateException("STALE_SOURCE: fixture inputs changed after request creation");
            }
            context.cobolFiles = parsingService.findCobolSourceFiles(request.fixtureDir()).stream()
                .sorted().toList();
            context.copybooks = parsingService.findCopybooks(request.fixtureDir()).stream()
                .sorted().toList();
            if (context.cobolFiles.isEmpty()) {
                throw new IllegalStateException("No COBOL source files found");
            }
            return "Discovered " + context.cobolFiles.size() + " COBOL source file(s) and "
                + context.copybooks.size() + " copybook(s); snapshot=" + actualHash;
        });
        if (!context.discover.success()) return failed(request, started, context, gaps);

        context.parse = executeStage("parse", () -> {
            for (Path source : context.cobolFiles) {
                parsingService.parseCobolFile(source);
                context.models.put(source, intermediateModelService.parse(source));
            }
            return "Parsed " + context.models.size() + " source file(s) into COBOL IR";
        });
        if (!context.parse.success()) return failed(request, started, context, gaps);

        context.semanticIr = executeStage("semanticIr", () -> {
            for (Map.Entry<Path, CobolIntermediateModel> entry : context.models.entrySet()) {
                Path source = entry.getKey();
                SemanticProgram semantic = semanticProjector.project(entry.getValue(),
                    relativeSource(request.fixtureDir(), source), Files.readAllBytes(source),
                    Optional.of(parsingService.getDefaultDialect().name()), Optional.empty());
                context.semanticPrograms.add(semantic);
                semantic.unclassifiedDataAccesses().forEach(access -> recordSemanticGap(
                    semantic, access, context, gaps));
            }
            return "Projected " + context.semanticPrograms.size() + " Semantic IR program(s) with "
                + context.semanticPrograms.stream().mapToInt(value -> 1 + value.types().size()
                    + value.dataIntents().size() + value.sideEffects().size()
                    + value.ioOperations().size() + value.controlFlow().nodes().size()
                    + value.controlFlow().edges().size() + value.unclassifiedDataAccesses().size()).sum()
                + " node(s)";
        });
        if (!context.semanticIr.success()) return failed(request, started, context, gaps);

        context.domainModelStage = executeStage("domainModel", () -> {
            context.effectiveProfile = effectiveProfile(request);
            context.architectureRequest = ArchitectureRequest.create(context.semanticPrograms,
                context.effectiveProfile, GroupingConfiguration.empty(), copybooksByProgram(context),
                evidenceHashes(context.semanticPrograms));
            context.domainModel = domainProjector.project(context.architectureRequest.requestHash(),
                context.semanticPrograms);
            return "Projected Domain Model hash=" + context.domainModel.canonicalHash() + " with "
                + context.domainModel.nodes().size() + " node(s)";
        });
        if (!context.domainModelStage.success()) return failed(request, started, context, gaps);

        context.decisions = executeStage("decisions", () -> {
            return "Resolved Java " + context.effectiveProfile.profile().target().languageVersion()
                + " profile hash=" + context.effectiveProfile.profileHash() + " with "
                + context.effectiveProfile.resolvedDecisions().size() + " decision(s)";
        });
        if (!context.decisions.success()) return failed(request, started, context, gaps);

        context.architectureStage = executeStage("architecture", () -> {
            context.architecture = architectureTransformer.transform(context.architectureRequest);
            if (!context.domainModel.canonicalHash().equals(
                    context.architecture.domainModel().canonicalHash())) {
                throw new IllegalStateException("Domain projection diverged from canonical architecture");
            }
            return "Projected Architecture Model hash="
                + context.architecture.architectureModel().canonicalHash() + " with "
                + context.architecture.architectureModel().components().size() + " component(s)";
        });
        if (!context.architectureStage.success()) return failed(request, started, context, gaps);

        context.manifest = executeStage("manifest", () -> {
            if (context.architecture.manifest().artifacts().isEmpty()) {
                throw new IllegalStateException("Canonical artifact manifest is empty");
            }
            return "Produced manifest hash=" + context.architecture.manifestHash() + " with "
                + context.architecture.manifest().artifacts().size() + " artifact(s)";
        });
        if (!context.manifest.success()) return failed(request, started, context, gaps);

        context.emit = executeStage("emit", () -> {
            StubResult generated = generationOrchestrator.generateInterfaceStubs(
                createGenerationQuery(request), createGenerationWorkspace(request),
                context.effectiveProfile, context.architecture.manifestHash());
            if (generated == null || !generated.isSuccess()) {
                throw new IllegalStateException("Java emission failed: "
                    + (generated == null ? "no result" : generated.getMessage()));
            }
            Map<String, String> code = generated.getGeneratedCode();
            if (code == null || code.isEmpty() || countJavaFiles(request.outputDir()) == 0) {
                throw new IllegalStateException("Java emission produced no source files");
            }
            collectGenerationActionItems(generated, gaps);
            code.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                if (containsUnresolvedMarker(entry.getValue())) {
                    gaps.recordGap(ActionItem.blocking("UNRESOLVED_GENERATION_MARKER", entry.getKey(), "",
                        "Generated source contains TODO or unsupported placeholder behavior",
                        "Resolve the associated manual action before accepting the migration"));
                }
                if (entry.getValue() != null && entry.getValue().contains("COBOL not translated")) {
                    gaps.recordGap(ActionItem.warning("UNTRANSLATED_GENERATED_CODE", entry.getKey(), "",
                        "Generated source retains explicitly untranslated COBOL statements",
                        "Review the stable manual-action report before accepting migrated behavior"));
                }
            });
            return "Emitted " + code.size() + " Java file(s) from canonical manifest "
                + context.architecture.manifestHash();
        });
        if (!context.emit.success()) return failed(request, started, context, gaps);

        context.build = executeStage("build", () -> {
            MavenBuildService.BuildResult build = buildService.compile(request.outputDir(),
                "org.projectlombok:lombok:1.18.30");
            if (!build.success()) {
                throw new IllegalStateException("Build failed: " + build.errors());
            }
            return "Compiled generated Java with Java 21 in " + build.compilationTimeMs() + "ms";
        });
        if (!context.build.success()) return failed(request, started, context, gaps);

        if (request.verifyEquivalence() && request.expectedDir() != null) {
            context.equivalenceReports = equivalenceChecker.checkAll(request.fixtureId(),
                request.outputDir(), request.expectedDir(), EquivalenceChecker.EquivalenceConfig.defaults());
            context.equivalence = context.equivalenceReports.stream()
                .filter(report -> !report.isPassed()).findFirst()
                .orElseGet(() -> context.equivalenceReports.stream().findFirst().orElse(null));
        }

        boolean successful = !gaps.hasBlockingGaps()
            && context.equivalenceReports.stream().allMatch(EquivalenceReport::isPassed);
        return result(request, started, Instant.now(), context, gaps.getActionItems(), successful);
    }

    @Override
    public List<PipelineResult> executeAll(List<PipelineRequest> requests) {
        return requests.stream().map(this::execute).toList();
    }

    private PipelineResult failed(PipelineRequest request, Instant started, PipelineContext context,
                                  SemanticGapTracker gaps) {
        return result(request, started, Instant.now(), context, gaps.getActionItems(), false);
    }

    private PipelineResult result(PipelineRequest request, Instant started, Instant ended,
                                  PipelineContext context, List<ActionItem> gaps, boolean success) {
        return new PipelineResult(request.fixtureId(), request.outputDir(), started, ended,
            Duration.between(started, ended), context.discover, context.parse, context.semanticIr,
            context.domainModelStage, context.decisions, context.architectureStage, context.manifest,
            context.emit, context.build, context.equivalence, context.equivalenceReports, gaps, success);
    }

    private MigrationProfiles.EffectiveProfile effectiveProfile(PipelineRequest request) {
        MigrationProfile.ArchitectureStyle style = architectureStyle(request);
        MigrationProfile overlay = new MigrationProfile(MigrationProfiles.SCHEMA_VERSION, Map.of(),
            new MigrationProfile.Target(MigrationProfile.Language.JAVA, "21"),
            new MigrationProfile.Architecture(style, MigrationProfile.ModuleGrouping.BY_PROGRAM),
            null, null, null, null);
        TreeMap<String, String> decisions = new TreeMap<>();
        request.decisions().forEach((key, value) -> decisions.put(key,
            value instanceof String text ? text : MigrationProfiles.canonical(value)));
        return MigrationProfiles.effective(overlay, decisions, Map.of(), List.of());
    }

    private MigrationProfile.ArchitectureStyle architectureStyle(PipelineRequest request) {
        Object configured = request.decisions().get("architectureStyle");
        if (configured != null) {
            return MigrationProfile.ArchitectureStyle.valueOf(configured.toString().toUpperCase());
        }
        String id = request.fixtureId().toLowerCase();
        if (id.contains("cics")) return MigrationProfile.ArchitectureStyle.LAYERED_MVC;
        if (id.contains("db2")) return MigrationProfile.ArchitectureStyle.LAYERED;
        return MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT;
    }

    private Map<String, List<String>> copybooksByProgram(PipelineContext context) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<Path, CobolIntermediateModel> entry : context.models.entrySet()) {
            result.put(entry.getValue().getProgramId(),
                parsingService.extractCopybookReferences(entry.getKey()));
        }
        return result;
    }

    private List<String> evidenceHashes(List<SemanticProgram> programs) {
        return programs.stream().flatMap(program -> program.sourceProvenance()
            .parentEvidenceHashes().stream()).distinct().sorted().toList();
    }

    private void recordSemanticGap(SemanticProgram program,
                                   SemanticProgram.UnclassifiedDataAccess access,
                                   PipelineContext context, SemanticGapTracker gaps) {
        String description = access.observedOperation() + " " + access.subject() + ": " + access.reason();
        if (isDb2ImplicitRegister(access.subject())) {
            gaps.recordGap(ActionItem.warning("DB2_IMPLICIT_REGISTER",
                program.sourceProvenance().sourcePath(), "", description,
                "Map the DB2 status register through the persistence adapter"));
        } else if (isDeclaredByCopybook(access.subject(), context.copybooks)) {
            gaps.recordGap(ActionItem.warning("COPYBOOK_DATA_ACCESS",
                program.sourceProvenance().sourcePath(), "", description,
                "Bind the referenced copybook field through the generated data model"));
        } else {
            gaps.recordGap(ActionItem.blocking("UNCLASSIFIED_DATA_ACCESS",
                program.sourceProvenance().sourcePath(), "", description,
                "Classify the referenced data before accepting generated behavior"));
        }
    }

    private boolean isDb2ImplicitRegister(String subject) {
        return subject != null && (subject.equalsIgnoreCase("SQLCODE")
            || subject.equalsIgnoreCase("SQLSTATE"));
    }

    private boolean isDeclaredByCopybook(String subject, List<Path> copybooks) {
        if (subject == null || subject.isBlank() || copybooks.isEmpty()) return false;
        List<String> names = java.util.Arrays.stream(subject.toUpperCase().split("\\s+(?:IN|OF)\\s+"))
            .map(String::strip).filter(value -> !value.isEmpty()).toList();
        try {
            String declarations = copybooks.stream().map(path -> {
                try {
                    return Files.readString(path).toUpperCase();
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
            }).collect(java.util.stream.Collectors.joining("\n"));
            return !names.isEmpty() && names.stream().allMatch(name ->
                java.util.regex.Pattern.compile("(?<![A-Z0-9-])"
                    + java.util.regex.Pattern.quote(name) + "(?![A-Z0-9-])")
                    .matcher(declarations).find());
        } catch (java.io.UncheckedIOException exception) {
            return false;
        }
    }

    private NqlQuery createGenerationQuery(PipelineRequest request) {
        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("stubs");
        query.setLanguage("cobol");
        query.setParameters(request.decisions());
        return query;
    }

    private Workspace createGenerationWorkspace(PipelineRequest request) {
        Workspace workspace = new Workspace(request.fixtureId(), request.fixtureDir().toString(), "main");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(JavaWriteService.OUTPUT_DIRECTORY_METADATA_KEY,
            request.outputDir().toAbsolutePath().normalize().resolve(GENERATED_PACKAGE).toString());
        metadata.put(JavaGenerationService.ACTION_ITEM_OUTPUT_DIRECTORY_METADATA_KEY,
            request.outputDir().toAbsolutePath().normalize().toString());
        workspace.setMetadata(metadata);
        return workspace;
    }

    private void collectGenerationActionItems(StubResult generated, SemanticGapTracker gaps) {
        if (generated.getMetadata() == null) return;
        Object value = generated.getMetadata().get("manualActionItems");
        if (!(value instanceof List<?> items)) return;
        items.stream().filter(ManualActionItem.class::isInstance)
            .map(ManualActionItem.class::cast).forEach(item -> {
                String location = item.sourceSpan() == null || item.sourceSpan().isBlank()
                    ? item.sourceFile() : item.sourceFile() + ":" + item.sourceSpan();
                String suggestion = item.requiredHumanAction() + "; acceptance: "
                    + item.acceptanceCondition();
                ActionItem action = item.severity() == ManualActionSeverity.WARNING
                    ? ActionItem.warning(item.constructionFamily(), location, item.paragraph(),
                        item.reason(), suggestion)
                    : ActionItem.blocking(item.constructionFamily(), location, item.paragraph(),
                        item.reason(), suggestion);
                gaps.recordGap(action);
            });
    }

    private long countJavaFiles(Path directory) throws IOException {
        if (!Files.exists(directory)) return 0;
        try (var stream = Files.walk(directory)) {
            return stream.filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java")).count();
        }
    }

    private String relativeSource(Path fixture, Path source) {
        return fixture.toAbsolutePath().normalize().relativize(source.toAbsolutePath().normalize())
            .toString().replace('\\', '/');
    }

    private boolean containsUnresolvedMarker(String source) {
        return source != null && (source.contains("TODO")
            || source.contains("UnsupportedOperationException") || source.contains("FIXME"));
    }

    private StageResult executeStage(String name, StageOperation operation) {
        Instant started = Instant.now();
        try {
            String output = operation.execute();
            return StageResult.success(name, started, Instant.now(), output);
        } catch (Exception exception) {
            log.error("Stage {} failed: {}", name, exception.getMessage());
            return StageResult.failure(name, started, Instant.now(),
                List.of(exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage()));
        }
    }

    @FunctionalInterface
    private interface StageOperation {
        String execute() throws Exception;
    }

    private static final class PipelineContext {
        private List<Path> cobolFiles = List.of();
        private List<Path> copybooks = List.of();
        private final Map<Path, CobolIntermediateModel> models = new LinkedHashMap<>();
        private final List<SemanticProgram> semanticPrograms = new ArrayList<>();
        private DomainModel domainModel;
        private MigrationProfiles.EffectiveProfile effectiveProfile;
        private ArchitectureResult architecture;
        private ArchitectureRequest architectureRequest;
        private StageResult discover;
        private StageResult parse;
        private StageResult semanticIr;
        private StageResult domainModelStage;
        private StageResult decisions;
        private StageResult architectureStage;
        private StageResult manifest;
        private StageResult emit;
        private StageResult build;
        private EquivalenceReport equivalence;
        private List<EquivalenceReport> equivalenceReports = List.of();
    }
}
