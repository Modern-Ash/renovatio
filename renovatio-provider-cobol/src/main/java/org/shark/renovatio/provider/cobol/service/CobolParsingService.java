package org.shark.renovatio.provider.cobol.service;

import lombok.Getter;
import org.shark.renovatio.provider.cobol.domain.CobolProgram;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.PerformanceMetrics;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Lightweight COBOL parsing service used for tests and examples.
 *
 * <p>This implementation avoids external parser dependencies so that the
 * module can be built in environments without network access. The parsing
 * performed here is intentionally simplistic and relies on regular
 * expressions to extract a few pieces of information such as the program id,
 * embedded SQL statements and simple CICS commands.</p>
 */
@Getter
public class CobolParsingService {

    // --- Constants: file extensions ---
    private static final String EXT_COB = ".cob";
    private static final String EXT_COBOL = ".cobol";
    private static final String EXT_CBL = ".cbl";
    private static final String EXT_CPY = ".cpy";

    // --- Constants: map keys ---
    private static final String KEY_PROGRAMS = "programs";
    private static final String KEY_FILE_PATH = "filePath";
    private static final String KEY_PROGRAM_ID = "programId";
    private static final String KEY_CICS_COMMANDS = "cicsCommands";
    private static final String KEY_CALLS = "calls";
    private static final String KEY_COPIES = "copies";
    private static final String KEY_DATA_ITEMS = "dataItems";
    private static final String KEY_LINKAGE_ITEMS = "linkageItems";
    private static final String KEY_LINKAGE_STRUCT_NAME = "linkageStructName";
    private static final String KEY_STRUCT_NAME = "structName";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_DIALECT = "dialect";
    private static final String KEY_ITEMS = "items";

    // --- Constants: COBOL tokens / indicators ---
    private static final String COBOL_COMP3 = "COMP-3";
    private static final String COBOL_COMP5 = "COMP-5";
    private static final String PIC_DECIMAL_V = "V";
    private static final String PIC_ALPHA_PREFIX = "X";

    // --- Constants: Java type names ---
    private static final String JAVA_TYPE_BIG_DECIMAL = "BigDecimal";
    private static final String JAVA_TYPE_STRING = "String";
    private static final String JAVA_TYPE_INTEGER = "Integer";
    private static final String JAVA_TYPE_LONG = "Long";

    // --- Constants: regex patterns (precompiled) ---
    private static final Pattern EXEC_SQL_PATTERN = Pattern.compile("EXEC\\s+SQL(.*?)END-EXEC", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern WS_SECTION_PATTERN = Pattern.compile("WORKING-STORAGE SECTION\\.(.*)(PROCEDURE DIVISION\\.|\\Z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern WS_FIELD_PATTERN = Pattern.compile("^\\s*\\d+\\s+([A-Z0-9-]+)\\s+PIC\\s+([A-Z0-9()]+)(?:\\s+COMP-?\\d+)?(?:\\s+SIGNED)?\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern LINKAGE_SECTION_PATTERN = Pattern.compile("LINKAGE SECTION\\.(.*)(PROCEDURE DIVISION\\.|\\Z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern LINKAGE_GROUP_PATTERN = Pattern.compile("^\\s*01\\s+([A-Z0-9-]+)\\s*\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern LINKAGE_FIELD_PATTERN = Pattern.compile("^\\s*05\\s+([A-Z0-9-]+)\\s+PIC\\s+([A-Z0-9()]+)(?:\\s+COMP-?\\d+)?(?:\\s+SIGNED)?\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTRY_PATTERN = Pattern.compile("ENTRY\\s+\"([A-Z0-9_-]+)\"\\s+USING\\s+([A-Z0-9-]+)\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRAM_ID_PATTERN = Pattern.compile("PROGRAM-ID\\.\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CICS_COMMAND_PATTERN = Pattern.compile("EXEC\\s+CICS\\s+([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COPYBOOK_PATTERN = Pattern.compile(
            "\\bCOPY\\s+(?:\"([^\"]+)\"|'([^']+)'|([A-Z0-9][A-Z0-9_.-]*))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DIGITS_PATTERN = Pattern.compile("9\\((\\d+)\\)");

    private final Dialect defaultDialect;

    public CobolParsingService() {
        this(Dialect.IBM);
    }

    public CobolParsingService(Dialect dialect) {
        this.defaultDialect = dialect == null ? Dialect.IBM : dialect;
    }

    /**
     * Locate COBOL source files inside a workspace.
     */
    public List<Path> findCobolSourceFiles(Path workspacePath) throws IOException {
        List<Path> cobolFiles = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(workspacePath)) {
            walkStream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(EXT_COB) ||
                                name.endsWith(EXT_COBOL) ||
                                name.endsWith(EXT_CBL);
                    })
                    .forEach(cobolFiles::add);
        }
        return cobolFiles;
    }

    /**
     * Backwards-compatible alias for source COBOL files.
     */
    public List<Path> findCobolFiles(Path workspacePath) throws IOException {
        return findCobolSourceFiles(workspacePath);
    }

    public List<Path> findCopybooks(Path workspacePath) throws IOException {
        List<Path> copybooks = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(workspacePath)) {
            walkStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(EXT_CPY))
                    .forEach(copybooks::add);
        }
        return copybooks;
    }

    /**
     * Extract all embedded EXEC SQL statements from a COBOL source file.
     */
    public List<String> extractExecSqlStatements(Path cobolFile) throws IOException {
        String content = Files.readString(cobolFile);
        return extractExecSqlStatements(content);
    }

    /**
     * Extract embedded EXEC SQL statements from COBOL source content.
     */
    public List<String> extractExecSqlStatements(String cobolSource) {
        List<String> statements = new ArrayList<>();
        Matcher matcher = EXEC_SQL_PATTERN.matcher(cobolSource);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    /** Extract normalized copybook names referenced by COBOL COPY statements. */
    public List<String> extractCopybookReferences(Path cobolFile) throws IOException {
        return extractCopybookReferences(Files.readString(cobolFile));
    }

    /** Extract normalized copybook names referenced by COBOL COPY statements. */
    public List<String> extractCopybookReferences(String cobolSource) {
        SortedSet<String> copybooks = new TreeSet<>();
        Matcher matcher = COPYBOOK_PATTERN.matcher(Objects.requireNonNull(cobolSource, "cobolSource"));
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1)
                    : matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            String fileName = Path.of(raw).getFileName().toString()
                    .replaceFirst("\\.$", "").replaceFirst("(?i)\\.cpy$", "");
            copybooks.add(fileName.toUpperCase(Locale.ROOT));
        }
        return List.copyOf(copybooks);
    }

    /**
     * Analyze all COBOL files in the given workspace. The dialect can be
     * provided via query parameters ("dialect") or workspace metadata. When
     * absent the service's default dialect is used.
     */
    public AnalyzeResult analyzeCOBOL(NqlQuery query, Workspace workspace) throws IOException {
        long start = System.nanoTime();

        Dialect dialect = resolveDialect(query, workspace);
        Path root = Paths.get(workspace.getPath());
        List<Path> sourceFiles = findCobolSourceFiles(root);
        List<Path> copybooks = findCopybooks(root);

        List<CobolProgram> programs = new ArrayList<>();
        for (Path cobolFile : sourceFiles) {
            Map<String, Object> metadata = parseCobolFile(cobolFile, dialect);
            metadata.put(KEY_FILE_PATH, cobolFile.toString());
            CobolProgram program = new CobolProgram();
            program.setProgramId((String) metadata.get(KEY_PROGRAM_ID));
            program.setProgramName((String) metadata.get(KEY_PROGRAM_ID));
            program.setMetadata(metadata);
            programs.add(program);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("sourceFiles", toRelativePaths(root, sourceFiles));
        data.put("copybooks", toRelativePaths(root, copybooks));
        data.put(KEY_PROGRAMS, programs);
        data.put("summary", Map.of(
                "sourceFiles", sourceFiles.size(),
                "copybooks", copybooks.size(),
                "programs", programs.size()
        ));

        AnalyzeResult result = new AnalyzeResult(true, String.format(
                "Parsed %d COBOL source file(s) and %d copybook(s)",
                sourceFiles.size(),
                copybooks.size()
        ));
        result.setData(data);

        long elapsed = System.nanoTime() - start;
        result.setPerformance(new PerformanceMetrics(elapsed / 1_000_000));
        return result;
    }

    private Dialect resolveDialect(NqlQuery query, Workspace workspace) {
        String dialectStr = null;
        if (query != null && query.getParameters() != null) {
            Object param = query.getParameters().get(KEY_DIALECT);
            if (param != null) {
                dialectStr = param.toString();
            }
        }
        if (dialectStr == null && workspace != null && workspace.getMetadata() != null) {
            Object meta = workspace.getMetadata().get(KEY_DIALECT);
            if (meta != null) {
                dialectStr = meta.toString();
            }
        }

        return Dialect.fromString(dialectStr);
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

    /**
     * Parse a COBOL file using the service's default dialect.
     */
    public Map<String, Object> parseCobolFile(Path cobolFile) throws IOException {
        return parseCobolFile(cobolFile, defaultDialect);
    }

    /**
     * Parse a COBOL file and return a small metadata map using the given
     * dialect. The returned map contains the program id, a list of detected
     * CICS commands and the dialect name. Additional fields required by other
     * services (calls and dataItems) are returned as empty collections.
     */
    public Map<String, Object> parseCobolFile(Path cobolFile, Dialect dialect) throws IOException {
        String source = Files.readString(cobolFile);

        Map<String, Object> ast = new HashMap<>();
        String programId = extractProgramId(source);
        if (programId == null || programId.isEmpty()) {
            programId = cobolFile.getFileName().toString();
        }
        ast.put(KEY_PROGRAM_ID, programId);
        ast.put(KEY_CICS_COMMANDS, extractCicsCommands(source));
        ast.put(KEY_CALLS, new HashSet<String>());
        ast.put(KEY_COPIES, new LinkedHashSet<>(extractCopybookReferences(source)));
        // Working-Storage data items
        ast.put(KEY_DATA_ITEMS, extractDataItems(source));
        // Linkage Section items (used by ENTRY ... USING ...)
        Map<String, Object> linkage = extractLinkage(source);
        ast.put(KEY_LINKAGE_ITEMS, linkage.getOrDefault(KEY_ITEMS, Collections.emptyList()));
        ast.put(KEY_LINKAGE_STRUCT_NAME, linkage.get(KEY_STRUCT_NAME));
        // ENTRY points
        ast.put(KEY_ENTRIES, extractEntries(source));
        ast.put(KEY_DIALECT, dialect.name());
        return ast;
    }

    /**
     * Extract fields from WORKING-STORAGE section and map them to Java types.
     */
    private List<Map<String, Object>> extractDataItems(String source) {
        List<Map<String, Object>> items = new ArrayList<>();
        Matcher wsMatcher = WS_SECTION_PATTERN.matcher(source);
        if (!wsMatcher.find()) {
            return items;
        }
        String wsSection = wsMatcher.group(1);
        Matcher fieldMatcher = WS_FIELD_PATTERN.matcher(wsSection);
        while (fieldMatcher.find()) {
            String name = fieldMatcher.group(1);
            String pic = fieldMatcher.group(2);
            String decl = fieldMatcher.group(0).toUpperCase(Locale.ROOT);
            if (decl.contains(COBOL_COMP3)) pic = pic + " " + COBOL_COMP3;
            if (decl.contains(COBOL_COMP5)) pic = pic + " " + COBOL_COMP5;
            String javaType = mapCobolTypeToJava(pic);
            Map<String, Object> item = new HashMap<>();
            item.put("name", toCamelCase(name));
            item.put("javaType", javaType);
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> extractLinkage(String source) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        result.put(KEY_ITEMS, items);

        Matcher lkMatcher = LINKAGE_SECTION_PATTERN.matcher(source);
        if (!lkMatcher.find()) {
            return result;
        }
        String lkSection = lkMatcher.group(1);

        // Detect main group 01 name (e.g., 01 calculator.)
        Matcher groupMatcher = LINKAGE_GROUP_PATTERN.matcher(lkSection);
        String structName; // no redundant initializer
        if (groupMatcher.find()) {
            structName = groupMatcher.group(1).toLowerCase(Locale.ROOT);
            result.put(KEY_STRUCT_NAME, structName);
        }

        // Capture level 05 fields within LINKAGE section (no deep nesting)
        Matcher fieldMatcher2 = LINKAGE_FIELD_PATTERN.matcher(lkSection);
        while (fieldMatcher2.find()) {
            String name = fieldMatcher2.group(1);
            String pic = fieldMatcher2.group(2);
            String declLine = fieldMatcher2.group(0).toUpperCase(Locale.ROOT);
            if (declLine.contains(COBOL_COMP3)) pic = pic + " " + COBOL_COMP3;
            if (declLine.contains(COBOL_COMP5)) pic = pic + " " + COBOL_COMP5;
            String javaType = mapCobolTypeToJava(pic);
            Map<String, Object> item = new HashMap<>();
            item.put("name", toCamelCase(name));
            item.put("javaType", javaType);
            items.add(item);
        }

        return result;
    }

    /**
     * Extract ENTRY "name" USING structName.
     */
    private List<Map<String, Object>> extractEntries(String source) {
        List<Map<String, Object>> entries = new ArrayList<>();
        Matcher m = ENTRY_PATTERN.matcher(source);
        while (m.find()) {
            Map<String, Object> e = new HashMap<>();
            e.put("name", m.group(1).toLowerCase(Locale.ROOT));
            e.put("using", m.group(2).toLowerCase(Locale.ROOT));
            entries.add(e);
        }
        return entries;
    }

    /**
     * Map COBOL PIC type to Java type.
     */
    private String mapCobolTypeToJava(String pic) {
        String p = pic.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        boolean comp3 = p.contains(COBOL_COMP3);
        boolean comp5 = p.contains(COBOL_COMP5);
        boolean hasV = p.contains(PIC_DECIMAL_V);

        // Packed decimal or explicit decimal -> BigDecimal
        if (comp3 || hasV) {
            return JAVA_TYPE_BIG_DECIMAL;
        }
        // Alphanumeric
        if (p.startsWith(PIC_ALPHA_PREFIX)) {
            return JAVA_TYPE_STRING;
        }
        // Numeric binary COMP-5: choose Integer/Long based on digits
        int digits = extractDigitsCount(p);
        if (comp5) {
            if (digits <= 9) return JAVA_TYPE_INTEGER;
            return JAVA_TYPE_LONG;
        }
        // Plain numeric without V: Integer/Long based on digits
        if (digits > 0) {
            if (digits <= 9) return JAVA_TYPE_INTEGER;
            return JAVA_TYPE_LONG;
        }
        return JAVA_TYPE_STRING;
    }

    private int extractDigitsCount(String pic) {
        Matcher m = DIGITS_PATTERN.matcher(pic);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        // Single 9
        if (pic.contains("9")) return 1;
        return 0;
    }

    private String toCamelCase(String name) {
        String[] parts = name.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].toLowerCase(Locale.ROOT);
            if (i == 0) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return sb.toString();
    }

    // Helper methods for COBOL parsing
    private String extractProgramId(String source) {
        Matcher m = PROGRAM_ID_PATTERN.matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private Set<String> extractCicsCommands(String source) {
        Set<String> cmds = new HashSet<>();
        Matcher m = CICS_COMMAND_PATTERN.matcher(source);
        while (m.find()) {
            cmds.add(m.group(1).toUpperCase(Locale.ROOT));
        }
        return cmds;
    }

    /**
     * Parse a COBOL copybook. For this lightweight implementation only the
     * file name and dialect are returned.
     */
    public Map<String, Object> parseCopybook(Path copybookFile, Dialect dialect) {
        Map<String, Object> ast = new HashMap<>();
        String programId = copybookFile.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        ast.put(KEY_PROGRAM_ID, programId);
        ast.put(KEY_DATA_ITEMS, new ArrayList<>());
        ast.put(KEY_DIALECT, dialect.name());
        return ast;
    }

    /**
     * Supported COBOL dialects.
     */
    public enum Dialect {
        IBM,
        GNU,
        MICRO_FOCUS;

        /**
         * Parse a string value into a {@link Dialect}. If the value does not
         * match any known dialect the default IBM dialect is returned.
         */
        public static Dialect fromString(String value) {
            if (value == null) {
                return IBM;
            }
            return switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "GNU", "GNUCOBOL" -> GNU;
                case "MICROFOCUS", "MICRO_FOCUS", "MF" -> MICRO_FOCUS;
                case "IBM" -> IBM;
                default -> IBM;
            };
        }
    }
}
