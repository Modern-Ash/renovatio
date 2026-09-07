package org.shark.renovatio.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.Dataset;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.Diagnostic;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.SourceFile;
import org.shark.renovatio.api.dto.WorkbenchSourceExplorerDto.Symbol;
import org.springframework.stereotype.Service;

/**
 * Builds the read-only {@link WorkbenchSourceExplorerDto} for the Workbench
 * Source Explorer. The parsing is deterministic and regex-based on purpose: it
 * never invokes the semantic pipeline and never writes project state.
 */
@Service
public class WorkbenchSourceExplorerService {

    private static final int MAX_DEPTH = 8;
    private static final Pattern DIVISION = Pattern.compile("^\\s*([A-Z]+)\\s+DIVISION\\s*\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern SECTION = Pattern.compile("^\\s*([A-Z0-9][A-Z0-9-]*)\\s+SECTION\\s*\\.", Pattern.CASE_INSENSITIVE);
    private static final Pattern PARAGRAPH = Pattern.compile("^ {0,4}([A-Z0-9][A-Z0-9-]*)\\s*\\.\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PERFORM = Pattern.compile("\\bPERFORM\\s+([A-Z0-9][A-Z0-9-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL = Pattern.compile("\\bCALL\\s+[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern COPY = Pattern.compile("\\bCOPY\\s+([A-Z0-9][A-Z0-9-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXEC_SQL = Pattern.compile("\\bEXEC\\s+SQL\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXEC_CICS = Pattern.compile("\\bEXEC\\s+CICS\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PROGRAM_ID = Pattern.compile("\\bPROGRAM-ID\\s*\\.\\s*([A-Z0-9][A-Z0-9-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern JCL_STEP = Pattern.compile("^//([A-Z0-9#@$]+)\\s+EXEC\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JCL_DD = Pattern.compile("^//([A-Z0-9#@$]+)\\s+DD\\s+(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JCL_DSN = Pattern.compile("DSN(?:AME)?=([A-Z0-9$#@.\\-()]+)", Pattern.CASE_INSENSITIVE);

    private final ProjectService projects;

    public WorkbenchSourceExplorerService(ProjectService projects) {
        this.projects = projects;
    }

    public WorkbenchSourceExplorerDto summary(String projectId) throws IOException {
        Path root = projects.getProject(projectId)
                .map(project -> Path.of(project.getWorkspacePath()).toAbsolutePath().normalize())
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        return explore(root);
    }

    /** Package-visible entry point that operates purely on a filesystem root. */
    public WorkbenchSourceExplorerDto explore(Path root) throws IOException {
        List<Path> assets;
        try (Stream<Path> paths = Files.walk(root, MAX_DEPTH)) {
            assets = paths.filter(Files::isRegularFile)
                    .filter(WorkbenchSourceExplorerService::isLegacyAsset)
                    .sorted()
                    .toList();
        }
        List<SourceFile> files = new ArrayList<>();
        Map<String, TreeSet<String>> datasetReferences = new LinkedHashMap<>();
        for (Path asset : assets) {
            files.add(describe(root, asset, datasetReferences));
        }
        files.sort(Comparator.comparing(SourceFile::path));
        List<Dataset> datasets = datasetReferences.entrySet().stream()
                .map(entry -> new Dataset(entry.getKey(), entry.getKey(), List.copyOf(entry.getValue())))
                .sorted(Comparator.comparing(Dataset::id))
                .toList();
        return new WorkbenchSourceExplorerDto(List.copyOf(files), datasets);
    }

    private static boolean isLegacyAsset(Path path) {
        String ext = extension(path);
        return switch (ext) {
            case ".cbl", ".cob", ".cpy", ".jcl" -> true;
            default -> false;
        };
    }

    private SourceFile describe(Path root, Path asset, Map<String, TreeSet<String>> datasetReferences) throws IOException {
        byte[] bytes = Files.readAllBytes(asset);
        String content = new String(bytes, StandardCharsets.UTF_8);
        String relative = root.relativize(asset).toString().replace(asset.getFileSystem().getSeparator(), "/");
        String name = asset.getFileName().toString();
        String ext = extension(asset);
        String kind = switch (ext) {
            case ".cpy" -> "copybook";
            case ".jcl" -> "jcl";
            default -> "cobol-program";
        };
        String[] lines = content.split("\n", -1);
        List<Symbol> symbols = new ArrayList<>();
        List<Diagnostic> diagnostics = new ArrayList<>();

        String programName = ext.equals(".jcl") ? name : programName(content, name);
        if (ext.equals(".jcl")) {
            parseJcl(lines, name, symbols, datasetReferences);
        } else {
            parseCobol(lines, programName, symbols, diagnostics, root);
        }

        String analysisStatus = analysisStatus(ext, symbols, content);
        return new SourceFile(
                relative,
                name,
                kind,
                relative,
                "sha256:" + sha256(bytes),
                encoding(bytes),
                analysisStatus,
                List.copyOf(symbols),
                List.copyOf(diagnostics));
    }

    private void parseCobol(String[] lines, String program, List<Symbol> symbols, List<Diagnostic> diagnostics, Path root) {
        String currentDivision = null;
        String currentSection = null;
        String currentParent = null;
        int paragraphsInProcedure = 0;
        boolean inProcedure = false;
        for (int index = 0; index < lines.length; index++) {
            String raw = lines[index];
            String line = stripSequenceArea(raw);
            if (line.isBlank() || isComment(raw)) {
                continue;
            }
            int lineNumber = index + 1;

            Matcher division = DIVISION.matcher(line);
            if (division.find()) {
                currentDivision = division.group(1).toUpperCase(Locale.ROOT) + " DIVISION";
                currentSection = null;
                currentParent = symbolId(program, "division", currentDivision);
                inProcedure = currentDivision.startsWith("PROCEDURE");
                symbols.add(new Symbol(currentParent, "division", currentDivision, lineNumber,
                        column(raw, line), null, ir(program, "division", currentDivision)));
                continue;
            }
            Matcher section = SECTION.matcher(line);
            if (section.find() && !line.toUpperCase(Locale.ROOT).contains("DIVISION")) {
                currentSection = section.group(1).toUpperCase(Locale.ROOT);
                String sectionId = symbolId(program, "section", currentSection);
                symbols.add(new Symbol(sectionId, "section", currentSection, lineNumber, column(raw, line),
                        currentDivision == null ? null : symbolId(program, "division", currentDivision),
                        ir(program, "section", currentSection)));
                currentParent = sectionId;
                continue;
            }
            if (inProcedure) {
                Matcher paragraph = PARAGRAPH.matcher(line);
                if (paragraph.matches() && !line.toUpperCase(Locale.ROOT).contains(" SECTION")) {
                    String paragraphName = paragraph.group(1).toUpperCase(Locale.ROOT);
                    String paragraphId = symbolId(program, "paragraph", paragraphName);
                    symbols.add(new Symbol(paragraphId, "paragraph", paragraphName, lineNumber, column(raw, line),
                            currentSection == null
                                    ? (currentDivision == null ? null : symbolId(program, "division", currentDivision))
                                    : symbolId(program, "section", currentSection),
                            ir(program, "paragraph", paragraphName)));
                    currentParent = paragraphId;
                    paragraphsInProcedure++;
                    continue;
                }
            }

            addMatches(PERFORM, line, raw, lineNumber, "perform", program, currentParent, symbols);
            addMatches(CALL, line, raw, lineNumber, "call", program, currentParent, symbols);
            for (String copybook : allMatches(COPY, line)) {
                symbols.add(new Symbol(symbolId(program, "copy", copybook), "copy", copybook, lineNumber,
                        column(raw, line), currentParent, ir(program, "copy", copybook)));
                if (!copybookExists(root, copybook)) {
                    diagnostics.add(new Diagnostic("warning", "COPY " + copybook + " not found in workspace", lineNumber));
                }
            }
            if (EXEC_SQL.matcher(line).find()) {
                symbols.add(new Symbol(symbolId(program, "exec-sql", "L" + lineNumber), "exec-sql", "EXEC SQL",
                        lineNumber, column(raw, line), currentParent, ir(program, "exec-sql", "L" + lineNumber)));
            }
            if (EXEC_CICS.matcher(line).find()) {
                symbols.add(new Symbol(symbolId(program, "exec-cics", "L" + lineNumber), "exec-cics", "EXEC CICS",
                        lineNumber, column(raw, line), currentParent, ir(program, "exec-cics", "L" + lineNumber)));
            }
        }
        if (inProcedureSeen(symbols) && paragraphsInProcedure == 0) {
            diagnostics.add(new Diagnostic("info", "PROCEDURE DIVISION has no recognizable paragraphs", 0));
        }
    }

    private void parseJcl(String[] lines, String member, List<Symbol> symbols, Map<String, TreeSet<String>> datasetReferences) {
        for (int index = 0; index < lines.length; index++) {
            String raw = lines[index];
            String line = raw.stripTrailing();
            if (line.startsWith("//*") || line.isBlank()) {
                continue;
            }
            int lineNumber = index + 1;
            Matcher step = JCL_STEP.matcher(line);
            if (step.find()) {
                symbols.add(new Symbol("jcl:" + member + "#step:" + step.group(1), "jcl-step", step.group(1),
                        lineNumber, 0, null, "ir://" + member + "/jcl-step/" + step.group(1)));
            }
            Matcher dd = JCL_DD.matcher(line);
            if (dd.find()) {
                symbols.add(new Symbol("jcl:" + member + "#dd:" + dd.group(1), "jcl-dd", dd.group(1),
                        lineNumber, 0, null, "ir://" + member + "/jcl-dd/" + dd.group(1)));
            }
            Matcher dsn = JCL_DSN.matcher(line);
            while (dsn.find()) {
                String dataset = dsn.group(1).toUpperCase(Locale.ROOT);
                datasetReferences.computeIfAbsent(dataset, key -> new TreeSet<>()).add(member);
            }
        }
    }

    private void addMatches(Pattern pattern, String line, String raw, int lineNumber, String kind, String program,
            String parent, List<Symbol> symbols) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String target = matcher.group(1).toUpperCase(Locale.ROOT);
            symbols.add(new Symbol(symbolId(program, kind, target + "@" + lineNumber), kind, target, lineNumber,
                    columnAt(raw, matcher.start(1)), parent, ir(program, kind, target)));
        }
    }

    private List<String> allMatches(Pattern pattern, String line) {
        List<String> values = new ArrayList<>();
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            values.add(matcher.group(1).toUpperCase(Locale.ROOT));
        }
        return values;
    }

    private boolean inProcedureSeen(List<Symbol> symbols) {
        return symbols.stream().anyMatch(symbol -> "division".equals(symbol.kind())
                && symbol.name().startsWith("PROCEDURE"));
    }

    private boolean copybookExists(Path root, String copybook) {
        try (Stream<Path> paths = Files.walk(root, MAX_DEPTH)) {
            return paths.filter(Files::isRegularFile).anyMatch(path -> {
                String fileName = path.getFileName().toString();
                int dot = fileName.lastIndexOf('.');
                String base = dot < 0 ? fileName : fileName.substring(0, dot);
                return base.equalsIgnoreCase(copybook) && extension(path).equals(".cpy");
            });
        } catch (IOException exception) {
            return true;
        }
    }

    private static final Pattern DATA_ITEM = Pattern.compile("^\\s*(\\d{1,2})\\s+[A-Z0-9][A-Z0-9-]*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

    private static String analysisStatus(String extension, List<Symbol> symbols, String content) {
        boolean hasStructure = symbols.stream().anyMatch(symbol ->
                "division".equals(symbol.kind()) || "jcl-step".equals(symbol.kind()));
        if (hasStructure) {
            return "parsed";
        }
        boolean hasContent = !symbols.isEmpty() || DATA_ITEM.matcher(content).find();
        return hasContent ? "partial" : "unsupported";
    }

    private static String programName(String content, String fallback) {
        Matcher matcher = PROGRAM_ID.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).toUpperCase(Locale.ROOT);
        }
        int dot = fallback.lastIndexOf('.');
        return (dot < 0 ? fallback : fallback.substring(0, dot)).toUpperCase(Locale.ROOT);
    }

    private static String symbolId(String program, String kind, String name) {
        return program + "#" + kind + ":" + name;
    }

    private static String ir(String program, String kind, String name) {
        return "ir://" + program + "/" + kind + "/" + name;
    }

    private static String stripSequenceArea(String raw) {
        String line = raw.stripTrailing();
        if (line.length() > 72) {
            line = line.substring(0, 72);
        }
        if (line.length() >= 7) {
            return line.substring(6);
        }
        return line;
    }

    private static boolean isComment(String raw) {
        return raw.length() >= 7 && (raw.charAt(6) == '*' || raw.charAt(6) == '/');
    }

    private static int column(String raw, String logical) {
        int trimmed = logical.length() - logical.stripLeading().length();
        int offset = raw.length() >= 7 ? 6 : 0;
        return offset + trimmed + 1;
    }

    private static int columnAt(String raw, int logicalIndex) {
        int offset = raw.length() >= 7 ? 6 : 0;
        return offset + logicalIndex + 1;
    }

    private static String extension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot);
    }

    private static String encoding(byte[] bytes) {
        for (byte value : bytes) {
            if ((value & 0xFF) >= 0x80) {
                return "UTF-8";
            }
        }
        return "US-ASCII";
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
