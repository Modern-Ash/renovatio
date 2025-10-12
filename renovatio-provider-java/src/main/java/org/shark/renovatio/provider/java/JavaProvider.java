package org.shark.renovatio.provider.java;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.shark.renovatio.provider.java.adapter.OpenRewriteAnalyzeAdapter;
import org.shark.renovatio.provider.java.adapter.OpenRewriteApplyAdapter;
import org.shark.renovatio.provider.java.discovery.OpenRewriteRecipeDiscoveryService;
import org.shark.renovatio.provider.java.discovery.OpenRewriteRecipeDiscoveryService.RecipeInfo;
import org.shark.renovatio.provider.java.execution.JavaRecipeExecutionResult;
import org.shark.renovatio.provider.java.execution.JavaRecipeExecutor;
import org.shark.renovatio.provider.java.planner.JavaPlan;
import org.shark.renovatio.provider.java.planner.JavaRefactorPlanner;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.BaseLanguageProvider;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Modern Java MCP provider exposing a curated set of OpenRewrite-based tools.
 */
public class JavaProvider extends BaseLanguageProvider {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JavaProvider.class);

    // Common identifiers
    private static final String LANGUAGE_ID = "java";

    // Default scope
    private static final String DEFAULT_SCOPE_PATTERN = "**/*.java";
    private static final List<String> DEFAULT_SCOPE = List.of(DEFAULT_SCOPE_PATTERN);

    // Tool names (fully qualified)
    private static final String TOOL_DISCOVER = "java.discover";
    private static final String TOOL_ANALYZE = "java.analyze";
    private static final String TOOL_PLAN = "java.plan";
    private static final String TOOL_APPLY = "java.apply";
    private static final String TOOL_DIFF = "java.diff";
    private static final String TOOL_REVIEW = "java.review";
    private static final String TOOL_FORMAT = "java.format";
    private static final String TOOL_TEST = "java.test";
    private static final String TOOL_METRICS = "java.metrics";
    private static final String TOOL_RECIPE_LIST = "java.recipe_list";
    private static final String TOOL_RECIPE_DESCRIBE = "java.recipe_describe";
    private static final String TOOL_PIPELINE = "java.pipeline";

    // Capability identifiers (lowercase, unqualified)
    private static final String CAP_DISCOVER = "discover";
    private static final String CAP_ANALYZE = "analyze";
    private static final String CAP_PLAN = "plan";
    private static final String CAP_APPLY = "apply";
    private static final String CAP_DIFF = "diff";
    private static final String CAP_REVIEW = "review";
    private static final String CAP_FORMAT = "format";
    private static final String CAP_TEST = "test";
    private static final String CAP_RECIPE_LIST = "recipe_list";
    private static final String CAP_RECIPE_DESCRIBE = "recipe_describe";
    private static final String CAP_PIPELINE = "pipeline";

    // Argument / field keys
    private static final String KEY_TYPE = "type";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_MODULES = "modules";
    private static final String KEY_DEPENDENCIES = "dependencies";
    private static final String KEY_FILES = "files";
    private static final String KEY_CHANGES = "changes";
    private static final String KEY_ISSUES = "issues";
    private static final String KEY_METRICS = "metrics";
    private static final String KEY_RECIPES = "recipes";
    private static final String KEY_RUN_ID = "runId";
    private static final String KEY_DRY_RUN = "dryRun";
    private static final String KEY_CHECKPOINT_REF = "checkpointRef";
    private static final String KEY_SUMMARY_MD = "summaryMarkdown";
    private static final String KEY_HIGHLIGHTS = "highlights";
    private static final String KEY_PASSED = "passed";
    private static final String KEY_FAILED = "failed";
    private static final String KEY_FAILURES = "failures";
    private static final String KEY_DETAILS = "details";
    private static final String KEY_PLAN_ID = "planId";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_GOALS = "goals";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_CREATED_AT = "createdAt";
    private static final String KEY_WORKSPACE_PATH = "workspacePath";
    private static final String KEY_FROM_REF = "fromRef";
    private static final String KEY_TO_REF = "toRef";
    private static final String KEY_NAME = "name";
    private static final String KEY_PRESET = "preset";
    // Additional common keys
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_TAGS = "tags";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_RECIPE = "recipe";
    private static final String KEY_LINE = "line";
    private static final String KEY_SEVERITY = "severity";
    private static final String KEY_ANALYZED_FILES = "analyzedFiles";
    private static final String KEY_SUMMARY = "summary";

    // Nested object keys used in maps
    private static final String KEY_FILE = "file";
    private static final String KEY_DIFF = "diff";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_ARTIFACT_ID = "artifactId";
    private static final String KEY_VERSION = "version";

    // Misc
    private static final String METADATA_OUTPUT_SCHEMA = "outputSchema";
    private static final String GIT_HEAD = "HEAD";
    private static final String COMMIT_PREFIX = "Renovatio: ";

    // Profiles and presets
    private static final String PROFILE_QUALITY = "quality";
    private static final String PROFILE_STYLE = "style";
    private static final String PROFILE_MODERNIZE_JAVA17 = "modernize_java17";
    private static final String PROFILE_SECURITY = "security";
    private static final String PROFILE_TESTING_SUPPORT = "testing_support";
    private static final String PROFILE_ALL = "all";
    private static final List<String> PROFILES_ENUM = List.of(
            PROFILE_QUALITY, PROFILE_STYLE, PROFILE_MODERNIZE_JAVA17, PROFILE_SECURITY, PROFILE_TESTING_SUPPORT, PROFILE_ALL
    );

    private static final String PRESET_CLEANUP_STYLE = "cleanup_style";
    private static final String PRESET_REMOVE_DEPRECATIONS = "remove_deprecations";
    private static final String PRESET_FORMAT_ONLY = "format_only";
    private static final List<String> PRESETS_ENUM = List.of(
            PROFILE_MODERNIZE_JAVA17, PRESET_CLEANUP_STYLE, PRESET_REMOVE_DEPRECATIONS, PRESET_FORMAT_ONLY
    );

    // Recipe constants
    private static final String RECIPE_AUTO_FORMAT = "org.openrewrite.java.format.AutoFormat";
    private static final String RECIPE_REMOVE_UNUSED_IMPORTS = "org.openrewrite.java.cleanup.RemoveUnusedImports";

    private final OpenRewriteRecipeDiscoveryService discoveryService;
    private final JavaRefactorPlanner planner;
    private final JavaRecipeExecutor executor;
    private final OpenRewriteAnalyzeAdapter analyzeAdapter;
    private final OpenRewriteApplyAdapter applyAdapter;

    private final Map<String, JavaRecipeExecutionResult> executions = new ConcurrentHashMap<>();
    private final Map<String, String> checkpoints = new ConcurrentHashMap<>();

    public JavaProvider(OpenRewriteRecipeDiscoveryService discoveryService,
                        JavaRefactorPlanner planner,
                        JavaRecipeExecutor executor,
                        OpenRewriteAnalyzeAdapter analyzeAdapter,
                        OpenRewriteApplyAdapter applyAdapter) {
        this.discoveryService = discoveryService;
        this.planner = planner;
        this.executor = executor;
        this.analyzeAdapter = analyzeAdapter;
        this.applyAdapter = applyAdapter;
    }

    @Override
    public String language() {
        return LANGUAGE_ID;
    }

    @Override
    public Set<Capabilities> capabilities() {
        return EnumSet.of(Capabilities.ANALYZE, Capabilities.PLAN, Capabilities.APPLY, Capabilities.METRICS, Capabilities.DIFF);
    }

    @Override
    public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
        try {
            Map<String, Object> params = optionalParameters(query);
            String profile = stringParam(params, KEY_PROFILE, PROFILE_QUALITY);
            List<String> goals = combineLists(listParam(params, KEY_GOALS), List.of(profile));
            List<String> include = combineLists(listParam(params, KEY_INCLUDE), listParam(params, KEY_RECIPES));
            List<String> exclude = combineLists(listParam(params, KEY_EXCLUDE), listParam(params, KEY_EXCLUDE_RECIPES));
            int maxFindings = intParam(params, KEY_MAX_FINDINGS, 200);

            List<String> recipes = sanitizeRecipes(planner.resolveRecipes(goals, include, exclude));
            List<String> scopePatterns = listParam(params, KEY_SCOPE);
            if (scopePatterns.isEmpty()) {
                scopePatterns = DEFAULT_SCOPE;
            }

            logger.info("[analyze] Workspace: {} | Recipes: {} | Scope: {}", workspace.getPath(), recipes, scopePatterns);

            JavaRecipeExecutionResult execution = executor.preview(workspace.getPath(), recipes, scopePatterns);
            AnalyzeResult analyzeResult = analyzeAdapter.adapt(execution, workspace, profile, maxFindings);
            executions.put(analyzeResult.getRunId(), execution);
            return analyzeResult;
        } catch (Exception e) {
            logger.error("Error in JavaProvider.analyze: {}", e.getMessage(), e);
            AnalyzeResult errorResult = new AnalyzeResult();
            errorResult.setSuccess(false);
            errorResult.setMessage("JavaProvider.analyze failed: " + e.getMessage());
            return errorResult;
        }
    }

    @Override
    public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
        Map<String, Object> params = optionalParameters(query);
        String profile = stringParam(params, KEY_PROFILE, null);
        List<String> goals = combineLists(listParam(params, KEY_GOALS), profile != null ? List.of(profile) : List.of());
        goals = goals.stream().filter(Objects::nonNull).filter(goal -> !goal.isBlank()).toList();
        List<String> include = combineLists(listParam(params, KEY_INCLUDE), listParam(params, KEY_INCLUDE_RECIPES));
        List<String> exclude = combineLists(listParam(params, KEY_EXCLUDE), listParam(params, KEY_EXCLUDE_RECIPES));
        List<String> scopePatterns = scope != null && !CollectionUtils.isEmpty(scope.getIncludePatterns())
                ? scope.getIncludePatterns()
                : DEFAULT_SCOPE;

        JavaPlan plan = planner.createPlan(workspace.getPath(), goals, include, exclude, scopePatterns);
        PlanResult result = new PlanResult(true, String.format(Locale.ROOT,
                "Generated plan %s with %d recipe(s)", plan.id(), plan.recipes().size()));
        result.setPlanId(plan.id());
        result.setRunId(plan.id());

        Map<String, Object> steps = new LinkedHashMap<>();
        steps.put(KEY_STEPS, planner.describePlanSteps(plan));
        steps.put(KEY_RECIPES, plan.recipes());
        steps.put(KEY_GOALS, plan.goals());
        steps.put(KEY_SCOPE, plan.scope());
        result.setSteps(steps);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(KEY_PLAN_ID, plan.id());
        metadata.put(KEY_CREATED_AT, plan.createdAt().toString());
        metadata.put(KEY_WORKSPACE_PATH, plan.workspacePath());
        result.setMetadata(metadata);
        return result;
    }

    @Override
    public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put(KEY_WORKSPACE_PATH, workspace != null ? workspace.getPath() : null);
        args.put(KEY_PLAN_ID, planId);
        args.put(KEY_DRY_RUN, dryRun);
        Map<String, Object> response = handleApply(args);
        if (!Boolean.TRUE.equals(response.get(KEY_SUCCESS))) {
            return new ApplyResult(false, Objects.toString(response.get(KEY_ERROR), "Apply failed"));
        }
        JavaRecipeExecutionResult execution = executions.get(response.get(KEY_RUN_ID));
        ApplyResult applyResult = applyAdapter.adapt(execution, dryRun, checkpoints.get(response.get(KEY_RUN_ID)));
        applyResult.setRunId((String) response.get(KEY_RUN_ID));
        applyResult.setMessage((String) response.getOrDefault(KEY_MESSAGE, execution.summary()));
        return applyResult;
    }

    @Override
    public DiffResult diff(String runId, Workspace workspace) {
        JavaRecipeExecutionResult execution = executions.get(runId);
        DiffResult result;
        if (execution == null) {
            result = new DiffResult(false, "No execution found for runId " + runId);
        } else {
            result = new DiffResult(true, execution.summary());
            result.setUnifiedDiff(execution.changes().stream()
                    .map(change -> change.diff() + System.lineSeparator())
                    .collect(Collectors.joining()));
        }
        result.setRunId(runId);
        return result;
    }

    @Override
    public MetricsResult metrics(Scope scope, Workspace workspace) {
        List<String> scopePatterns = scope != null && !CollectionUtils.isEmpty(scope.getIncludePatterns())
                ? scope.getIncludePatterns()
                : DEFAULT_SCOPE;
        JavaRecipeExecutionResult execution = executor.preview(workspace.getPath(), List.of(), scopePatterns);
        MetricsResult metricsResult = new MetricsResult(execution.success(), execution.summary());
        Map<String, Number> metrics = new LinkedHashMap<>();
        execution.metrics().forEach((key, value) -> {
            if (value instanceof Number number) {
                metrics.put(key, number);
            }
        });
        metricsResult.setMetrics(metrics);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(KEY_RECIPES, execution.recipes());
        details.put(KEY_ANALYZED_FILES, execution.analyzedFiles());
        metricsResult.setDetails(details);
        return metricsResult;
    }

    @Override
    public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
        return Optional.empty();
    }

    @Override
    public List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();
        tools.add(createTool(TOOL_DISCOVER, "Inspect workspace structure", workspaceSchema(), discoverOutputSchema()));
        tools.add(createTool(TOOL_ANALYZE, "Analyze Java sources with OpenRewrite", analyzeSchema(), analyzeOutputSchema()));
        tools.add(createTool(TOOL_PLAN, "Plan refactoring based on goals", planSchema(), planOutputSchema()));
        tools.add(createTool(TOOL_APPLY, "Apply OpenRewrite recipes", applySchema(), applyOutputSchema()));
        tools.add(createTool(TOOL_DIFF, "Generate git diff between revisions", diffSchema(), diffOutputSchema()));
        tools.add(createTool(TOOL_REVIEW, "Summarize refactoring outcome", reviewSchema(), reviewOutputSchema()));
        tools.add(createTool(TOOL_FORMAT, "Format sources and remove unused imports", formatSchema(), applyOutputSchema()));
        tools.add(createTool(TOOL_TEST, "Run project tests", testSchema(), testOutputSchema()));
        tools.add(createTool(TOOL_METRICS, "Collect high level metrics", metricsSchema(), metricsOutputSchema()));
        tools.add(createTool(TOOL_RECIPE_LIST, "List available OpenRewrite recipes", Map.of("type", "object"), recipeListOutputSchema()));
        tools.add(createTool(TOOL_RECIPE_DESCRIBE, "Describe a specific recipe", recipeDescribeSchema(), recipeDescribeOutputSchema()));
        tools.add(createTool(TOOL_PIPELINE, "Execute preset modernization pipeline", pipelineSchema(), pipelineOutputSchema()));
        return tools;
    }

    @Override
    public Map<String, Object> executeExtendedTool(String capability, Map<String, Object> arguments) {
        if (capability == null) {
            return null;
        }
        return switch (capability.toLowerCase(Locale.ROOT)) {
            case CAP_DISCOVER -> handleDiscover(arguments);
            case CAP_APPLY -> handleApply(arguments);
            case CAP_DIFF -> handleDiff(arguments);
            case CAP_REVIEW -> handleReview(arguments);
            case CAP_FORMAT -> handleFormat(arguments);
            case CAP_TEST -> handleTest(arguments);
            case CAP_RECIPE_LIST -> handleRecipeList(arguments);
            case CAP_RECIPE_DESCRIBE -> handleRecipeDescribe(arguments);
            case CAP_PIPELINE -> handlePipeline(arguments);
            default -> null;
        };
    }

    private Map<String, Object> handleDiscover(Map<String, Object> arguments) {
        Path workspace = workspace(arguments);
        Map<String, Object> result = baseResponse(CAP_DISCOVER);
        if (workspace == null) {
            return error(result, KEY_WORKSPACE_PATH + " is required");
        }
        try {
            result.put(KEY_MODULES, discoverModules(workspace));
            result.put(KEY_DEPENDENCIES, discoverDependencies(workspace));
            result.put(KEY_FILES, listFiles(workspace, 200));
            result.put(KEY_MESSAGE, String.format(Locale.ROOT, "Discovered %d module(s)", ((List<?>) result.get(KEY_MODULES)).size()));
            return success(result);
        } catch (IOException ex) {
            return error(result, ex.getMessage());
        }
    }

    private Map<String, Object> handleApply(Map<String, Object> arguments) {
        Path workspace = workspace(arguments);
        Map<String, Object> result = baseResponse(CAP_APPLY);
        if (workspace == null) {
            return error(result, KEY_WORKSPACE_PATH + " is required");
        }

        String planId = stringParam(arguments, KEY_PLAN_ID, null);
        List<String> recipes = listParam(arguments, KEY_RECIPES);
        boolean dryRun = booleanParam(arguments, KEY_DRY_RUN, true);
        List<String> scope = listParam(arguments, KEY_SCOPE);

        if (planId != null) {
            Optional<JavaPlan> plan = planner.findPlan(planId);
            if (plan.isPresent()) {
                if (recipes.isEmpty()) {
                    recipes = plan.get().recipes();
                }
                if (scope.isEmpty()) {
                    scope = plan.get().scope();
                }
            }
        }
        if (recipes.isEmpty()) {
            return error(result, "No recipes specified. Provide planId or recipes[]");
        }
        if (scope.isEmpty()) {
            scope = DEFAULT_SCOPE;
        }

        List<String> sanitizedRecipes = sanitizeRecipes(recipes);
        if (sanitizedRecipes.isEmpty()) {
            return error(result, "All requested recipes require configuration. Provide safe recipes or a curated profile");
        }

        JavaRecipeExecutionResult execution = executor.apply(workspace.toString(), sanitizedRecipes, scope, dryRun);
        String runId = planId != null ? planId + "-run" : generateRunId();
        executions.put(runId, execution);
        result.put(KEY_RUN_ID, runId);
        result.put(KEY_DRY_RUN, dryRun);
        result.put(KEY_CHANGES, execution.changes());
        result.put(KEY_ISSUES, execution.issues());
        result.put(KEY_METRICS, execution.metrics());
        result.put(KEY_RECIPES, execution.recipes());
        result.put(KEY_MESSAGE, execution.summary());

        if (!execution.success()) {
            return error(result, execution.summary());
        }

        if (!dryRun) {
            String checkpoint = createCheckpoint(workspace, execution.summary());
            if (checkpoint != null) {
                checkpoints.put(runId, checkpoint);
                result.put(KEY_CHECKPOINT_REF, checkpoint);
            }
        }
        return success(result);
    }

    private Map<String, Object> handleDiff(Map<String, Object> arguments) {
        Path workspace = workspace(arguments);
        Map<String, Object> result = baseResponse(CAP_DIFF);
        if (workspace == null) {
            return error(result, KEY_WORKSPACE_PATH + " is required");
        }
        String fromRef = stringParam(arguments, KEY_FROM_REF, null);
        String toRef = stringParam(arguments, KEY_TO_REF, GIT_HEAD);
        try {
            List<Map<String, Object>> changes = gitDiff(workspace, fromRef, toRef);
            result.put(KEY_CHANGES, changes);
            result.put(KEY_MESSAGE, String.format(Locale.ROOT, "Generated diff with %d change(s)", changes.size()));
            return success(result);
        } catch (Exception ex) {
            return error(result, ex.getMessage());
        }
    }

    private Map<String, Object> handleReview(Map<String, Object> arguments) {
        Map<String, Object> diff = handleDiff(arguments);
        Map<String, Object> result = baseResponse(CAP_REVIEW);
        if (Boolean.FALSE.equals(diff.get(KEY_SUCCESS))) {
            return diff;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> changes = (List<Map<String, Object>>) diff.getOrDefault(KEY_CHANGES, List.of());
        List<Map<String, Object>> highlights = new ArrayList<>();
        for (Map<String, Object> change : changes) {
            Map<String, Object> highlight = new LinkedHashMap<>();
            highlight.put(KEY_FILE, change.get(KEY_FILE));
            highlight.put(KEY_SUMMARY, "Updated file " + change.get(KEY_FILE));
            highlights.add(highlight);
        }
        result.put(KEY_SUMMARY_MD, String.format(Locale.ROOT, "### Review\n- %d change(s) detected", changes.size()));
        result.put(KEY_HIGHLIGHTS, highlights);
        result.put(KEY_DIFF, diff.get(KEY_CHANGES));
        return success(result);
    }

    private Map<String, Object> handleFormat(Map<String, Object> arguments) {
        arguments = new LinkedHashMap<>(arguments);
        arguments.put(KEY_RECIPES, List.of(
                RECIPE_AUTO_FORMAT,
                RECIPE_REMOVE_UNUSED_IMPORTS
        ));
        arguments.put(KEY_DRY_RUN, false);
        return handleApply(arguments);
    }

    private Map<String, Object> handleTest(Map<String, Object> arguments) {
        Path workspace = workspace(arguments);
        Map<String, Object> result = baseResponse(CAP_TEST);
        if (workspace == null) {
            return error(result, KEY_WORKSPACE_PATH + " is required");
        }
        List<String> command = determineTestCommand(workspace);
        if (command.isEmpty()) {
            return error(result, "No supported build tool (mvn/gradle) found");
        }
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workspace.toFile());
            Process process = builder.start();
            int exit = process.waitFor();
            result.put(KEY_PASSED, exit == 0);
            result.put(KEY_FAILED, exit == 0 ? 0 : 1);
            if (exit != 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String failureMessage = reader.lines().limit(50).collect(Collectors.joining(System.lineSeparator()));
                    Map<String, Object> failure = new LinkedHashMap<>();
                    failure.put(KEY_MESSAGE, failureMessage);
                    result.put(KEY_FAILURES, List.of(failure));
                }
            } else {
                result.put(KEY_FAILURES, List.of());
            }
            result.put(KEY_MESSAGE, exit == 0 ? "Tests passed" : "Tests failed (exit code " + exit + ")");
            return success(result);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            return error(result, ex.getMessage());
        }
    }

    private Map<String, Object> handleRecipeList(Map<String, Object> arguments) {
        Map<String, Object> result = baseResponse(CAP_RECIPE_LIST);
        List<Map<String, Object>> recipes = discoveryService.listAllRecipes().stream()
                .map(this::toRecipeMap)
                .collect(Collectors.toList());
        result.put(KEY_RECIPES, recipes);
        result.put(KEY_MESSAGE, String.format(Locale.ROOT, "Found %d recipes", recipes.size()));
        return success(result);
    }

    private Map<String, Object> handleRecipeDescribe(Map<String, Object> arguments) {
        Map<String, Object> result = baseResponse(CAP_RECIPE_DESCRIBE);
        String name = stringParam(arguments, KEY_NAME, null);
        if (name == null || name.isBlank()) {
            return error(result, KEY_NAME + " parameter is required");
        }
        Optional<RecipeInfo> info = discoveryService.describeRecipe(name);
        if (info.isEmpty()) {
            return error(result, "Recipe not found: " + name);
        }
        result.put(KEY_RECIPE, toRecipeMap(info.get()));
        return success(result);
    }

    private Map<String, Object> handlePipeline(Map<String, Object> arguments) {
        Path workspace = workspace(arguments);
        Map<String, Object> result = baseResponse(CAP_PIPELINE);
        if (workspace == null) {
            return error(result, KEY_WORKSPACE_PATH + " is required");
        }
        String preset = stringParam(arguments, KEY_PRESET, PROFILE_MODERNIZE_JAVA17);
        boolean dryRun = booleanParam(arguments, KEY_DRY_RUN, true);

        List<String> goals = List.of(preset);
        JavaPlan plan = planner.createPlan(workspace.toString(), goals, List.of(), List.of(), DEFAULT_SCOPE);
        JavaRecipeExecutionResult analyze = executor.preview(workspace.toString(), plan.recipes(), DEFAULT_SCOPE);
        JavaRecipeExecutionResult apply = executor.apply(workspace.toString(), plan.recipes(), DEFAULT_SCOPE, dryRun);

        result.put(KEY_PLAN_ID, plan.id());
        result.put(KEY_ISSUES, analyze.issues());
        result.put(KEY_CHANGES, apply.changes());
        result.put(KEY_MESSAGE, String.format(Locale.ROOT,
                "Pipeline '%s' executed with %d recipe(s)", preset, plan.recipes().size()));
        return success(result);
    }

    private Map<String, Object> baseResponse(String type) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(KEY_TYPE, type);
        response.put(KEY_SUCCESS, false);
        return response;
    }

    private Map<String, Object> success(Map<String, Object> response) {
        response.put(KEY_SUCCESS, true);
        return response;
    }

    private Map<String, Object> error(Map<String, Object> response, String message) {
        response.put(KEY_SUCCESS, false);
        response.put(KEY_ERROR, message);
        response.put(KEY_MESSAGE, message);
        return response;
    }

    private Path workspace(Map<String, Object> arguments) {
        String path = stringParam(arguments, KEY_WORKSPACE_PATH, null);
        return path != null ? Paths.get(path) : null;
    }

    private Map<String, Object> toRecipeMap(RecipeInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(KEY_NAME, info.name());
        map.put(KEY_DISPLAY_NAME, info.displayName());
        map.put(KEY_DESCRIPTION, info.description());
        map.put(KEY_TAGS, info.tags());
        map.put(KEY_OPTIONS, info.options());
        return map;
    }

    private String createCheckpoint(Path workspace, String summary) {
        try (Git git = Git.open(workspace.toFile())) {
            if (git.status().call().isClean()) {
                return null;
            }
            git.add().addFilepattern(".").call();
            RevCommit commit = git.commit().setMessage(COMMIT_PREFIX + summary).call();
            return commit.getName();
        } catch (IOException | GitAPIException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> gitDiff(Path workspace, String fromRef, String toRef) throws Exception {
        try (Git git = Git.open(workspace.toFile())) {
            Repository repository = git.getRepository();
            ObjectId to = repository.resolve(toRef);
            ObjectId from = fromRef != null ? repository.resolve(fromRef) : repository.resolve(toRef + "^");
            if (to == null || from == null) {
                throw new IllegalArgumentException("Unable to resolve git references");
            }
            try (ObjectReader reader = repository.newObjectReader();
                 DiffFormatter formatter = new DiffFormatter(new java.io.ByteArrayOutputStream());
                 RevWalk walk = new RevWalk(repository)) {
                formatter.setRepository(repository);
                RevCommit fromCommit = walk.parseCommit(from);
                RevCommit toCommit = walk.parseCommit(to);
                AbstractTreeIterator oldTree = prepareTreeParser(reader, fromCommit.getTree());
                AbstractTreeIterator newTree = prepareTreeParser(reader, toCommit.getTree());
                List<Map<String, Object>> changes = new ArrayList<>();
                for (DiffEntry entry : formatter.scan(oldTree, newTree)) {
                    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                    try (DiffFormatter entryFormatter = new DiffFormatter(out)) {
                        entryFormatter.setRepository(repository);
                        entryFormatter.format(entry);
                        Map<String, Object> change = new LinkedHashMap<>();
                        change.put(KEY_FILE, entry.getNewPath());
                        change.put(KEY_DIFF, new String(out.toByteArray(), StandardCharsets.UTF_8));
                        changes.add(change);
                    }
                }
                return changes;
            }
        }
    }

    private AbstractTreeIterator prepareTreeParser(ObjectReader reader, RevTree tree) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        parser.reset(reader, tree.getId());
        return parser;
    }

    private List<String> discoverModules(Path workspace) throws IOException {
        List<String> modules = new ArrayList<>();
        try (var stream = Files.list(workspace)) {
            stream.filter(Files::isDirectory).forEach(path -> {
                if (Files.exists(path.resolve("pom.xml")) || Files.exists(path.resolve("build.gradle"))) {
                    modules.add(path.getFileName().toString());
                }
            });
        }
        if (modules.isEmpty()) {
            modules.add(workspace.getFileName().toString());
        }
        return modules;
    }

    private List<Map<String, String>> discoverDependencies(Path workspace) throws IOException {
        Path pom = workspace.resolve("pom.xml");
        if (!Files.exists(pom)) {
            return List.of();
        }
        List<Map<String, String>> dependencies = new ArrayList<>();
        List<String> lines = Files.readAllLines(pom);
        String group = null;
        String artifact = null;
        String version = null;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("<groupId>")) {
                group = trimmed.replace("<groupId>", "").replace("</groupId>", "").trim();
            } else if (trimmed.startsWith("<artifactId>")) {
                artifact = trimmed.replace("<artifactId>", "").replace("</artifactId>", "").trim();
            } else if (trimmed.startsWith("<version>")) {
                version = trimmed.replace("<version>", "").replace("</version>", "").trim();
            } else if (trimmed.startsWith("</dependency>")) {
                if (artifact != null) {
                    Map<String, String> dependency = new LinkedHashMap<>();
                    dependency.put(KEY_GROUP_ID, group);
                    dependency.put(KEY_ARTIFACT_ID, artifact);
                    dependency.put(KEY_VERSION, version);
                    dependencies.add(dependency);
                }
                group = artifact = version = null;
            }
        }
        return dependencies;
    }

    private List<String> listFiles(Path workspace, int limit) throws IOException {
        List<String> files = new ArrayList<>();
        try (var stream = Files.walk(workspace)) {
            stream.filter(Files::isRegularFile)
                    .limit(limit)
                    .forEach(path -> files.add(workspace.relativize(path).toString()));
        }
        return files;
    }

    private List<String> determineTestCommand(Path workspace) {
        if (Files.exists(workspace.resolve("mvnw"))) {
            return Arrays.asList("./mvnw", "-q", "test");
        }
        if (Files.exists(workspace.resolve("mvnw.cmd"))) {
            return Arrays.asList("mvnw.cmd", "-q", "test");
        }
        if (Files.exists(workspace.resolve("pom.xml"))) {
            return Arrays.asList("mvn", "-q", "test");
        }
        if (Files.exists(workspace.resolve("gradlew"))) {
            return Arrays.asList("./gradlew", "test");
        }
        if (Files.exists(workspace.resolve("gradlew.bat"))) {
            return Arrays.asList("gradlew.bat", "test");
        }
        if (Files.exists(workspace.resolve("build.gradle"))) {
            return Arrays.asList("gradle", "test");
        }
        return List.of();
    }

    private Map<String, Object> workspaceSchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
        });
    }

    private Map<String, Object> analyzeSchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
            builder.put(KEY_PROFILE, property("string", "Analysis profile", false,
                    Map.of("enum", PROFILES_ENUM)));
            builder.put(KEY_INCLUDE, arrayProperty("Include specific recipes"));
            builder.put(KEY_EXCLUDE, arrayProperty("Exclude recipes"));
            builder.put(KEY_MAX_FINDINGS, property("integer", "Maximum issues returned", false));
        });
    }

    private Map<String, Object> planSchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
            builder.put(KEY_GOALS, arrayProperty("High level goals"));
            builder.put(KEY_INCLUDE_RECIPES, arrayProperty("Force include recipes"));
            builder.put(KEY_EXCLUDE_RECIPES, arrayProperty("Remove recipes"));
            builder.put(KEY_SCOPE, arrayProperty("Glob patterns"));
            builder.put(KEY_PROFILE, property("string", "Planning profile", false,
                    Map.of("enum", PROFILES_ENUM)));
        });
    }

    private Map<String, Object> applySchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
            builder.put(KEY_PLAN_ID, property("string", "Existing plan identifier", false));
            builder.put(KEY_RECIPES, arrayProperty("Recipes to execute"));
            builder.put(KEY_DRY_RUN, property("boolean", "Preview changes only", false));
            builder.put(KEY_SCOPE, arrayProperty("Glob patterns"));
        });
    }

    private Map<String, Object> diffSchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
            builder.put(KEY_FROM_REF, property("string", "Git reference to diff from", false));
            builder.put(KEY_TO_REF, property("string", "Git reference to diff to", false));
        });
    }

    private Map<String, Object> reviewSchema() {
        return diffSchema();
    }

    private Map<String, Object> formatSchema() {
        return schema(builder -> builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true)));
    }

    private Map<String, Object> testSchema() {
        return schema(builder -> builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true)));
    }

    private Map<String, Object> metricsSchema() {
        return schema(builder -> builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true)));
    }

    private Map<String, Object> recipeDescribeSchema() {
        return schema(builder -> builder.put(KEY_NAME, property("string", "Recipe name", true)));
    }

    private Map<String, Object> pipelineSchema() {
        return schema(builder -> {
            builder.put(KEY_WORKSPACE_PATH, property("string", "Workspace root", true));
            builder.put(KEY_PRESET, property("string", "Preset pipeline", false,
                    Map.of("enum", PRESETS_ENUM)));
            builder.put(KEY_DRY_RUN, property("boolean", "Preview changes", false));
        });
    }

    private Map<String, Object> discoverOutputSchema() {
        Map<String, Object> dependency = new LinkedHashMap<>();
        dependency.put("type", "object");
        dependency.put("properties", Map.of(
                KEY_GROUP_ID, Map.of("type", "string"),
                KEY_ARTIFACT_ID, Map.of("type", "string"),
                KEY_VERSION, Map.of("type", "string")
        ));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_MODULES, Map.of("type", "array", "items", Map.of("type", "string")),
                KEY_DEPENDENCIES, Map.of("type", "array", "items", dependency),
                KEY_FILES, Map.of("type", "array", "items", Map.of("type", "string")),
                KEY_MESSAGE, Map.of("type", "string")
        ));
        return schema;
    }

    private Map<String, Object> analyzeOutputSchema() {
        Map<String, Object> issue = new LinkedHashMap<>();
        issue.put("type", "object");
        issue.put("properties", Map.of(
                KEY_FILE, Map.of("type", "string"),
                KEY_LINE, Map.of("type", "integer"),
                KEY_SEVERITY, Map.of("type", "string"),
                "type", Map.of("type", "string"),
                KEY_MESSAGE, Map.of("type", "string"),
                KEY_RECIPE, Map.of("type", "string")
        ));
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_ISSUES, Map.of("type", "array", "items", issue),
                KEY_METRICS, Map.of("type", "object")
        ));
        return schema;
    }

    private Map<String, Object> planOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_PLAN_ID, Map.of("type", "string"),
                KEY_RECIPES, Map.of("type", "array", "items", Map.of("type", "string")),
                KEY_STEPS, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private Map<String, Object> applyOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_CHANGES, Map.of("type", "array", "items", Map.of("type", "object")),
                KEY_ISSUES, Map.of("type", "array", "items", Map.of("type", "object")),
                KEY_METRICS, Map.of("type", "object")
        ));
        return schema;
    }

    private Map<String, Object> diffOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_CHANGES, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private Map<String, Object> reviewOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_SUMMARY_MD, Map.of("type", "string"),
                KEY_HIGHLIGHTS, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private Map<String, Object> testOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_PASSED, Map.of("type", "boolean"),
                KEY_FAILED, Map.of("type", "integer"),
                KEY_FAILURES, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private Map<String, Object> metricsOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_METRICS, Map.of("type", "object"),
                KEY_DETAILS, Map.of("type", "object")
        ));
        return schema;
    }

    private Map<String, Object> recipeListOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_RECIPES, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private Map<String, Object> recipeDescribeOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(KEY_RECIPE, Map.of("type", "object")));
        return schema;
    }

    private Map<String, Object> pipelineOutputSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                KEY_PLAN_ID, Map.of("type", "string"),
                KEY_ISSUES, Map.of("type", "array", "items", Map.of("type", "object")),
                KEY_CHANGES, Map.of("type", "array", "items", Map.of("type", "object"))
        ));
        return schema;
    }

    private BasicTool createTool(String name, String description, Map<String, Object> inputSchema, Map<String, Object> outputSchema) {
        BasicTool tool = new BasicTool(name, description, inputSchema);
        tool.getMetadata().put(METADATA_OUTPUT_SCHEMA, outputSchema);
        return tool;
    }

    private Map<String, Object> schema(java.util.function.Consumer<Map<String, Object>> consumer) {
        Map<String, Object> properties = new LinkedHashMap<>();
        consumer.accept(properties);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        List<String> required = properties.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(((Map<?, ?>) entry.getValue()).get("required")))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    private Map<String, Object> property(String type, String description, boolean required) {
        return property(type, description, required, Map.of());
    }

    private Map<String, Object> property(String type, String description, boolean required, Map<String, Object> extra) {
        Map<String, Object> prop = new LinkedHashMap<>();
        prop.put("type", type);
        if (description != null) {
            prop.put("description", description);
        }
        if (!extra.isEmpty()) {
            prop.putAll(extra);
        }
        if (required) {
            prop.put("required", true);
        }
        return prop;
    }

    private Map<String, Object> arrayProperty(String description) {
        return property("array", description, false,
                Map.of("items", Map.of("type", "string")));
    }

    private Map<String, Object> optionalParameters(NqlQuery query) {
        return query != null && query.getParameters() != null
                ? query.getParameters()
                : Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<String> listParam(Map<String, ?> params, String key) {
        Object value = params.get(key);
        if (value == null) {
            return new ArrayList<>();
        }
        if (value instanceof List<?>) {
            return ((List<?>) value).stream().map(Objects::toString).collect(Collectors.toList());
        }
        if (value instanceof String str) {
            if (str.isBlank()) {
                return new ArrayList<>();
            }
            return Arrays.stream(str.split(",")).map(String::trim).filter(s -> !s.isBlank()).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String stringParam(Map<String, ?> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private boolean booleanParam(Map<String, ?> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private int intParam(Map<String, ?> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    // Keys for common parameter names to avoid typos
    private static final String KEY_INCLUDE = "include";
    private static final String KEY_EXCLUDE = "exclude";
    private static final String KEY_INCLUDE_RECIPES = "includeRecipes";
    private static final String KEY_EXCLUDE_RECIPES = "excludeRecipes";
    private static final String KEY_PROFILE = "profile";
    private static final String KEY_MAX_FINDINGS = "maxFindings";

    private List<String> sanitizeRecipes(List<String> recipes) {
        if (recipes == null || recipes.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> safe = new LinkedHashSet<>();
        for (String recipe : recipes) {
            if (recipe == null || recipe.isBlank()) {
                continue;
            }
            String trimmed = recipe.trim();
            if (discoveryService.isRecipeSafe(trimmed)) {
                safe.add(trimmed);
            } else {
                // Skipped unsafe recipe
            }
        }
        return List.copyOf(safe);
    }

    private List<String> combineLists(List<String> first, List<String> second) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (first != null) {
            set.addAll(first.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).toList());
        }
        if (second != null) {
            set.addAll(second.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).toList());
        }
        return new ArrayList<>(set);
    }

    // Generate unique run id
    private String generateRunId() {
        return java.util.UUID.randomUUID().toString();
    }
}
