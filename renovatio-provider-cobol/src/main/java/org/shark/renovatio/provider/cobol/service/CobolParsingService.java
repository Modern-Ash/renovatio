package org.shark.renovatio.provider.cobol.service;

import org.shark.renovatio.provider.cobol.domain.CobolProgram;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.PerformanceMetrics;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;
import org.springframework.stereotype.Service;

import java.io.File;
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
 * module can be built in environments without network access.  The parsing
 * performed here is intentionally simplistic and relies on regular
 * expressions to extract a few pieces of information such as the program id,
 * embedded SQL statements and simple CICS commands.</p>
 * ightweight regular-expression based parser.
 */
@Service
public class CobolParsingService {

    private final Dialect defaultDialect;

    public CobolParsingService() {
        this(Dialect.IBM);
    }

    public CobolParsingService(Dialect dialect) {
        this.defaultDialect = dialect == null ? Dialect.IBM : dialect;
    }

    public Dialect getDefaultDialect() {
        return defaultDialect;
    }

    /**
     * Locate COBOL source files inside a workspace.
     */
    public List<Path> findCobolFiles(Path workspacePath) throws IOException {
        List<Path> cobolFiles = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(workspacePath)) {
            walkStream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".cob") ||
                                name.endsWith(".cobol") ||
                                name.endsWith(".cbl") ||
                                name.endsWith(".cpy");
                    })
                    .forEach(cobolFiles::add);
        }
        return cobolFiles;
    }

    public List<Path> findCopybooks(Path workspacePath) throws IOException {
        List<Path> copybooks = new ArrayList<>();
        try (Stream<Path> walkStream = Files.walk(workspacePath)) {
            walkStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".cpy"))
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
        Pattern pattern = Pattern.compile("EXEC\\s+SQL(.*?)END-EXEC", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(cobolSource);
        while (matcher.find()) {
            String sql = matcher.group(1).trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
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
        List<Path> cobolFiles = findCobolFiles(root);

        List<CobolProgram> programs = new ArrayList<>();
        for (Path cobolFile : cobolFiles) {
            Map<String, Object> metadata = parseCobolFile(cobolFile, dialect);
            metadata.put("filePath", cobolFile.toString());
            CobolProgram program = new CobolProgram();
            program.setProgramId((String) metadata.get("programId"));
            program.setProgramName((String) metadata.get("programId"));
            program.setMetadata(metadata);
            programs.add(program);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("programs", programs);

        AnalyzeResult result = new AnalyzeResult(true, "Parsed " + programs.size() + " COBOL files");
        result.setData(data);

        long elapsed = System.nanoTime() - start;
        result.setPerformance(new PerformanceMetrics(elapsed / 1_000_000));
        return result;
    }

    private Dialect resolveDialect(NqlQuery query, Workspace workspace) {
        String dialectStr = null;
        if (query != null && query.getParameters() != null) {
            Object param = query.getParameters().get("dialect");
            if (param != null) {
                dialectStr = param.toString();
            }
        }
        if (dialectStr == null && workspace != null && workspace.getMetadata() != null) {
            Object meta = workspace.getMetadata().get("dialect");
            if (meta != null) {
                dialectStr = meta.toString();
            }
        }

        return Dialect.fromString(dialectStr);
    }

    /**
     * Attempts to parse a COBOL file using the ProLeap parser via reflection.
     * If the library is not available or an error occurs, {@code null} is
     * returned so that the caller can fall back to another strategy.
     */
    private CobolProgram parseWithProLeap(Path cobolFile) {
        try {
            // We invoke ProLeap via reflection to avoid a hard dependency.
            Class<?> runnerClass = Class.forName("io.proleap.cobol.asg.runner.CobolParserRunnerImpl");
            Object runner = runnerClass.getDeclaredConstructor().newInstance();

            // ProLeap requires a source format enum; we attempt to resolve it
            // but default to AUTO when unavailable.
            Class<?> formatEnum = Class.forName("io.proleap.cobol.asg.params.CobolSourceFormatEnum");
            Object format = Enum.valueOf((Class<Enum>) formatEnum, "AUTO");

            // Call analyzeFile(File, CobolSourceFormatEnum)
            runnerClass.getMethod("analyzeFile", File.class, formatEnum)
                    .invoke(runner, cobolFile.toFile(), format);

            // We only need metadata for now; reuse regex parsing for structure
            CobolProgram program = parseWithRegex(cobolFile);
            if (program != null && program.getMetadata() != null) {
                program.getMetadata().put("parser", "proleap");
            }
            return program;
        } catch (Throwable t) {
            // Any failure leads to a null result so the caller can fallback
            return null;
        }
    }

    /**
     * Attempts to parse a COBOL file using the Koopa parser via reflection.
     * Returns {@code null} if Koopa is not on the classpath or parsing fails.
     */
    private CobolProgram parseWithKoopa(Path cobolFile) {
        try {
            Class<?> parserClass = Class.forName("koopa.parsers.cobol.CobolParser");
            Object parser = parserClass.getDeclaredConstructor().newInstance();

            // Koopa exposes a parse(File) method which returns a parse tree.
            parserClass.getMethod("parse", File.class).invoke(parser, cobolFile.toFile());

            CobolProgram program = parseWithRegex(cobolFile);
            if (program != null && program.getMetadata() != null) {
                program.getMetadata().put("parser", "koopa");
            }
            return program;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Checks whether a given class is available on the classpath.
     */
    private boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Parse a COBOL file using the service's default dialect.
     */
    public Map<String, Object> parseCobolFile(Path cobolFile) throws IOException {
        return parseCobolFile(cobolFile, defaultDialect);
    }

    /**
     * Parse a COBOL file and return a very small metadata map using the given
     * dialect.  The returned map contains the program id, a list of detected
     * CICS commands and the dialect name.  Additional fields required by other
     * services (calls, copies, dataItems) are returned as empty collections.
     */
    public Map<String, Object> parseCobolFile(Path cobolFile, Dialect dialect) throws IOException {
        String source = Files.readString(cobolFile);

        Map<String, Object> ast = new HashMap<>();
        String programId = extractProgramId(source);
        if (programId == null || programId.isEmpty()) {
            programId = cobolFile.getFileName().toString();
        }
        ast.put("programId", programId);
        ast.put("cicsCommands", extractCicsCommands(source));
        ast.put("calls", new HashSet<String>());
        ast.put("copies", new HashSet<String>());
        // Working-Storage data items
        ast.put("dataItems", extractDataItems(source));
        // Linkage Section items (used by ENTRY ... USING ...)
        Map<String, Object> linkage = extractLinkage(source);
        ast.put("linkageItems", linkage.getOrDefault("items", Collections.emptyList()));
        ast.put("linkageStructName", linkage.get("structName"));
        // ENTRY points
        ast.put("entries", extractEntries(source));
        ast.put("dialect", dialect.name());
        return ast;
    }

    /**
     * Extrae los campos de la sección WORKING-STORAGE y los mapea a tipos Java.
     */
    private List<Map<String, Object>> extractDataItems(String source) {
        List<Map<String, Object>> items = new ArrayList<>();
        // Buscar la sección WORKING-STORAGE
        Pattern wsPattern = Pattern.compile("WORKING-STORAGE SECTION\\.(.*?)(PROCEDURE DIVISION\\.|\\Z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher wsMatcher = wsPattern.matcher(source);
        if (wsMatcher.find()) {
            String wsSection = wsMatcher.group(1);
            // Capturar PIC y opcional COMP-3/COMP-5/SIGN
            Pattern fieldPattern = Pattern.compile("^\\s*\\d+\\s+([A-Z0-9-]+)\\s+PIC\\s+([A-Z0-9\\(\\)V]+)(?:\\s+COMP-?\\d+)?(?:\\s+SIGNED)?\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            Matcher fieldMatcher = fieldPattern.matcher(wsSection);
            while (fieldMatcher.find()) {
                String name = fieldMatcher.group(1);
                String pic = fieldMatcher.group(2);
                // Intentar capturar sufijos COMP-3/COMP-5
                String fullDeclLine = fieldMatcher.group(0).toUpperCase(Locale.ROOT);
                if (fullDeclLine.contains("COMP-3")) pic = pic + " COMP-3";
                if (fullDeclLine.contains("COMP-5")) pic = pic + " COMP-5";
                String javaType = mapCobolTypeToJava(pic);
                Map<String, Object> item = new HashMap<>();
                item.put("name", toCamelCase(name));
                item.put("javaType", javaType);
                items.add(item);
            }
        }
        return items;
    }

    /**
     * Extrae LINKAGE SECTION: nombre del grupo 01 usado en ENTRY USING y sus campos nivel 05.
     */
    private Map<String, Object> extractLinkage(String source) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        result.put("items", items);

        Pattern lkPattern = Pattern.compile("LINKAGE SECTION\\.(.*?)(PROCEDURE DIVISION\\.|\\Z)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher lkMatcher = lkPattern.matcher(source);
        if (!lkMatcher.find()) {
            return result;
        }
        String lkSection = lkMatcher.group(1);

        // Detectar nombre del grupo 01 principal (ej: 01 calculator.)
        Pattern groupPattern = Pattern.compile("^\\s*01\\s+([A-Z0-9-]+)\\s*\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher groupMatcher = groupPattern.matcher(lkSection);
        String structName = null;
        if (groupMatcher.find()) {
            structName = groupMatcher.group(1).toLowerCase(Locale.ROOT);
            result.put("structName", structName);
        }

        // Capturar los campos nivel 05 dentro de la sección de LINKAGE (sin profundizar niveles)
        Pattern fieldPattern = Pattern.compile("^\\s*05\\s+([A-Z0-9-]+)\\s+PIC\\s+([A-Z0-9\\(\\)V]+)(?:\\s+COMP-?\\d+)?(?:\\s+SIGNED)?\\.", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
        Matcher fieldMatcher = fieldPattern.matcher(lkSection);
        while (fieldMatcher.find()) {
            String name = fieldMatcher.group(1);
            String pic = fieldMatcher.group(2);
            String declLine = fieldMatcher.group(0).toUpperCase(Locale.ROOT);
            if (declLine.contains("COMP-3")) pic = pic + " COMP-3";
            if (declLine.contains("COMP-5")) pic = pic + " COMP-5";
            String javaType = mapCobolTypeToJava(pic);
            Map<String, Object> item = new HashMap<>();
            item.put("name", toCamelCase(name));
            item.put("javaType", javaType);
            items.add(item);
        }

        return result;
    }

    /**
     * Extrae ENTRY "name" USING structName.
     */
    private List<Map<String, Object>> extractEntries(String source) {
        List<Map<String, Object>> entries = new ArrayList<>();
        Pattern entryPattern = Pattern.compile("ENTRY\\s+\"([A-Z0-9_-]+)\"\\s+USING\\s+([A-Z0-9-]+)\\.", Pattern.CASE_INSENSITIVE);
        Matcher m = entryPattern.matcher(source);
        while (m.find()) {
            Map<String, Object> e = new HashMap<>();
            e.put("name", m.group(1).toLowerCase(Locale.ROOT));
            e.put("using", m.group(2).toLowerCase(Locale.ROOT));
            entries.add(e);
        }
        return entries;
    }

    /**
     * Mapea el tipo PIC COBOL a tipo Java.
     */
    private String mapCobolTypeToJava(String pic) {
        String p = pic.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
        boolean comp3 = p.contains("COMP-3");
        boolean comp5 = p.contains("COMP-5");
        boolean hasV = p.contains("V");

        // Packed decimal or explicit decimal -> BigDecimal
        if (comp3 || hasV) {
            return "BigDecimal";
        }
        // Alphanumeric
        if (p.startsWith("X")) {
            return "String";
        }
        // Numeric binary COMP-5: choose Integer/Long based on digits
        int digits = extractDigitsCount(p);
        if (comp5) {
            if (digits <= 9) return "Integer";
            return "Long";
        }
        // Plain numeric without V: Integer/Long based on digits
        if (digits > 0) {
            if (digits <= 9) return "Integer";
            return "Long";
        }
        return "String";
    }

    private int extractDigitsCount(String pic) {
        Matcher m = Pattern.compile("9\\((\\d+)\\)").matcher(pic);
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

    /**
     * Parses a COBOL file using regular expressions and returns a CobolProgram object.
     */
    private CobolProgram parseWithRegex(Path cobolFile) throws IOException {
        String source = Files.readString(cobolFile);
        Map<String, Object> metadata = new HashMap<>();
        String programId = extractProgramId(source);
        if (programId == null || programId.isEmpty()) {
            programId = cobolFile.getFileName().toString();
        }
        metadata.put("programId", programId);
        metadata.put("cicsCommands", extractCicsCommands(source));
        metadata.put("calls", new HashSet<String>());
        metadata.put("copies", new HashSet<String>());
        metadata.put("dataItems", new ArrayList<>());
        // Also expose linkage and entries for regex path
        Map<String, Object> linkage = extractLinkage(source);
        metadata.put("linkageItems", linkage.getOrDefault("items", Collections.emptyList()));
        metadata.put("linkageStructName", linkage.get("structName"));
        metadata.put("entries", extractEntries(source));
        metadata.put("dialect", defaultDialect.name());

        CobolProgram program = new CobolProgram();
        program.setProgramId(programId);
        program.setProgramName(programId);
        program.setMetadata(metadata);
        return program;
    }

    // Métodos auxiliares para parsing COBOL
    private String extractProgramId(String source) {
        Pattern pattern = Pattern.compile("PROGRAM-ID\\.\\s*([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private Set<String> extractCicsCommands(String source) {
        Set<String> cmds = new HashSet<>();
        Pattern pattern = Pattern.compile("EXEC\\s+CICS\\s+([A-Z0-9-]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(source);
        while (m.find()) {
            cmds.add(m.group(1).toUpperCase(Locale.ROOT));
        }
        return cmds;
    }

    /**
     * Parse a COBOL copybook. For this lightweight implementation only the
     * file name and dialect are returned.
     */
    public Map<String, Object> parseCopybook(Path copybookFile, Dialect dialect) throws IOException {
        Map<String, Object> ast = new HashMap<>();
        String programId = copybookFile.getFileName().toString().replaceFirst("\\.[^.]+$", "");
        ast.put("programId", programId);
        ast.put("dataItems", new ArrayList<>());
        ast.put("dialect", dialect.name());
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
            switch (value.trim().toUpperCase(Locale.ROOT)) {
                case "GNU":
                case "GNUCOBOL":
                    return GNU;
                case "MICROFOCUS":
                case "MICRO_FOCUS":
                case "MF":
                    return MICRO_FOCUS;
                case "IBM":
                default:
                    return IBM;
            }
        }
    }
}
