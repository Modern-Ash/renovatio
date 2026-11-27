package org.modernash.renovatio.cobol.ir.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One {@code EXEC CICS} command split into its option keywords: words without an argument
 * ({@code READ}, {@code UPDATE}, {@code GTEQ}) in order, and options with their argument text
 * ({@code DATASET(WS-USRSEC-FILE)}) by upper-case name.
 */
public record CobolCicsCommand(List<String> words, Map<String, String> options) {

    private static final Pattern OPTION = Pattern.compile(
            "([A-Za-z][A-Za-z0-9-]*)(?:\\s*\\(((?:[^()']|'[^']*'|\\([^()]*\\))*)\\))?");

    public CobolCicsCommand {
        words = List.copyOf(words);
        options = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(options));
    }

    /** Parses the statement text ({@code EXEC CICS ... END-EXEC}); null when it is not a CICS command. */
    public static CobolCicsCommand parse(String text) {
        if (text == null || !text.trim().toUpperCase(Locale.ROOT).startsWith("EXEC CICS")) {
            return null;
        }
        String body = text.trim().replaceAll("(?i)^EXEC\\s+CICS\\s+", "")
                .replaceAll("(?i)\\s*END-EXEC\\s*\\.?\\s*$", "").trim();
        List<String> words = new ArrayList<>();
        Map<String, String> options = new LinkedHashMap<>();
        Matcher matcher = OPTION.matcher(body);
        while (matcher.find()) {
            String name = matcher.group(1).toUpperCase(Locale.ROOT);
            if (matcher.group(2) == null) words.add(name);
            else options.put(name, matcher.group(2).trim());
        }
        return new CobolCicsCommand(words, options);
    }

    public String verb() {
        return words.isEmpty() ? "" : words.get(0);
    }

    /** The file a file-control command names: DATASET or FILE. */
    public String dataset() {
        return options.getOrDefault("DATASET", options.get("FILE"));
    }

    public boolean fileCommand() {
        return dataset() != null && List.of("READ", "WRITE", "REWRITE", "DELETE", "STARTBR", "READNEXT",
                "READPREV", "ENDBR", "RESETBR", "UNLOCK").contains(verb());
    }
}
