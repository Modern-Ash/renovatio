package org.shark.renovatio.provider.cobol.carddemo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.provider.cobol.service.CobolParsingService;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.provider.cobol.service.TemplateCodeGenerationService;
import org.shark.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.shark.renovatio.shared.domain.StubResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Issue #217 — runs the Renovatio pipeline (parse -> emit Java -> javac) over the vendored AWS
 * CardDemo corpus and writes a deterministic coverage report to {@code docs/reports/}.
 *
 * <p>Measurement only: it never changes the translator and never fails on low coverage. It is
 * excluded from the default build (JUnit tag {@code coverage}); run it with
 * {@code mvn -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true}.
 */
@Tag("coverage")
class CardDemoCoverageReportTest {

    private static final Path REPORT_DIR = Path.of("..", "docs", "reports");

    /** Resolve the CardDemo corpus regardless of the Maven working directory (module root or repo root). */
    private static final Path CORPUS = resolveCorpus(Path.of("src/test/resources/corpus/carddemo"));

    private static Path resolveCorpus(Path local) {
        if (Files.isDirectory(local)) {
            return local;
        }
        Path fromRoot = Path.of("renovatio-provider-cobol", "src", "test", "resources", "corpus", "carddemo");
        return Files.isDirectory(fromRoot) ? fromRoot : local;
    }
    private static final String TODO = "// TODO: Implement COBOL business logic";

    /**
     * Signals the pipeline actually emits into the generated Java when a statement is not
     * translated as real code (spec D3). These are matched against the emitted Java, not the
     * COBOL source, so the "not translated" ranking derives from pipeline evidence instead of a
     * lexical scan. Patterns (see PopulateCobolProcessRecipe): {@code // EXEC SQL <sql>},
     * {@code // CALL <program>}, {@code // <VIEW|OPEN|READ|CLOSE|WRITE|DELETE|SEARCH|...> <file>}
     * and the fallback {@code // Unhandled COBOL statement}.
     */
    private static final Pattern PIPELINE_COMMENT =
            Pattern.compile("(?i)//\\s*("
                    + "EXEC SQL\\b[.\\s]*.*"
                    + "|CALL\\b[.\\s]+[^\\n]+"
                    + "|(?:VIEW|OPEN|READ|CLOSE|WRITE|DELETE|SEARCH|SORT|MERGE|REWRITE|COMMIT|ROLLBACK|STARTER)\\b[.\\s]+[^\\n]+"
                    + ")");

    /**
     * Pipeline fallback marker (no construct name is emitted) used to count dropped statements.
     * {@link #PIPELINE_COMMENT} intentionally does not match it because the verb would be
     * unidentifiable.
     */
    private static final String UNHANDLED = "// Unhandled COBOL statement";
    private static final String NOT_TRANSLATED = "// COBOL not translated:";

    /**
     * Procedural verbs / constructs we scan for in the COBOL source. These only feed the
     * {@code present} column (lexical context, spec D3); the "not translated" ranking is
     * derived exclusively from pipeline evidence in the emitted Java
     * (see {@link #collectPipelineEvidence}).
     */
    private static final List<String> SCANNED_VERBS = List.of(
            "MOVE", "COMPUTE", "IF", "PERFORM", "VARYING", "EVALUATE", "CALL", "ADD", "SUBTRACT", "MULTIPLY", "DIVIDE",
            "READ", "WRITE", "REWRITE", "OPEN", "CLOSE",
            "DISPLAY", "ACCEPT", "STRING", "UNSTRING", "INSPECT", "INITIALIZE", "SET", "SEARCH", "GO TO",
            "GOBACK", "CONTINUE", "STOP RUN", "EXEC CICS", "EXEC SQL", "EXEC DLI");

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void writeCardDemoCoverageReport(@TempDir Path tempDir) throws Exception {
        assertTrue(Files.isDirectory(CORPUS), "CardDemo corpus not found at " + CORPUS.toAbsolutePath());

        List<Path> copybooks = walk(CORPUS, name -> name.endsWith(".cpy") || name.endsWith(".CPY"));
        List<Path> programs = walk(CORPUS, name -> name.endsWith(".cbl") || name.endsWith(".CBL")
                || name.endsWith(".cob") || name.endsWith(".COB"));
        programs.sort(Comparator.comparing(p -> p.getFileName().toString().toUpperCase(Locale.ROOT)));

        Map<String, Row> byProgramId = new TreeMap<>();
        List<String> duplicateProgramIds = new ArrayList<>();

        for (Path program : programs) {
            Row row = analyze(program, copybooks, tempDir.resolve("run-" + byProgramId.size()));
            if (byProgramId.putIfAbsent(row.programId, row) != null) {
                duplicateProgramIds.add(row.programId + " (" + row.file + ")");
            }
        }

        List<Row> rows = new ArrayList<>(byProgramId.values());
        ObjectNode json = buildJson(rows, duplicateProgramIds, programs.size(), copybooks.size());
        assertFalse(mapper.writeValueAsString(json).contains(tempDir.toString()),
                "coverage report must not contain run-specific temporary paths");

        Files.createDirectories(REPORT_DIR);
        Files.writeString(REPORT_DIR.resolve("carddemo-coverage.json"),
                mapper.writeValueAsString(json) + "\n", StandardCharsets.UTF_8);
        Files.writeString(REPORT_DIR.resolve("carddemo-coverage.md"),
                renderMarkdown(json), StandardCharsets.UTF_8);

        assertTrue(rows.size() >= 40, "expected >= 40 CardDemo programs, got " + rows.size());
        assertTrue(Files.size(REPORT_DIR.resolve("carddemo-coverage.md")) > 0);
        assertTrue(rows.stream().anyMatch(row -> row.compile && row.verbsPresent.containsKey("VARYING")),
                "expected at least one CardDemo program with PERFORM VARYING to compile");
    }

    private Row analyze(Path program, List<Path> copybooks, Path workspace) throws Exception {
        String source = Files.readString(program, StandardCharsets.UTF_8);
        Row row = new Row();
        row.file = CORPUS.relativize(program).toString().replace('\\', '/');
        row.programId = programId(source).orElse(program.getFileName().toString());
        row.loc = (int) source.lines().count();
        row.subsystem = classify(row.file, source);
        for (String verb : SCANNED_VERBS) {
            int count = countVerb(source, verb);
            if (count > 0) {
                row.verbsPresent.put(verb, count);
            }
        }

        try {
            Files.createDirectories(workspace);
            Files.copy(program, workspace.resolve(program.getFileName().toString()));
            for (Path copybook : copybooks) {
                Path target = workspace.resolve(copybook.getFileName().toString());
                if (!Files.exists(target)) {
                    Files.copy(copybook, target);
                }
            }
            Workspace ws = new Workspace(row.programId, workspace.toString(), "main");
            CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
            try {
                row.parse = parsing.analyzeCOBOL(new NqlQuery(), ws).isSuccess();
            } catch (RuntimeException | java.io.IOException e) {
                row.parse = false;
                row.parseError = shortMessage(e);
            }

            if (row.parse) {
                try {
                    JavaGenerationService generator = new JavaGenerationService(parsing,
                            new TemplateCodeGenerationService(), new CobolIntermediateModelService(),
                            new CobolSemanticTranspiler(),
                            new ObjectMapper().findAndRegisterModules(), true);
                    StubResult result = generator.generateInterfaceStubs(new NqlQuery(), ws);
                    row.emit = result.isSuccess() && result.getGeneratedCode() != null
                            && !result.getGeneratedCode().isEmpty();
                    if (row.emit) {
                        Map<String, String> code = result.getGeneratedCode();
                        row.javaFiles = code.size();
                        for (String content : code.values()) {
                            row.todoCount += countOccurrences(content, TODO);
                            row.unhandledCount += countOccurrences(content, UNHANDLED);
                            row.notTranslatedCount += countOccurrences(content, NOT_TRANSLATED);
                            collectPipelineEvidence(content, row.unsupportedEvidenced);
                        }
                        row.manualActionItems = readActionItemCount(workspace);
                        row.compile = compile(code, row);
                    } else {
                        row.emitError = shortMessage(result.getMessage());
                    }
                } catch (RuntimeException e) {
                    row.emit = false;
                    row.emitError = shortMessage(e);
                }
            }
        } finally {
            deleteRecursively(workspace);
        }

        return row;
    }

    // --- classification & scanning -------------------------------------------------------------

    private static String classify(String file, String source) {
        String upper = source.toUpperCase(Locale.ROOT);
        if (file.contains("app-authorization-ims-db2-mq") || upper.contains("EXEC DLI") || upper.contains("CBLTDLI")) {
            return "ims-mq";
        }
        if (upper.contains("EXEC SQL")) {
            return "db2";
        }
        if (upper.contains("EXEC CICS")) {
            return "cics-online";
        }
        if (upper.contains("MQOPEN") || upper.contains("MQPUT") || upper.contains("MQGET")) {
            return "vsam-mq";
        }
        return "batch";
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }

    /**
     * Extract construct names from pipeline-emitted comments in the generated Java (spec D3:
     * evidence must come from emitted {@code Unhandled} markers, comments the translator emits
     * for statements it renders as comments, or ManualActionItems — not from a lexical scan of
     * the COBOL source).
     */
    private static void collectPipelineEvidence(String java, Map<String, Integer> tally) {
        Matcher matcher = PIPELINE_COMMENT.matcher(java);
        while (matcher.find()) {
            String construct = matcher.group(1).trim().toUpperCase(Locale.ROOT);
            if (construct.startsWith("EXEC ")) {
                construct = "EXEC SQL";
            } else {
                construct = construct.split("\\s+", 2)[0];
            }
            tally.merge(construct, 1, Integer::sum);
        }
    }

    /** Count verb occurrences as whole tokens so "MOVE" does not match inside "REMOVED" or "MOVED". */
    private static int countVerb(String source, String verb) {
        Pattern pattern = Pattern.compile("(?i)(?<![\\p{L}\\p{N}-])" + Pattern.quote(verb) + "(?![\\p{L}\\p{N}-])");
        Matcher matcher = pattern.matcher(source);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static java.util.Optional<String> programId(String source) {
        // Strip fixed-format sequence numbers (cols 1-6) so PROGRAM-ID can be read even when the
        // program name sits on the next line. COBOL program names start with a letter.
        String normalized = source.lines()
                .map(line -> line.replaceFirst("^\\s{0,6}\\d{6}", "").replaceFirst("\\s+\\d{6,8}\\s*$", ""))
                .collect(Collectors.joining("\n"));
        Matcher matcher = Pattern.compile("(?i)PROGRAM-ID\\s*\\.?\\s+([A-Z][A-Z0-9-]*)").matcher(normalized);
        return matcher.find() ? java.util.Optional.of(matcher.group(1).toUpperCase(Locale.ROOT)) : java.util.Optional.empty();
    }

    private int readActionItemCount(Path workspace) {
        Path report = workspace.resolve("build/reports/renovatio/manual-action-items.json");
        if (!Files.exists(report)) {
            return 0;
        }
        try {
            JsonNode node = mapper.readTree(Files.readString(report, StandardCharsets.UTF_8));
            JsonNode items = node.has("items") ? node.get("items") : node;
            return items.isArray() ? items.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean compile(Map<String, String> generatedCode, Row row) {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return false;
        }
        try {
            Path dir = Files.createTempDirectory("carddemo-coverage-javac-");
            try {
                List<String> arguments = new ArrayList<>(List.of("-classpath",
                        System.getProperty("java.class.path"), "-proc:none",
                        "-d", dir.resolve("classes").toString()));
                for (Map.Entry<String, String> entry : generatedCode.entrySet()) {
                    Path file = dir.resolve(entry.getKey());
                    Files.createDirectories(file.getParent());
                    Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
                    arguments.add(file.toString());
                }
                Files.createDirectories(dir.resolve("classes"));
                java.io.ByteArrayOutputStream sink = new java.io.ByteArrayOutputStream();
                boolean success = compiler.run(null, null, sink, arguments.toArray(String[]::new)) == 0;
                if (success) {
                    return true;
                }
                row.compileError = shortMessage(stableCompilerOutput(
                        new String(sink.toByteArray(), StandardCharsets.UTF_8), dir));
                return false;
            } finally {
                deleteRecursively(dir);
            }
        } catch (Exception e) {
            row.compileError = shortMessage(e);
            return false;
        }
    }

    // --- report rendering --------------------------------------------------------------------

    private ObjectNode buildJson(List<Row> rows, List<String> duplicates, int fileCount, int copybookCount) {
        ObjectNode root = mapper.createObjectNode();
        root.put("corpus", "carddemo");
        root.put("schema", "renovatio/carddemo-coverage.v1");

        ObjectNode totals = root.putObject("totals");
        totals.put("programs", rows.size());
        totals.put("parsed", (int) rows.stream().filter(r -> r.parse).count());
        totals.put("emitted", (int) rows.stream().filter(r -> r.emit).count());
        totals.put("compiled", (int) rows.stream().filter(r -> r.compile).count());
        totals.put("cobolFiles", fileCount);
        totals.put("copybooks", copybookCount);

        ObjectNode bySubsystem = root.putObject("bySubsystem");
        for (String subsystem : rows.stream().map(r -> r.subsystem).distinct().sorted().toList()) {
            List<Row> group = rows.stream().filter(r -> r.subsystem.equals(subsystem)).toList();
            ObjectNode node = bySubsystem.putObject(subsystem);
            node.put("programs", group.size());
            node.put("parsed", (int) group.stream().filter(r -> r.parse).count());
            node.put("emitted", (int) group.stream().filter(r -> r.emit).count());
            node.put("compiled", (int) group.stream().filter(r -> r.compile).count());
        }

        Map<String, int[]> unsupported = new TreeMap<>();
        for (Row row : rows) {
            for (Map.Entry<String, Integer> entry : row.unsupportedEvidenced.entrySet()) {
                int[] tally = unsupported.computeIfAbsent(entry.getKey(), ignored -> new int[2]);
                tally[0]++;
                tally[1] += entry.getValue();
            }
        }
        ArrayNode unsupportedNode = root.putArray("unsupportedConstructs");
        unsupported.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
                .forEach(entry -> {
                    ObjectNode node = unsupportedNode.addObject();
                    node.put("construct", entry.getKey());
                    node.put("programs", entry.getValue()[0]);
                    node.put("occurrences", entry.getValue()[1]);
                });

        ArrayNode candidates = root.putArray("e2eCandidates");
        e2eCandidates(rows).forEach(candidates::add);

        ArrayNode programs = root.putArray("programs");
        for (Row row : rows) {
            ObjectNode node = programs.addObject();
            node.put("programId", row.programId);
            node.put("file", row.file);
            node.put("loc", row.loc);
            node.put("subsystem", row.subsystem);
            node.put("parse", row.parse);
            node.put("emit", row.emit);
            node.put("compile", row.compile);
            node.put("javaFiles", row.javaFiles);
            node.put("manualActionItems", row.manualActionItems);
            node.put("todoBodies", row.todoCount);
            node.put("unhandledStatements", row.unhandledCount);
            node.put("notTranslatedMarkers", row.notTranslatedCount);
            ArrayNode unsup = node.putArray("notTranslated");
            row.unsupportedEvidenced.forEach((construct, count) -> {
                ObjectNode entry = unsup.addObject();
                entry.put("construct", construct);
                entry.put("evidence", count);
            });
            ArrayNode present = node.putArray("present");
            row.verbsPresent.keySet().stream().sorted().forEach(present::add);
            if (row.parseError != null) {
                node.put("parseError", row.parseError);
            }
            if (row.emitError != null) {
                node.put("emitError", row.emitError);
            }
            if (row.compileError != null) {
                node.put("compileError", row.compileError);
            }
        }

        ObjectNode inventory = root.putObject("inventory");
        inventory.put("cobolFiles", fileCount);
        ArrayNode dupes = inventory.putArray("duplicateProgramIds");
        duplicates.forEach(dupes::add);
        return root;
    }

    private static List<String> e2eCandidates(List<Row> rows) {
        return rows.stream()
                .filter(r -> r.subsystem.equals("batch") && r.parse && r.emit)
                .sorted(Comparator
                        .comparing((Row r) -> r.compile ? 0 : 1)
                        .thenComparing(r -> r.todoCount + r.unhandledCount + r.notTranslatedCount)
                        .thenComparing(r -> r.manualActionItems)
                        .thenComparing(r -> r.loc)
                        .thenComparing(r -> r.programId))
                .limit(3)
                .map(r -> r.programId)
                .toList();
    }

    private String renderMarkdown(ObjectNode json) {
        StringBuilder md = new StringBuilder();
        JsonNode totals = json.get("totals");
        md.append("# CardDemo pipeline coverage (issue #217)\n\n");
        md.append("Generated by `CardDemoCoverageReportTest` over the vendored corpus at ")
                .append("`renovatio-provider-cobol/src/test/resources/corpus/carddemo/`. ")
                .append("This is a measurement report; low coverage does not fail the test.\n\n");
        md.append("Run: `mvn -pl renovatio-provider-cobol test -Dgroups=coverage -Drenovatio.surefire.excludedGroups= -Dexec.skip=true`\n\n");

        md.append("## Totals\n\n");
        md.append("| Metric | Value |\n| --- | --- |\n");
        md.append("| COBOL programs | ").append(totals.get("programs").asInt()).append(" |\n");
        md.append("| Parse OK | ").append(totals.get("parsed").asInt()).append(" |\n");
        md.append("| Emit Java OK | ").append(totals.get("emitted").asInt()).append(" |\n");
        md.append("| Generated Java compiles | ").append(totals.get("compiled").asInt()).append(" |\n");
        md.append("| Copybooks | ").append(totals.get("copybooks").asInt()).append(" |\n\n");

        md.append("## By subsystem\n\n");
        md.append("| Subsystem | Programs | Parse | Emit | Compile |\n| --- | --: | --: | --: | --: |\n");
        JsonNode bySubsystem = json.get("bySubsystem");
        bySubsystem.fieldNames().forEachRemaining(name -> {
            JsonNode node = bySubsystem.get(name);
            md.append("| ").append(name).append(" | ").append(node.get("programs").asInt())
                    .append(" | ").append(node.get("parsed").asInt())
                    .append(" | ").append(node.get("emitted").asInt())
                    .append(" | ").append(node.get("compiled").asInt()).append(" |\n");
        });
        md.append("\n");

        md.append("## Constructs not translated today (pipeline evidence from emitted Java, D3)\n\n");
        md.append("| Construct | Programs | Evidence |\n| --- | --: | --: |\n");
        for (JsonNode node : json.get("unsupportedConstructs")) {
            md.append("| `").append(node.get("construct").asText()).append("` | ")
                    .append(node.get("programs").asInt()).append(" | ")
                    .append(node.get("occurrences").asInt()).append(" |\n");
        }
        if (json.get("unsupportedConstructs").isEmpty()) {
            md.append("_No statements were emitted as comments by the pipeline._\n");
        }
        md.append("\n");

        md.append("## E2E candidates for issue #216 (simplest batch programs)\n\n");
        for (JsonNode node : json.get("e2eCandidates")) {
            md.append("- `").append(node.asText()).append("`\n");
        }
        md.append("\n");

        md.append("## Compilation failures\n\n");
        List<JsonNode> failed = new ArrayList<>();
        for (JsonNode node : json.get("programs")) {
            if (node.has("compileError")) {
                failed.add(node);
            }
        }
        if (failed.isEmpty()) {
            md.append("None — every emitted program compiled.\n");
        } else {
            md.append("| Program | Error |\n| --- | --- |\n");
            for (JsonNode node : failed) {
                md.append("| `").append(node.get("programId").asText()).append("` | `")
                        .append(node.get("compileError").asText().replace("`", "'")).append("` |\n");
            }
        }
        md.append("\n");

        md.append("## Per program\n\n");
        md.append("| Program | Subsystem | LOC | Parse | Emit | Compile | Java files | Action items | TODO | Unhandled | Not translated | Present (lexical) |\n");
        md.append("| --- | --- | --: | :-: | :-: | :-: | --: | --: | --: | --: | --: | --- |\n");
        for (JsonNode node : json.get("programs")) {
            List<String> present = new ArrayList<>();
            node.get("present").forEach(p -> present.add(p.asText()));
            md.append("| `").append(node.get("programId").asText()).append("` | ")
                    .append(node.get("subsystem").asText()).append(" | ")
                    .append(node.get("loc").asInt()).append(" | ")
                    .append(mark(node.get("parse").asBoolean())).append(" | ")
                    .append(mark(node.get("emit").asBoolean())).append(" | ")
                    .append(mark(node.get("compile").asBoolean())).append(" | ")
                    .append(node.get("javaFiles").asInt()).append(" | ")
                    .append(node.get("manualActionItems").asInt()).append(" | ")
                    .append(node.get("todoBodies").asInt()).append(" | ")
                    .append(node.get("unhandledStatements").asInt()).append(" | ")
                    .append(node.get("notTranslatedMarkers").asInt()).append(" | ")
                    .append(String.join(", ", present)).append(" |\n");
        }
        return md.toString();
    }

    private static String mark(boolean value) {
        return value ? "yes" : "no";
    }

    // --- helpers -----------------------------------------------------------------------------

    private static List<Path> walk(Path root, java.util.function.Predicate<String> nameFilter) throws Exception {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> nameFilter.test(path.getFileName().toString()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private static String shortMessage(Object value) {
        String text = value instanceof Throwable throwable
                ? throwable.getClass().getSimpleName() + ": " + throwable.getMessage()
                : String.valueOf(value);
        text = text == null ? "" : text.replace('\n', ' ').trim();
        return text.length() > 200 ? text.substring(0, 200) + "…" : text;
    }

    private static String stableCompilerOutput(String output, Path compilationDirectory) {
        String prefix = compilationDirectory.toAbsolutePath().normalize()
                + java.io.File.separator;
        return output.replace(prefix, "");
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(entry -> {
                try {
                    Files.deleteIfExists(entry);
                } catch (Exception ignored) {
                    // best effort
                }
            });
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static final class Row {
        String file;
        String programId;
        int loc;
        String subsystem;
        final Map<String, Integer> verbsPresent = new LinkedHashMap<>();
        final Map<String, Integer> unsupportedEvidenced = new TreeMap<>();
        boolean parse;
        boolean emit;
        boolean compile;
        int javaFiles;
        int manualActionItems;
        int todoCount;
        int unhandledCount;
        int notTranslatedCount;
        String parseError;
        String emitError;
        String compileError;
    }
}
