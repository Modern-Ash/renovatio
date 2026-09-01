package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.SemanticProgram;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.emission.TargetStructure;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Aggregate project transformation used as the single preview/emission source. */
public final class ArchitectureTransformer {
    private final ModuleGroupingResolver groupingResolver;
    private final Map<MigrationProfile.ArchitectureStyle, ArchitectureProfile> profiles;
    private final Map<MigrationProfile.Language, ArtifactLayoutPlanner> layoutPlanners;

    public ArchitectureTransformer() {
        this(new ModuleGroupingResolver(), List.of(new TransactionScriptArchitectureProfile(),
                new HexagonalArchitectureProfile()), List.of());
    }

    public ArchitectureTransformer(List<ArtifactLayoutPlanner> layoutPlanners) {
        this(new ModuleGroupingResolver(), List.of(new TransactionScriptArchitectureProfile(),
                new HexagonalArchitectureProfile()), layoutPlanners);
    }

    public ArchitectureTransformer(ModuleGroupingResolver groupingResolver, List<ArchitectureProfile> profiles) {
        this(groupingResolver, profiles, List.of());
    }

    public ArchitectureTransformer(ModuleGroupingResolver groupingResolver, List<ArchitectureProfile> profiles,
                                   List<ArtifactLayoutPlanner> layoutPlanners) {
        this.groupingResolver = Objects.requireNonNull(groupingResolver, "groupingResolver");
        EnumMap<MigrationProfile.ArchitectureStyle, ArchitectureProfile> indexed =
                new EnumMap<>(MigrationProfile.ArchitectureStyle.class);
        Objects.requireNonNull(profiles, "profiles").forEach(profile -> {
            ArchitectureProfile previous = indexed.putIfAbsent(profile.style(), profile);
            if (previous != null) throw new IllegalArgumentException("duplicate architecture profile " + profile.style());
        });
        this.profiles = Map.copyOf(indexed);
        EnumMap<MigrationProfile.Language, ArtifactLayoutPlanner> planners =
                new EnumMap<>(MigrationProfile.Language.class);
        Objects.requireNonNull(layoutPlanners, "layoutPlanners").forEach(planner -> {
            ArtifactLayoutPlanner previous = planners.putIfAbsent(
                    Objects.requireNonNull(planner, "layoutPlanner").targetLanguage(), planner);
            if (previous != null) throw new IllegalArgumentException(
                    "duplicate artifact layout planner " + planner.targetLanguage());
        });
        this.layoutPlanners = Map.copyOf(planners);
    }

    public ArchitectureResult transform(ArchitectureRequest request) {
        Objects.requireNonNull(request, "request");
        MigrationProfile.ArchitectureStyle requested = request.effectiveProfile().profile().architecture().style();
        ArchitectureProfile profile = profiles.get(requested);
        if (profile == null) throw new ArchitectureStyleNotActiveException(requested, profiles.keySet().stream()
                .sorted(java.util.Comparator.comparing(Enum::name)).toList());

        ModuleGroupingResolver.GroupingResult grouping = groupingResolver.resolve(request);
        Map<String, String> moduleIds = new TreeMap<>();
        grouping.moduleByProgram().values().stream().distinct().sorted().forEach(name -> moduleIds.put(name,
                ArchitectureSupport.id(request.requestHash(), "MODULE", name, name, "module")));
        Map<String, List<String>> programsByModule = new TreeMap<>();
        grouping.moduleByProgram().forEach((program, module) ->
                programsByModule.computeIfAbsent(module, ignored -> new ArrayList<>()).add(program));
        List<ArchitectureGraph.Module> modules = programsByModule.entrySet().stream()
                .map(entry -> new ArchitectureGraph.Module(moduleIds.get(entry.getKey()), entry.getKey(), entry.getValue()))
                .toList();

        List<ArchitectureGraph.Component> components = new ArrayList<>();
        List<ArchitectureGraph.Relation> relations = new ArrayList<>();
        List<ArchitectureResult.Diagnostic> diagnostics = new ArrayList<>();
        List<ArchitectureResult.ArchitectedProgram> architected = new ArrayList<>();
        List<ArtifactManifest.Artifact> manifestArtifacts = new ArrayList<>();
        Map<String, String> moduleNamesById = new TreeMap<>();
        moduleIds.forEach((name, id) -> moduleNamesById.put(id, name));
        for (SemanticProgram program : request.programs()) {
            String moduleId = moduleIds.get(grouping.moduleByProgram().get(program.programId()));
            ArchitectureProfile.ProgramResult transformed = profile.transform(new ArchitectureProfile.ProgramContext(
                    request.requestHash(), moduleId, program, request.effectiveProfile()));
            components.addAll(transformed.components());
            relations.addAll(transformed.relations());
            diagnostics.addAll(transformed.diagnostics());
            List<String> componentIds = transformed.components().stream().map(ArchitectureGraph.Component::id).toList();
            List<String> codes = transformed.diagnostics().stream().map(ArchitectureResult.Diagnostic::code).toList();
            List<ArtifactManifest.Artifact> programArtifacts = planArtifacts(request, moduleId,
                    moduleNamesById.get(moduleId), program, transformed);
            manifestArtifacts.addAll(programArtifacts);
            List<String> artifactIds = programArtifacts.stream().map(ArtifactManifest.Artifact::id).toList();
            List<String> artifactPaths = programArtifacts.stream().map(ArtifactManifest.Artifact::path).toList();
            TargetStructure structure = new TargetStructure(TargetStructure.SCHEMA_VERSION, request.requestHash(),
                    moduleId, requested, transformed.effectiveStyle(), componentIds, artifactPaths, codes);
            TargetModel model = new TargetModel(program, request.effectiveProfile().profile(),
                    request.effectiveProfile().resolvedDecisions(), request.effectiveProfile().appliedDecisionIds(),
                    request.effectiveProfile().profileHash(), program.sourceProvenance(), structure);
            architected.add(new ArchitectureResult.ArchitectedProgram(program.programId(), moduleId, requested,
                    transformed.effectiveStyle(), model, componentIds, artifactIds));
        }

        return new ArchitectureResult(ArchitectureResult.SCHEMA_VERSION, request.requestHash(), architected,
                new ArchitectureGraph(modules, components, relations), new ArtifactManifest(manifestArtifacts), diagnostics);
    }

    private List<ArtifactManifest.Artifact> planArtifacts(ArchitectureRequest request, String moduleId,
                                                          String moduleName, SemanticProgram program,
                                                          ArchitectureProfile.ProgramResult transformed) {
        MigrationProfile.Language language = request.effectiveProfile().profile().target().language();
        ArtifactLayoutPlanner planner = layoutPlanners.get(language);
        if (planner == null) return List.of();
        List<ArtifactLayoutPlanner.PlannedArtifact> planned = List.copyOf(Objects.requireNonNull(planner.plan(
                new ArtifactLayoutPlanner.LayoutContext(request.requestHash(), moduleId, moduleName, program,
                        transformed.effectiveStyle(), transformed.components())), "artifact plan"));
        return planned.stream().map(value -> new ArtifactManifest.Artifact(
                ArchitectureSupport.id(request.requestHash(), "ARTIFACT", moduleId, program.programId(),
                        value.role()), value.path(), value.componentId(), moduleId, program.programId(), language,
                value.role())).toList();
    }

    public static final class ArchitectureStyleNotActiveException extends IllegalStateException {
        public static final String CODE = "ARCHITECTURE_STYLE_NOT_ACTIVE";
        private final MigrationProfile.ArchitectureStyle requestedStyle;
        private final List<MigrationProfile.ArchitectureStyle> activeStyles;

        public ArchitectureStyleNotActiveException(MigrationProfile.ArchitectureStyle requestedStyle,
                                                   List<MigrationProfile.ArchitectureStyle> activeStyles) {
            super(CODE + ": " + requestedStyle);
            this.requestedStyle = Objects.requireNonNull(requestedStyle, "requestedStyle");
            this.activeStyles = List.copyOf(activeStyles);
        }

        public String code() { return CODE; }
        public MigrationProfile.ArchitectureStyle requestedStyle() { return requestedStyle; }
        public List<MigrationProfile.ArchitectureStyle> activeStyles() { return activeStyles; }
    }
}
