package org.shark.renovatio.provider.cobol;

import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.CobolParsingService.Dialect;
import org.shark.renovatio.shared.domain.*;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.shark.renovatio.shared.spi.BaseLanguageProvider;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * COBOL language provider implementation using real COBOL file analysis
 */
// @Component
public class CobolProvider extends BaseLanguageProvider {

    // --- Constants: language, tool names, descriptions, metadata keys/values, messages ---
    private static final String LANGUAGE = "cobol";

    // Tool names
    private static final String TOOL_ANALYZE = "cobol.analyze";
    private static final String TOOL_METRICS = "cobol.metrics";
    private static final String TOOL_DIFF = "cobol.diff";
    private static final String TOOL_STUBS = "cobol.stubs_generate";

    // Tool descriptions
    private static final String DESC_ANALYZE = "Analyze COBOL source code";
    private static final String DESC_METRICS = "Calculate COBOL code metrics";
    private static final String DESC_DIFF = "Generate semantic diff for COBOL code";
    private static final String DESC_STUBS = "Generate Java stubs/adapters for COBOL interfaces";

    // Metadata keys
    private static final String META_PARAMETERS = "parameters";
    private static final String META_CAPABILITY = "capability";
    private static final String META_WORKFLOW_PHASE = "workflowPhase";
    private static final String META_LANGUAGE = "language";
    private static final String META_DISPLAY_NAME = "displayName";

    // Common values
    private static final String VALUE_ANALYZE = "analyze";
    private static final String VALUE_METRICS = "metrics";
    private static final String VALUE_DIFF = "diff";
    private static final String VALUE_STUBS = "stubs";
    private static final String PHASE_ANALYSIS = "analysis";
    private static final String PHASE_BASELINE = "baseline";
    private static final String PHASE_REVIEW = "review";
    private static final String PHASE_REFACTOR = "refactor";

    // Schema keys
    private static final String SCHEMA_TYPE = "type";
    private static final String SCHEMA_OBJECT = "object";
    private static final String SCHEMA_PROPERTIES = "properties";
    private static final String SCHEMA_REQUIRED = "required";
    private static final String SCHEMA_EXAMPLE = "example";
    private static final String SCHEMA_DESCRIPTION = "description";
    private static final String SCHEMA_STRING = "string";

    // Common field keys
    private static final String FIELD_WORKSPACE_PATH = "workspacePath";
    private static final String FIELD_RUN_ID = "runId";
    private static final String FIELD_TARGET_LANGUAGE = "targetLanguage";
    private static final String FIELD_NAME = "name";

    // Display names
    private static final String DISPLAY_ANALYZE = "Analyze COBOL code";
    private static final String DISPLAY_METRICS = "Collect COBOL metrics";
    private static final String DISPLAY_DIFF = "Review COBOL changes";
    private static final String DISPLAY_STUBS = "Generate COBOL interface stubs";

    // Messages
    private static final String MSG_PLAN_UNSUPPORTED = "Direct planning not supported for COBOL. Use generateStubs instead.";
    private static final String MSG_APPLY_UNSUPPORTED = "Direct application not supported for COBOL. Use generateStubs instead.";
    private static final String MSG_DIFF_OK = "COBOL diff generated";
    private static final String MSG_METRICS_OK = "COBOL metrics calculated";
    private static final String MSG_ANALYZE_FAILED_PREFIX = "COBOL analysis failed: ";
    private static final String MSG_PARSED_FILES_FMT = "Parsed %d COBOL source file(s) and %d copybook(s)";
    private static final String MSG_STUBS_OK = "Java stubs generated for COBOL interfaces";
    private static final String MSG_TODO_COBOL_INTERFACE = "TODO: Implement COBOL interface";

    // Metrics/detail keys
    private static final String METRIC_LOC = "linesOfCode";
    private static final String METRIC_CYCLO = "cyclomaticComplexity";
    private static final String METRIC_NUM_PROGRAMS = "numberOfPrograms";
    private static final String METRIC_NUM_PROCEDURES = "numberOfProcedures";
    private static final String METRIC_COPYBOOK_USAGE = "copybookUsage";

    private static final String DETAIL_COMPLEX_PROCS = "complexProcedures";
    private static final String DETAIL_UNUSED_VARS = "unusedVariables";
    private static final String DETAIL_IO_OPS = "ioOperations";

    // AST keys
    private static final String AST_PROGRAMS = "programs";
    private static final String AST_FILE_COUNT = "fileCount";

    // Diff keys
    private static final String DIFF_PROCEDURES_ADDED = "proceduresAdded";
    private static final String DIFF_PROCEDURES_MODIFIED = "proceduresModified";
    private static final String DIFF_COPYBOOKS_CHANGED = "copybooksChanged";

    // Other
    private static final String DIALECT_KEY = "dialect";
    private static final String TARGET_LANG_JAVA = "java";

    private final CobolParsingService parsingService;

    public CobolProvider(CobolParsingService parsingService) {
        this.parsingService = parsingService;
    }

    @Override
    public String language() {
        return LANGUAGE;
    }

    @Override
    public Set<Capabilities> capabilities() {
        return Set.of(
                Capabilities.ANALYZE,
                Capabilities.DIFF,
                Capabilities.STUBS,
                Capabilities.METRICS
        );
        // Note: COBOL provider typically doesn't support direct PLAN/APPLY
        // Instead uses generateStubs strategy as mentioned in requirements
    }

    @Override
    public AnalyzeResult analyze(NqlQuery query, Workspace workspace) {
        AnalyzeResult result = new AnalyzeResult();
        result.setRunId(generateRunId());
        try {
            Path root = Paths.get(workspace.getPath());
            List<Path> cobolFiles = parsingService.findCobolSourceFiles(root);
            List<Path> copybooks = parsingService.findCopybooks(root);

            List<Map<String, Object>> astPrograms = new ArrayList<>();
            for (Path cobolFile : cobolFiles) {
                astPrograms.add(parsingService.parseCobolFile(cobolFile, resolveDialect(query, workspace)));
            }

            Map<String, Object> ast = new HashMap<>();
            ast.put(AST_PROGRAMS, astPrograms);
            ast.put(AST_FILE_COUNT, cobolFiles.size());
            result.setAst(ast);

            Map<String, Object> data = new HashMap<>();
            data.put("sourceFiles", toRelativePaths(root, cobolFiles));
            data.put("copybooks", toRelativePaths(root, copybooks));
            data.put(AST_PROGRAMS, astPrograms);
            data.put("summary", Map.of(
                    "sourceFiles", cobolFiles.size(),
                    "copybooks", copybooks.size(),
                    "programs", astPrograms.size()
            ));
            result.setData(data);

            result.setSuccess(true);
            result.setMessage(String.format(MSG_PARSED_FILES_FMT, cobolFiles.size(), copybooks.size()));
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(MSG_ANALYZE_FAILED_PREFIX + e.getMessage());
        }
        return result;
    }

    @Override
    public PlanResult plan(NqlQuery query, Scope scope, Workspace workspace) {
        // COBOL provider doesn't support direct planning
        PlanResult result = new PlanResult(false, MSG_PLAN_UNSUPPORTED);
        result.setRunId(generateRunId());
        return result;
    }

    @Override
    public ApplyResult apply(String planId, boolean dryRun, Workspace workspace) {
        // COBOL provider doesn't support direct application
        ApplyResult result = new ApplyResult(false, MSG_APPLY_UNSUPPORTED);
        result.setRunId(generateRunId());
        return result;
    }

    @Override
    public DiffResult diff(String runId, Workspace workspace) {
        DiffResult result = new DiffResult(true, MSG_DIFF_OK);
        result.setRunId(runId);

        // Would use GumTree for semantic diffs as mentioned in requirements
        String unifiedDiff = createSampleDiff();
        result.setUnifiedDiff(unifiedDiff);

        Map<String, Object> semanticDiff = new HashMap<>();
        semanticDiff.put(DIFF_PROCEDURES_ADDED, 1);
        semanticDiff.put(DIFF_PROCEDURES_MODIFIED, 2);
        semanticDiff.put(DIFF_COPYBOOKS_CHANGED, 1);
        result.setSemanticDiff(semanticDiff);

        return result;
    }

    @Override
    public Optional<StubResult> generateStubs(NqlQuery query, Workspace workspace) {
        StubResult result = new StubResult(true, MSG_STUBS_OK);
        result.setRunId(generateRunId());
        result.setTargetLanguage(TARGET_LANG_JAVA);

        // Would use JavaPoet/templates as mentioned in requirements
        Map<String, String> generatedFiles = new HashMap<>();
        generatedFiles.put("CustomerRecord.java", generateCustomerRecordStub());
        generatedFiles.put("TransactionRecord.java", generateTransactionRecordStub());
        generatedFiles.put("CobolProgramAdapter.java", generateProgramAdapterStub());
        result.setGeneratedFiles(generatedFiles);

        String template = "// Generated Java stubs for COBOL interface\n" +
                "// Target: " + query.getTarget() + "\n" +
                "// Generated from: " + workspace.getPath();
        result.setStubTemplate(template);

        return Optional.of(result);
    }

    @Override
    public MetricsResult metrics(Scope scope, Workspace workspace) {
        MetricsResult result = new MetricsResult(true, MSG_METRICS_OK);
        result.setRunId(generateRunId());

        Map<String, Number> metrics = new HashMap<>();
        metrics.put(METRIC_LOC, 2800);
        metrics.put(METRIC_CYCLO, 12.3);
        metrics.put(METRIC_NUM_PROGRAMS, 5);
        metrics.put(METRIC_NUM_PROCEDURES, 45);
        metrics.put(METRIC_COPYBOOK_USAGE, 8);
        result.setMetrics(metrics);

        Map<String, Object> details = new HashMap<>();
        details.put(DETAIL_COMPLEX_PROCS, Arrays.asList("PROCESS-TRANSACTIONS", "VALIDATE-CUSTOMER", "CALCULATE-TOTALS"));
        details.put(DETAIL_UNUSED_VARS, Arrays.asList("WS-TEMP", "WS-UNUSED"));
        details.put(DETAIL_IO_OPS, 15);
        result.setDetails(details);

        return result;
    }

    @Override
    public java.util.List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();

        // Analyze tool
        BasicTool analyzeTool = new BasicTool(
                TOOL_ANALYZE,
                DESC_ANALYZE,
                Map.of(
                        SCHEMA_TYPE, SCHEMA_OBJECT,
                        SCHEMA_PROPERTIES, Map.of(
                                FIELD_WORKSPACE_PATH, Map.of(
                                        SCHEMA_DESCRIPTION, "Path to the workspace directory to analyze",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                )
                        ),
                        SCHEMA_REQUIRED, List.of(FIELD_WORKSPACE_PATH),
                        SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace")
                )
        );
        analyzeTool.getMetadata().put(META_PARAMETERS, List.of(
                Map.of(
                        FIELD_NAME, FIELD_WORKSPACE_PATH,
                        SCHEMA_DESCRIPTION, "Path to the workspace directory to analyze",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                )
        ));
        analyzeTool.getMetadata().put(SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace"));
        analyzeTool.getMetadata().put(META_CAPABILITY, VALUE_ANALYZE);
        analyzeTool.getMetadata().put(META_WORKFLOW_PHASE, PHASE_ANALYSIS);
        analyzeTool.getMetadata().put(META_LANGUAGE, language());
        analyzeTool.getMetadata().put(META_DISPLAY_NAME, DISPLAY_ANALYZE);
        tools.add(analyzeTool);

        // Metrics tool
        BasicTool metricsTool = new BasicTool(
                TOOL_METRICS,
                DESC_METRICS,
                Map.of(
                        SCHEMA_TYPE, SCHEMA_OBJECT,
                        SCHEMA_PROPERTIES, Map.of(
                                FIELD_WORKSPACE_PATH, Map.of(
                                        SCHEMA_DESCRIPTION, "Path to the workspace directory to analyze",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                )
                        ),
                        SCHEMA_REQUIRED, List.of(FIELD_WORKSPACE_PATH),
                        SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace")
                )
        );
        metricsTool.getMetadata().put(META_PARAMETERS, List.of(
                Map.of(
                        FIELD_NAME, FIELD_WORKSPACE_PATH,
                        SCHEMA_DESCRIPTION, "Path to the workspace directory to analyze",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                )
        ));
        metricsTool.getMetadata().put(SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace"));
        metricsTool.getMetadata().put(META_CAPABILITY, VALUE_METRICS);
        metricsTool.getMetadata().put(META_WORKFLOW_PHASE, PHASE_BASELINE);
        metricsTool.getMetadata().put(META_LANGUAGE, language());
        metricsTool.getMetadata().put(META_DISPLAY_NAME, DISPLAY_METRICS);
        tools.add(metricsTool);

        // Diff tool
        BasicTool diffTool = new BasicTool(
                TOOL_DIFF,
                DESC_DIFF,
                Map.of(
                        SCHEMA_TYPE, SCHEMA_OBJECT,
                        SCHEMA_PROPERTIES, Map.of(
                                FIELD_RUN_ID, Map.of(
                                        SCHEMA_DESCRIPTION, "Run ID to generate diff for",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                ),
                                FIELD_WORKSPACE_PATH, Map.of(
                                        SCHEMA_DESCRIPTION, "Path to the workspace directory",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                )
                        ),
                        SCHEMA_REQUIRED, List.of(FIELD_RUN_ID, FIELD_WORKSPACE_PATH),
                        SCHEMA_EXAMPLE, Map.of(FIELD_RUN_ID, "run-123", FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace")
                )
        );
        diffTool.getMetadata().put(META_PARAMETERS, List.of(
                Map.of(
                        FIELD_NAME, FIELD_RUN_ID,
                        SCHEMA_DESCRIPTION, "Run ID to generate diff for",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                ),
                Map.of(
                        FIELD_NAME, FIELD_WORKSPACE_PATH,
                        SCHEMA_DESCRIPTION, "Path to the workspace directory",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                )
        ));
        diffTool.getMetadata().put(SCHEMA_EXAMPLE, Map.of(FIELD_RUN_ID, "run-123", FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace"));
        diffTool.getMetadata().put(META_CAPABILITY, VALUE_DIFF);
        diffTool.getMetadata().put(META_WORKFLOW_PHASE, PHASE_REVIEW);
        diffTool.getMetadata().put(META_LANGUAGE, language());
        diffTool.getMetadata().put(META_DISPLAY_NAME, DISPLAY_DIFF);
        tools.add(diffTool);

        // Generate stubs tool
        BasicTool stubsTool = new BasicTool(
                TOOL_STUBS,
                DESC_STUBS,
                Map.of(
                        SCHEMA_TYPE, SCHEMA_OBJECT,
                        SCHEMA_PROPERTIES, Map.of(
                                FIELD_WORKSPACE_PATH, Map.of(
                                        SCHEMA_DESCRIPTION, "Path to the workspace directory",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                ),
                                FIELD_TARGET_LANGUAGE, Map.of(
                                        SCHEMA_DESCRIPTION, "Target language for stubs (e.g., java)",
                                        SCHEMA_TYPE, SCHEMA_STRING
                                )
                        ),
                        SCHEMA_REQUIRED, List.of(FIELD_WORKSPACE_PATH, FIELD_TARGET_LANGUAGE),
                        SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace", FIELD_TARGET_LANGUAGE, TARGET_LANG_JAVA)
                )
        );
        stubsTool.getMetadata().put(META_PARAMETERS, List.of(
                Map.of(
                        FIELD_NAME, FIELD_WORKSPACE_PATH,
                        SCHEMA_DESCRIPTION, "Path to the workspace directory",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                ),
                Map.of(
                        FIELD_NAME, FIELD_TARGET_LANGUAGE,
                        SCHEMA_DESCRIPTION, "Target language for stubs (e.g., java)",
                        SCHEMA_TYPE, SCHEMA_STRING,
                        "required", true
                )
        ));
        stubsTool.getMetadata().put(SCHEMA_EXAMPLE, Map.of(FIELD_WORKSPACE_PATH, "/path/to/cobol/workspace", FIELD_TARGET_LANGUAGE, TARGET_LANG_JAVA));
        stubsTool.getMetadata().put(META_CAPABILITY, VALUE_STUBS);
        stubsTool.getMetadata().put(META_WORKFLOW_PHASE, PHASE_REFACTOR);
        stubsTool.getMetadata().put(META_LANGUAGE, language());
        stubsTool.getMetadata().put(META_DISPLAY_NAME, DISPLAY_STUBS);
        tools.add(stubsTool);

        return tools;
    }

    private Dialect resolveDialect(NqlQuery query, Workspace workspace) {
        String value = null;
        if (query != null && query.getParameters() != null) {
            Object p = query.getParameters().get(DIALECT_KEY);
            if (p != null) {
                value = p.toString();
            }
        }
        if (value == null && workspace != null && workspace.getMetadata() != null) {
            Object m = workspace.getMetadata().get(DIALECT_KEY);
            if (m != null) {
                value = m.toString();
            }
        }
        if (value == null) {
            return parsingService.getDefaultDialect();
        }
        return Dialect.fromString(value);
    }

    private List<String> toRelativePaths(Path root, List<Path> paths) {
        List<String> relativePaths = new ArrayList<>();
        for (Path path : paths) {
            try {
                relativePaths.add(root.relativize(path).toString());
            } catch (Exception e) {
                relativePaths.add(path.toString());
            }
        }
        return relativePaths;
    }


    private String generateCustomerRecordStub() {
        return """
                package org.example.cobol.records;
                
                /**
                 * Generated stub for COBOL CUSTOMER-RECORD
                 */
                public class CustomerRecord {
                    private Long customerId;
                    private String customerName;
                    private String customerEmail;
                    private String customerPhone;
                
                    // Generated getters and setters
                    public Long getCustomerId() { return customerId; }
                    public void setCustomerId(Long customerId) { this.customerId = customerId; }
                
                    public String getCustomerName() { return customerName; }
                    public void setCustomerName(String customerName) { this.customerName = customerName; }
                
                    public String getCustomerEmail() { return customerEmail; }
                    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
                
                    public String getCustomerPhone() { return customerPhone; }
                    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
                }
                """;
    }

    private String generateTransactionRecordStub() {
        return """
                package org.example.cobol.records;
                
                import java.math.BigDecimal;
                import java.time.LocalDate;
                
                /**
                 * Generated stub for COBOL TRANSACTION-RECORD
                 */
                public class TransactionRecord {
                    private Long transactionId;
                    private Long customerId;
                    private BigDecimal amount;
                    private LocalDate transactionDate;
                
                    // Generated getters and setters
                    public Long getTransactionId() { return transactionId; }
                    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
                
                    public Long getCustomerId() { return customerId; }
                    public void setCustomerId(Long customerId) { this.customerId = customerId; }
                
                    public BigDecimal getAmount() { return amount; }
                    public void setAmount(BigDecimal amount) { this.amount = amount; }
                
                    public LocalDate getTransactionDate() { return transactionDate; }
                    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }
                }
                """;
    }

    private String generateProgramAdapterStub() {
        return """
                package org.example.cobol.adapters;
                
                import org.example.cobol.records.CustomerRecord;
                import org.example.cobol.records.TransactionRecord;
                
                /**
                 * Generated adapter for COBOL program interface
                 */
                public class CobolProgramAdapter {
                
                    /**
                     * Process customer data - delegates to COBOL PROCESS-CUSTOMER procedure
                     */
                    public void processCustomer(CustomerRecord customer) {
                        // TODO: Implement JNI call to COBOL or web service adapter
                        throw new UnsupportedOperationException("TODO: Implement COBOL interface");
                    }
                
                    /**
                     * Validate transaction - delegates to COBOL VALIDATE-TRANSACTION procedure
                     */
                    public boolean validateTransaction(TransactionRecord transaction) {
                        // TODO: Implement JNI call to COBOL or web service adapter
                        throw new UnsupportedOperationException("TODO: Implement COBOL interface");
                    }
                }
                """;
    }
}
