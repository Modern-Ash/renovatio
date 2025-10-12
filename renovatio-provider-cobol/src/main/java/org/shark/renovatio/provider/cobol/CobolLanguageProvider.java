package org.shark.renovatio.provider.cobol;

import org.shark.renovatio.provider.cobol.service.*;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.BaseLanguageProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * COBOL Language Provider implementation supporting COBOL to Java migration
 * Implements parsing, analysis, code generation and migration capabilities
 */
public class CobolLanguageProvider extends BaseLanguageProvider {

    // --- Constants to avoid duplicated string literals ---
    private static final String LANG_COBOL = "cobol";

    // Tool identifiers
    private static final String TOOL_ANALYZE = "cobol.analyze";
    private static final String TOOL_METRICS = "cobol.metrics";
    private static final String TOOL_PLAN = "cobol.plan";
    private static final String TOOL_APPLY = "cobol.apply";
    private static final String TOOL_DIFF = "cobol.diff";
    private static final String TOOL_MIGRATE_COPYBOOK = "cobol.migrate_copybook";
    private static final String TOOL_MIGRATE_DB2 = "cobol.migrate_db2";

    // Extended capabilities (executeExtendedTool)
    private static final String CAP_MIGRATE_COPYBOOK = "migrate_copybook";
    private static final String CAP_MIGRATE_DB2 = "migrate_db2";

    // Common JSON keys and values
    private static final String KEY_TYPE = "type";
    private static final String KEY_PROPERTIES = "properties";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_ITEMS = "items";
    private static final String KEY_STRING = "string";
    private static final String KEY_ARRAY = "array";
    private static final String KEY_BOOLEAN = "boolean";
    private static final String KEY_REQUIRED = "required";
    private static final String KEY_NQL = "nql";
    private static final String KEY_SCOPE = "scope";
    private static final String KEY_GOALS = "goals";
    private static final String KEY_PLAN_ID = "planId";
    private static final String KEY_DRY_RUN = "dryRun";
    private static final String KEY_RUN_ID = "runId";
    private static final String KEY_COPYBOOK = "copybook";
    private static final String KEY_PROGRAM = "program";
    private static final String KEY_WORKSPACE_PATH = "workspacePath";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_DATA = "data";
    private static final String KEY_GENERATED = "generated";

    // Common values
    private static final String TYPE_OBJECT = "object";
    private static final String TYPE_STUBS = "stubs";

    // Error/Info messages
    private static final String MSG_ANALYSIS_FAILED = "COBOL analysis failed: ";
    private static final String MSG_PLAN_FAILED = "Migration planning failed: ";
    private static final String MSG_APPLY_FAILED = "Migration application failed: ";
    private static final String MSG_DIFF_FAILED = "Diff generation failed: ";
    private static final String MSG_METRICS_FAILED = "Metrics calculation failed: ";
    private static final String MSG_NO_COPYBOOK = "No copybook specified";
    private static final String MSG_COPYBOOK_NOT_FOUND = "Copybook not found: ";
    private static final String MSG_NO_PROGRAM = "No COBOL program specified";
    private static final String MSG_PROGRAM_NOT_FOUND = "COBOL program not found: ";
    private static final String MSG_REQUIRED_WORKSPACE = "workspacePath is required";
    private static final String MSG_REQUIRED_COPYBOOK = "copybook is required (e.g., CUSTOMER.cpy)";
    private static final String MSG_REQUIRED_PROGRAM = "program is required (e.g., ORDERPROC.cbl)";

    private final CobolParsingService parsingService;
    private final JavaGenerationService javaGenerationService;
    private final MigrationPlanService migrationPlanService;
    private final MetricsService metricsService;
    private final TemplateCodeGenerationService templateCodeGenerationService;
    private final Db2MigrationService db2MigrationService;
    private final IndexingService indexingService;

    public CobolLanguageProvider(
            CobolParsingService parsingService,
            JavaGenerationService javaGenerationService,
            MigrationPlanService migrationPlanService,
            IndexingService indexingService,
            MetricsService metricsService,
            TemplateCodeGenerationService templateCodeGenerationService,
            Db2MigrationService db2MigrationService) {
        this.parsingService = parsingService;
        this.javaGenerationService = javaGenerationService;
        this.migrationPlanService = migrationPlanService;
        this.indexingService = indexingService;
        this.metricsService = metricsService;
        this.templateCodeGenerationService = templateCodeGenerationService;
        this.db2MigrationService = db2MigrationService;
    }

    @Override
    public String language() {
        return LANG_COBOL;
    }

    @Override
    public Set<Capabilities> capabilities() {
        return EnumSet.of(
                Capabilities.ANALYZE,
                Capabilities.PLAN,
                Capabilities.APPLY,
                Capabilities.DIFF,
                Capabilities.STUBS,
                Capabilities.METRICS
        );
    }

    @Override
    public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
        // Index workspace for fast search on subsequent operations; ignore failures
        safeIndexWorkspace(workspace);
        try {
            return parsingService.analyzeCOBOL(query, workspace);
        } catch (Exception e) {
            AnalyzeResult result;
            result = new AnalyzeResult(false, MSG_ANALYSIS_FAILED + e.getMessage());
            return result;
        }
    }

    @Override
    public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
        try {
            return migrationPlanService.createMigrationPlan(query, scope, workspace);
        } catch (Exception e) {
            return new PlanResult(false, MSG_PLAN_FAILED + e.getMessage());
        }
    }

    @Override
    public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
        try {
            return migrationPlanService.applyMigrationPlan(planId, dryRun, workspace);
        } catch (Exception e) {
            return new ApplyResult(false, MSG_APPLY_FAILED + e.getMessage());
        }
    }

    @Override
    public DiffResult diff(String runId, Workspace workspace) {
        try {
            return migrationPlanService.generateDiff(runId, workspace);
        } catch (Exception e) {
            return new DiffResult(false, MSG_DIFF_FAILED + e.getMessage());
        }
    }

    @Override
    public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
        try {
            return Optional.of(javaGenerationService.generateInterfaceStubs(query, workspace));
        } catch (Exception e) {
            StubResult result = new StubResult(false, "Stub generation failed: " + e.getMessage());
            return Optional.of(result);
        }
    }

    @Override
    public MetricsResult metrics(Scope scope, Workspace workspace) {
        try {
            return metricsService.calculateMetrics(scope, workspace);
        } catch (Exception e) {
            return new MetricsResult(false, MSG_METRICS_FAILED + e.getMessage());
        }
    }

    /**
     * Migrate a specific COBOL copybook to Java artifacts using templates.
     */
    public StubResult migrateCopybook(NqlQuery query, Workspace workspace) {
        try {
            String copybookName = null;
            if (query.getParameters() != null) {
                Object cb = query.getParameters().get(KEY_COPYBOOK);
                if (cb != null) {
                    copybookName = cb.toString();
                }
            }
            if (copybookName == null) {
                return new StubResult(false, MSG_NO_COPYBOOK);
            }
            final String finalCopybookName = copybookName;
            Path root = Paths.get(workspace.getPath());
            java.util.List<Path> copybooks = parsingService.findCopybooks(root);
            Optional<Path> copybookPath = copybooks.stream()
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(finalCopybookName))
                    .findFirst();
            if (copybookPath.isEmpty()) {
                return new StubResult(false, MSG_COPYBOOK_NOT_FOUND + copybookName);
            }

            Map<String, Object> metadata = parsingService.parseCopybook(copybookPath.get(), parsingService.getDefaultDialect());
            metadata.put("filePath", copybookPath.get().toString());

            Map<String, String> generated = templateCodeGenerationService.generateFromCopybook(
                    copybookName.replaceFirst("\\.[^.]+$", ""), metadata);

            boolean success = !generated.isEmpty();
            StubResult result = new StubResult(success,
                    success ? "Generated " + generated.size() + " artifacts" : "No artifacts generated");
            result.setGeneratedCode(generated);
            return result;
        } catch (Exception e) {
            return new StubResult(false, "Copybook migration failed: " + e.getMessage());
        }
    }

    /**
     * Generate JPA artifacts from embedded DB2 EXEC SQL statements.
     */
    public StubResult migrateDb2(NqlQuery query, Workspace workspace) {
        try {
            String programName = null;
            if (query.getParameters() != null) {
                Object p = query.getParameters().get(KEY_PROGRAM);
                if (p != null) {
                    programName = p.toString();
                }
            }
            if (programName == null) {
                return new StubResult(false, MSG_NO_PROGRAM);
            }
            final String finalProgramName = programName;
            Path root = Paths.get(workspace.getPath());
            java.util.List<Path> cobolFiles = parsingService.findCobolFiles(root);
            Optional<Path> programPath = cobolFiles.stream()
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(finalProgramName))
                    .findFirst();
            if (programPath.isEmpty()) {
                return new StubResult(false, MSG_PROGRAM_NOT_FOUND + programName);
            }

            Map<String, String> generated = db2MigrationService.migrateCobolFile(programPath.get());
            boolean success = !generated.isEmpty();
            StubResult result = new StubResult(success,
                    success ? "Generated " + generated.size() + " artifacts" : "No SQL statements found");
            result.setGeneratedCode(generated);
            return result;
        } catch (Exception e) {
            return new StubResult(false, "DB2 migration failed: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<Tool> getTools() {
        // Publish COBOL tools for MCP clients
        List<Tool> tools = new ArrayList<>();
        tools.add(new BasicTool(TOOL_ANALYZE, "Analyze COBOL sources (parsing, AST, dependencies)", baseSchema()));
        tools.add(new BasicTool(TOOL_METRICS, "Collect high-level COBOL metrics (files, lines, copybooks)", baseSchema()));
        tools.add(new BasicTool(TOOL_PLAN, "Create migration plan from COBOL to Java", planSchema()));
        tools.add(new BasicTool(TOOL_APPLY, "Apply migration plan (code generation, transforms)", applySchema()));
        tools.add(new BasicTool(TOOL_DIFF, "Generate diff for last migration run", diffSchema()));
        // Extended provider-specific tools
        tools.add(new BasicTool(TOOL_MIGRATE_COPYBOOK, "Generate Java artifacts from a COBOL copybook (templates)", migrateCopybookSchema()));
        tools.add(new BasicTool(TOOL_MIGRATE_DB2, "Generate JPA code from embedded DB2 EXEC SQL in COBOL program", migrateDb2Schema()));
        return tools;
    }

    @Override
    @SuppressWarnings("java:S1168")
    // Intentionally return null for "not handled" to enable registry fallback per ExtendedLanguageProvider contract
    public Map<String, Object> executeExtendedTool(String capability, Map<String, Object> arguments) {
        if (capability == null) return null;
        String cap = capability.toLowerCase(Locale.ROOT);
        switch (cap) {
            case CAP_MIGRATE_COPYBOOK:
                return handleMigrateCopybook(arguments);
            case CAP_MIGRATE_DB2:
                return handleMigrateDb2(arguments);
            default:
                return null; // Not handled here, allow default routing
        }
    }

    // ---- Extended tool handlers ----
    private Map<String, Object> handleMigrateCopybook(Map<String, Object> args) {
        String workspacePath = asString(args.get(KEY_WORKSPACE_PATH));
        String copybook = asString(args.get(KEY_COPYBOOK));
        Map<String, Object> response = baseResponse(TYPE_STUBS);
        if (workspacePath == null || workspacePath.isBlank()) {
            return error(response, MSG_REQUIRED_WORKSPACE);
        }
        if (copybook == null || copybook.isBlank()) {
            return error(response, MSG_REQUIRED_COPYBOOK);
        }
        Workspace ws = new Workspace();
        ws.setId("default");
        ws.setPath(workspacePath);
        NqlQuery query = new NqlQuery();
        query.setLanguage(language());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(KEY_COPYBOOK, copybook);
        query.setParameters(params);
        StubResult result = migrateCopybook(query, ws);
        response.put(KEY_SUCCESS, result.isSuccess());
        response.put(KEY_MESSAGE, result.getMessage());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(KEY_GENERATED, result.getGeneratedCode());
        response.put(KEY_DATA, data);
        return success(response);
    }

    private Map<String, Object> handleMigrateDb2(Map<String, Object> args) {
        String workspacePath = asString(args.get(KEY_WORKSPACE_PATH));
        String program = asString(args.get(KEY_PROGRAM));
        Map<String, Object> response = baseResponse(TYPE_STUBS);
        if (workspacePath == null || workspacePath.isBlank()) {
            return error(response, MSG_REQUIRED_WORKSPACE);
        }
        if (program == null || program.isBlank()) {
            return error(response, MSG_REQUIRED_PROGRAM);
        }
        Workspace ws = new Workspace();
        ws.setId("default");
        ws.setPath(workspacePath);
        NqlQuery query = new NqlQuery();
        query.setLanguage(language());
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(KEY_PROGRAM, program);
        query.setParameters(params);
        StubResult result = migrateDb2(query, ws);
        response.put(KEY_SUCCESS, result.isSuccess());
        response.put(KEY_MESSAGE, result.getMessage());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(KEY_GENERATED, result.getGeneratedCode());
        response.put(KEY_DATA, data);
        return success(response);
    }

    // ---- Schemas ----
    private Map<String, Object> baseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(KEY_TYPE, TYPE_OBJECT);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(KEY_NQL, Map.of(
                KEY_TYPE, KEY_STRING,
                KEY_DESCRIPTION, "NQL query to select COBOL elements or recipes"
        ));
        props.put(KEY_SCOPE, Map.of(
                KEY_TYPE, KEY_STRING,
                KEY_DESCRIPTION, "Glob pattern for files to include (e.g., **/*.cbl)"
        ));
        schema.put(KEY_PROPERTIES, props);
        return schema;
    }

    private Map<String, Object> planSchema() {
        Map<String, Object> schema = baseSchema();
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) schema.get(KEY_PROPERTIES);
        props.put(KEY_GOALS, Map.of(
                KEY_TYPE, KEY_ARRAY,
                KEY_DESCRIPTION, "High-level migration goals (e.g., db2, jpa, rest)",
                KEY_ITEMS, Map.of(KEY_TYPE, KEY_STRING)
        ));
        return schema;
    }

    private Map<String, Object> applySchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(KEY_TYPE, TYPE_OBJECT);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(KEY_PLAN_ID, Map.of(KEY_TYPE, KEY_STRING, KEY_DESCRIPTION, "Plan id returned by cobol.plan"));
        props.put(KEY_DRY_RUN, Map.of(KEY_TYPE, KEY_BOOLEAN, KEY_DESCRIPTION, "Simulate without writing files"));
        schema.put(KEY_PROPERTIES, props);
        return schema;
    }

    private Map<String, Object> diffSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(KEY_TYPE, TYPE_OBJECT);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(KEY_RUN_ID, Map.of(KEY_TYPE, KEY_STRING, KEY_DESCRIPTION, "Run id from previous operation"));
        schema.put(KEY_PROPERTIES, props);
        return schema;
    }

    private Map<String, Object> migrateCopybookSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(KEY_TYPE, TYPE_OBJECT);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(KEY_COPYBOOK, Map.of(KEY_TYPE, KEY_STRING, KEY_DESCRIPTION, "Copybook file name (e.g., CUSTOMER.cpy)"));
        schema.put(KEY_PROPERTIES, props);
        schema.put(KEY_REQUIRED, java.util.List.of(KEY_COPYBOOK));
        return schema;
    }

    private Map<String, Object> migrateDb2Schema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(KEY_TYPE, TYPE_OBJECT);
        Map<String, Object> props = new LinkedHashMap<>();
        props.put(KEY_PROGRAM, Map.of(KEY_TYPE, KEY_STRING, KEY_DESCRIPTION, "COBOL program with EXEC SQL (e.g., ORDERPROC.cbl)"));
        schema.put(KEY_PROPERTIES, props);
        schema.put(KEY_REQUIRED, java.util.List.of(KEY_PROGRAM));
        return schema;
    }

    // ---- helpers ----
    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Map<String, Object> baseResponse(String type) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put(KEY_TYPE, type);
        r.put(KEY_SUCCESS, false);
        r.put(KEY_MESSAGE, "");
        return r;
    }

    private Map<String, Object> success(Map<String, Object> r) {
        r.put(KEY_SUCCESS, true);
        return r;
    }

    private Map<String, Object> error(Map<String, Object> r, String msg) {
        r.put(KEY_SUCCESS, false);
        r.put(KEY_MESSAGE, msg);
        return r;
    }

    /**
     * Best-effort workspace indexing that never throws, to avoid nested try/catch in callers.
     */
    private void safeIndexWorkspace(Workspace workspace) {
        try {
            indexingService.indexWorkspace(workspace);
        } catch (Exception ignored) {
            // Best-effort: indexing failures shouldn't block provider operations
        }
    }
}
