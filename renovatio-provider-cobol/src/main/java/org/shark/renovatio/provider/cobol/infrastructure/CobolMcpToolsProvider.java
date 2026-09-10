package org.shark.renovatio.provider.cobol.infrastructure;

import org.shark.renovatio.provider.cobol.CobolLanguageProvider;
import org.shark.renovatio.provider.cobol.domain.CobolMcpTool;
import org.shark.renovatio.provider.cobol.pipeline.PipelineRequest;
import org.shark.renovatio.provider.cobol.pipeline.PipelineResult;
import org.shark.renovatio.provider.cobol.service.CobolReferencePipelineService;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP tools integration for COBOL migration capabilities
 * Exposes COBOL provider functionality as MCP tools
 */
@Component
public class CobolMcpToolsProvider {

    private final CobolLanguageProvider cobolProvider;
    private final CobolReferencePipelineService referencePipeline;

    // ---- Constants to avoid duplicated string literals (Sonar S1192) ----
    private static final String LANG_COBOL = "cobol";

    // Tool names
    private static final String TOOL_ANALYZE = "cobol.analyze";
    private static final String TOOL_GENERATE_STUBS = "cobol.generate.stubs";
    private static final String TOOL_MIGRATION_PLAN = "cobol.migration.plan";
    private static final String TOOL_MIGRATION_APPLY = "cobol.migration.apply";
    private static final String TOOL_METRICS = "cobol.metrics";
    private static final String TOOL_DIFF = "cobol.diff";
    private static final String TOOL_COPYBOOK_MIGRATE = "cobol.copybook.migrate";
    private static final String TOOL_DB2_MIGRATE = "cobol.db2.migrate";
    private static final String TOOL_REFERENCE_PIPELINE = "cobol.pipeline.execute";

    // Query targets
    private static final String TARGET_PROGRAMS = "programs";
    private static final String TARGET_STUBS = "stubs";
    private static final String TARGET_MIGRATION = "migration";
    private static final String TARGET_COPYBOOK = "copybook";
    private static final String TARGET_DB2 = "db2";

    // Common argument names
    private static final String ARG_WORKSPACE_PATH = "workspacePath";
    private static final String ARG_PROGRAM = "program";
    private static final String ARG_COPYBOOK = "copybook";
    private static final String ARG_PLAN_ID = "planId";
    private static final String ARG_DRY_RUN = "dryRun";
    private static final String ARG_RUN_ID = "runId";
    private static final String ARG_OUTPUT_PATH = "outputPath";
    private static final String ARG_EXPECTED_PATH = "expectedPath";
    private static final String ARG_VERIFY_EQUIVALENCE = "verifyEquivalence";

    // Response keys
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERROR = "error";
    private static final String KEY_FILES = "files";
    private static final String KEY_DATA = "data";
    private static final String KEY_AST = "ast";
    private static final String KEY_SYMBOLS = "symbols";
    private static final String KEY_DEPENDENCIES = "dependencies";
    private static final String KEY_RUN_ID = "runId";
    private static final String KEY_GENERATED_FILES = "generatedFiles";
    private static final String KEY_DIFF = "diff";
    private static final String KEY_SEMANTIC = "semantic";
    private static final String KEY_METRICS = "metrics";
    private static final String KEY_DETAILS = "details";
    private static final String KEY_PLAN_ID = "planId";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_MODIFIED_FILES = "modifiedFiles";
    private static final String KEY_CHANGES = "changes";

    // JSON schema keys/values
    private static final String SCHEMA_TYPE = "type";
    private static final String SCHEMA_PROPERTIES = "properties";
    private static final String SCHEMA_REQUIRED = "required";
    private static final String SCHEMA_DESCRIPTION = "description";
    private static final String JSON_STRING = "string";
    private static final String JSON_OBJECT = "object";
    private static final String JSON_BOOLEAN = "boolean";

    // Common descriptions
    private static final String DESC_WORKSPACE = "Path to COBOL workspace";
    private static final String DESC_PROGRAM_FILE = "COBOL program file";
    private static final String DESC_COPYBOOK_FILE = "Copybook file name to migrate";

    // Misc
    private static final String BRANCH_MAIN = "main";
    private static final String RUN_ID_PREFIX = "mcp-";
    private static final String PATH_TMP = "/tmp";

    public CobolMcpToolsProvider(CobolLanguageProvider cobolProvider) {
        this.cobolProvider = cobolProvider;
        this.referencePipeline = null;
    }

    @Autowired
    public CobolMcpToolsProvider(CobolLanguageProvider cobolProvider,
                                 CobolReferencePipelineService referencePipeline) {
        this.cobolProvider = cobolProvider;
        this.referencePipeline = referencePipeline;
    }

    /**
     * Gets all available COBOL migration tools
     */
    public List<CobolMcpTool> getCobolMigrationTools() {
        List<CobolMcpTool> tools = new ArrayList<>();

        tools.add(createAnalyzeCobolTool());
        tools.add(createGenerateJavaStubsTool());
        tools.add(createCreateMigrationPlanTool());
        tools.add(createApplyMigrationPlanTool());
        tools.add(createCalculateMetricsTool());
        tools.add(createGenerateDiffTool());
        tools.add(createCopybookMigrationTool());
        tools.add(createDb2MigrationTool());
        tools.add(createReferencePipelineTool());

        return tools;
    }

    /**
     * Executes COBOL migration tool
     */
    public Object executeCobolTool(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case TOOL_ANALYZE -> executeAnalyzeTool(arguments);
            case TOOL_GENERATE_STUBS -> executeGenerateStubsTool(arguments);
            case TOOL_MIGRATION_PLAN -> executeCreatePlanTool(arguments);
            case TOOL_MIGRATION_APPLY -> executeApplyPlanTool(arguments);
            case TOOL_METRICS -> executeMetricsTool(arguments);
            case TOOL_DIFF -> executeDiffTool(arguments);
            case TOOL_COPYBOOK_MIGRATE -> executeCopybookMigrationTool(arguments);
            case TOOL_DB2_MIGRATE -> executeDb2MigrationTool(arguments);
            case TOOL_REFERENCE_PIPELINE -> executeReferencePipelineTool(arguments);
            default -> Map.of(KEY_ERROR, "Unknown COBOL tool: " + toolName);
        };
    }

    private CobolMcpTool createDb2MigrationTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_DB2_MIGRATE);
        tool.setDescription("Generate JPA code from embedded DB2 EXEC SQL statements");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                ARG_PROGRAM, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_PROGRAM_FILE)
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH, ARG_PROGRAM));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createReferencePipelineTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_REFERENCE_PIPELINE);
        tool.setDescription("Execute the complete governed COBOL-to-Java reference pipeline");
        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
            ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
            ARG_OUTPUT_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION,
                "Directory for generated Java output"),
            ARG_EXPECTED_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION,
                "Optional expected-output directory for equivalence"),
            ARG_VERIFY_EQUIVALENCE, Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION,
                "Compare the complete generated tree with expected output")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH, ARG_OUTPUT_PATH));
        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createAnalyzeCobolTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_ANALYZE);
        tool.setDescription("Analyze COBOL programs and extract structure information");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                "query", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Analysis query"),
                "includeMetrics", Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION, "Include code metrics in analysis")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createGenerateJavaStubsTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_GENERATE_STUBS);
        tool.setDescription("Generate Java interface stubs from COBOL programs");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                "targetPackage", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Java package for generated code"),
                "generateTests", Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION, "Generate test classes")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createCreateMigrationPlanTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_MIGRATION_PLAN);
        tool.setDescription("Create a migration plan for COBOL to Java transformation");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                "migrationStrategy", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Migration strategy (full, incremental, hybrid)"),
                "targetFramework", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Target Java framework (spring-boot, quarkus, etc.)")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createApplyMigrationPlanTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_MIGRATION_APPLY);
        tool.setDescription("Apply a migration plan to transform COBOL to Java");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_PLAN_ID, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Migration plan ID"),
                ARG_DRY_RUN, Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION, "Execute as dry run"),
                "outputPath", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Output path for generated Java code")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_PLAN_ID));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createCalculateMetricsTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_METRICS);
        tool.setDescription("Calculate code metrics for COBOL programs");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                "includeComplexity", Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION, "Include cyclomatic complexity"),
                "includeDependencies", Map.of(SCHEMA_TYPE, JSON_BOOLEAN, SCHEMA_DESCRIPTION, "Include dependency analysis")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createGenerateDiffTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_DIFF);
        tool.setDescription("Generate diff for migration changes");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_RUN_ID, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Migration run ID"),
                "format", Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, "Diff format (unified, semantic, both)")
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_RUN_ID));

        tool.setInputSchema(schema);
        return tool;
    }

    private CobolMcpTool createCopybookMigrationTool() {
        CobolMcpTool tool = new CobolMcpTool();
        tool.setName(TOOL_COPYBOOK_MIGRATE);
        tool.setDescription("Generate Java artifacts from a COBOL copybook");

        Map<String, Object> schema = new HashMap<>();
        schema.put(SCHEMA_TYPE, JSON_OBJECT);
        schema.put(SCHEMA_PROPERTIES, Map.of(
                ARG_WORKSPACE_PATH, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_WORKSPACE),
                ARG_COPYBOOK, Map.of(SCHEMA_TYPE, JSON_STRING, SCHEMA_DESCRIPTION, DESC_COPYBOOK_FILE)
        ));
        schema.put(SCHEMA_REQUIRED, List.of(ARG_WORKSPACE_PATH, ARG_COPYBOOK));

        tool.setInputSchema(schema);
        return tool;
    }

    // Tool execution methods

    private Object executeAnalyzeTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            NqlQuery query = new NqlQuery();
            query.setType(NqlQuery.QueryType.FIND);
            query.setTarget(TARGET_PROGRAMS);
            query.setLanguage(LANG_COBOL);

            AnalyzeResult result = cobolProvider.analyze(query, workspace);

            // Full response with all analysis data
            Map<String, Object> response = new HashMap<>();
            response.put(KEY_SUCCESS, result.isSuccess());
            response.put(KEY_MESSAGE, result.getMessage());

            if (result.getData() != null) {
                response.put(KEY_DATA, result.getData());
            }
            if (result.getAst() != null) {
                response.put(KEY_AST, result.getAst());
            }
            if (result.getSymbols() != null) {
                response.put(KEY_SYMBOLS, result.getSymbols());
            }
            if (result.getDependencies() != null) {
                response.put(KEY_DEPENDENCIES, result.getDependencies());
            }
            if (result.getRunId() != null) {
                response.put(KEY_RUN_ID, result.getRunId());
            }

            return response;

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeGenerateStubsTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            NqlQuery query = new NqlQuery();
            query.setType(NqlQuery.QueryType.FIND);
            query.setTarget(TARGET_STUBS);
            query.setLanguage(LANG_COBOL);

            Optional<StubResult> result = cobolProvider.generateStubs(query, workspace);
            if (result.isPresent()) {
                StubResult stubResult = result.get();
                return Map.of(
                        KEY_SUCCESS, stubResult.isSuccess(),
                        KEY_MESSAGE, stubResult.getMessage(),
                        KEY_GENERATED_FILES, stubResult.getGeneratedCode() != null ? stubResult.getGeneratedCode().size() : 0,
                        KEY_FILES, stubResult.getGeneratedCode()
                );
            } else {
                return Map.of(KEY_SUCCESS, false, KEY_ERROR, "No stubs generated");
            }

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeCreatePlanTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            NqlQuery query = new NqlQuery();
            query.setType(NqlQuery.QueryType.PLAN);
            query.setTarget(TARGET_MIGRATION);
            query.setLanguage(LANG_COBOL);

            Scope scope = new Scope();

            PlanResult result = cobolProvider.plan(query, scope, workspace);
            return Map.of(
                    KEY_SUCCESS, result.isSuccess(),
                    KEY_MESSAGE, result.getMessage(),
                    KEY_PLAN_ID, result.getPlanId(),
                    KEY_STEPS, result.getSteps()
            );

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeApplyPlanTool(Map<String, Object> arguments) {
        try {
            String planId = (String) arguments.get(ARG_PLAN_ID);
            Boolean dryRun = (Boolean) arguments.getOrDefault(ARG_DRY_RUN, true);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(PATH_TMP);
            workspace.setBranch(BRANCH_MAIN);

            ApplyResult result = cobolProvider.apply(planId, dryRun, workspace);
            return Map.of(
                    KEY_SUCCESS, result.isSuccess(),
                    KEY_MESSAGE, result.getMessage(),
                    KEY_RUN_ID, result.getRunId(),
                    KEY_MODIFIED_FILES, result.getModifiedFiles(),
                    KEY_CHANGES, result.getChanges()
            );

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeMetricsTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            Scope scope = new Scope();

            MetricsResult result = cobolProvider.metrics(scope, workspace);
            return Map.of(
                    KEY_SUCCESS, result.isSuccess(),
                    KEY_MESSAGE, result.getMessage(),
                    KEY_METRICS, result.getMetrics(),
                    KEY_DETAILS, result.getDetails()
            );

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeDiffTool(Map<String, Object> arguments) {
        try {
            String runId = (String) arguments.get(ARG_RUN_ID);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(PATH_TMP);
            workspace.setBranch(BRANCH_MAIN);

            DiffResult result = cobolProvider.diff(runId, workspace);
            return Map.of(
                    KEY_SUCCESS, result.isSuccess(),
                    KEY_MESSAGE, result.getMessage(),
                    KEY_DIFF, result.getUnifiedDiff(),
                    KEY_SEMANTIC, result.getSemanticDiff()
            );

        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeCopybookMigrationTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);
            String copybook = (String) arguments.get(ARG_COPYBOOK);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            NqlQuery query = new NqlQuery();
            query.setType(NqlQuery.QueryType.FIND);
            query.setTarget(TARGET_COPYBOOK);
            query.setLanguage(LANG_COBOL);
            Map<String, Object> params = new HashMap<>();
            params.put(ARG_COPYBOOK, copybook);
            query.setParameters(params);

            StubResult result = cobolProvider.migrateCopybook(query, workspace);
            return Map.of(
                KEY_SUCCESS, result.isSuccess(),
                KEY_MESSAGE, result.getMessage(),
                KEY_FILES, result.getGeneratedCode()
            );
        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeDb2MigrationTool(Map<String, Object> arguments) {
        try {
            String workspacePath = (String) arguments.get(ARG_WORKSPACE_PATH);
            String program = (String) arguments.get(ARG_PROGRAM);

            Workspace workspace = new Workspace();
            workspace.setId(RUN_ID_PREFIX + System.currentTimeMillis());
            workspace.setPath(workspacePath);
            workspace.setBranch(BRANCH_MAIN);

            NqlQuery query = new NqlQuery();
            query.setType(NqlQuery.QueryType.FIND);
            query.setTarget(TARGET_DB2);
            query.setLanguage(LANG_COBOL);
            Map<String, Object> params = new HashMap<>();
            params.put(ARG_PROGRAM, program);
            query.setParameters(params);

            StubResult result = cobolProvider.migrateDb2(query, workspace);
            return Map.of(
                KEY_SUCCESS, result.isSuccess(),
                KEY_MESSAGE, result.getMessage(),
                KEY_FILES, result.getGeneratedCode()
            );
        } catch (Exception e) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR, e.getMessage());
        }
    }

    private Object executeReferencePipelineTool(Map<String, Object> arguments) {
        if (referencePipeline == null) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR,
                "Reference pipeline is not configured for this adapter instance");
        }
        try {
            java.nio.file.Path fixture = java.nio.file.Path.of(
                Objects.toString(arguments.get(ARG_WORKSPACE_PATH), ""));
            java.nio.file.Path output = java.nio.file.Path.of(
                Objects.toString(arguments.get(ARG_OUTPUT_PATH), ""));
            PipelineRequest base = PipelineRequest.of(fixture, output);
            Object expectedArgument = arguments.get(ARG_EXPECTED_PATH);
            boolean verify = Boolean.TRUE.equals(arguments.getOrDefault(ARG_VERIFY_EQUIVALENCE,
                expectedArgument == null ? Boolean.FALSE : Boolean.TRUE));
            java.nio.file.Path expected = expectedArgument == null
                ? base.expectedDir() : java.nio.file.Path.of(expectedArgument.toString());
            PipelineRequest request = new PipelineRequest(base.fixtureId(), base.fixtureDir(),
                base.decisions(), base.outputDir(), expected, base.deterministic(), verify,
                base.sourceSnapshotHash());
            PipelineResult result = referencePipeline.execute(request);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put(KEY_SUCCESS, result.isSuccessful());
            response.put(KEY_MESSAGE, result.isSuccessful()
                ? "Reference pipeline completed" : "Reference pipeline failed");
            response.put("fixtureId", result.fixtureId());
            response.put("outputPath", result.outputDir().toString());
            response.put("semanticGaps", result.semanticGaps());
            response.put("equivalenceReports", result.equivalenceReports());
            Map<String, Object> stages = new LinkedHashMap<>();
            stages.put("discover", result.discover());
            stages.put("parse", result.parse());
            stages.put("semanticIr", result.semanticIr());
            stages.put("domainModel", result.domainModel());
            stages.put("decisions", result.decisions());
            stages.put("architecture", result.architecture());
            stages.put("manifest", result.manifest());
            stages.put("emit", result.emit());
            stages.put("build", result.build());
            response.put("stages", stages);
            return response;
        } catch (Exception exception) {
            return Map.of(KEY_SUCCESS, false, KEY_ERROR,
                exception.getMessage() == null ? exception.getClass().getName() : exception.getMessage());
        }
    }
}
