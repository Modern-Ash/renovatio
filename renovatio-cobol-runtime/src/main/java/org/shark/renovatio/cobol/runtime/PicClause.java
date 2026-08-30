package org.shark.renovatio.cobol.runtime;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses COBOL {@code PICTURE} clauses (optionally with a trailing
 * {@code USAGE} token) into a {@link PicType}.
 */
public final class PicClause {

    private static final Pattern REPEAT = Pattern.compile("([9XAP])\\((\\d+)\\)");

    private PicClause() {
    }

    public static PicType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("empty picture clause");
        }
        String s = raw.trim().toUpperCase(Locale.ROOT);
        s = s.replaceAll("^PICTURE\\b", "PIC").replaceAll("^PIC\\b", "").replaceAll("^\\s*IS\\b", "").trim();

        PicType.Usage usage = detectUsage(s);
        boolean separateSign = s.matches(
                ".*\\bSIGN(?:\\s+IS)?\\s+(?:LEADING|TRAILING)(?:\\s+SEPARATE(?:\\s+CHARACTER)?)?\\b.*");
        // Strip usage tokens so they do not pollute symbol counting.
        String pic = s.replaceAll(
                "\\b(PACKED-DECIMAL|COMPUTATIONAL-3|COMPUTATIONAL-5|COMPUTATIONAL-4|COMPUTATIONAL|COMP-3|COMP-5|COMP-4|COMP|BINARY|DISPLAY|USAGE)\\b",
                " ").replaceAll(
                "\\bSIGN(?:\\s+IS)?\\s+(?:LEADING|TRAILING)(?:\\s+SEPARATE(?:\\s+CHARACTER)?)?\\b",
                " ").trim();

        String symbols = expandRepeats(pic);

        boolean signed = separateSign || symbols.startsWith("S");
        if (symbols.startsWith("S")) {
            symbols = symbols.substring(1);
        }

        if (symbols.indexOf('X') >= 0) {
            return new PicType(PicType.Category.ALPHANUMERIC, count(symbols, 'X'), 0, false, usage);
        }
        if (symbols.indexOf('A') >= 0) {
            return new PicType(PicType.Category.ALPHABETIC, count(symbols, 'A'), 0, false, usage);
        }

        int vIndex = symbols.indexOf('V');
        int scale = vIndex < 0 ? 0 : count(symbols.substring(vIndex + 1), '9');
        int digits = count(symbols, '9');
        return new PicType(PicType.Category.NUMERIC, digits, scale, signed, usage);
    }

    private static PicType.Usage detectUsage(String s) {
        if (s.matches(".*\\b(COMP-3|COMPUTATIONAL-3|PACKED-DECIMAL)\\b.*")) {
            return PicType.Usage.COMP_3;
        }
        if (s.matches(".*\\b(COMP-5|COMPUTATIONAL-5)\\b.*")) {
            return PicType.Usage.COMP_5;
        }
        if (s.matches(".*\\b(COMP|COMP-4|COMPUTATIONAL|COMPUTATIONAL-4|BINARY)\\b.*")) {
            return PicType.Usage.COMP;
        }
        return PicType.Usage.DISPLAY;
    }

    private static String expandRepeats(String pic) {
        Matcher m = REPEAT.matcher(pic);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, String.valueOf(m.group(1)).repeat(Integer.parseInt(m.group(2))));
        }
        m.appendTail(sb);
        return sb.toString().replaceAll("[^SVXAP9]", "");
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }
}
