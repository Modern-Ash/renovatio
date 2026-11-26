package org.modernash.renovatio.cobol.ir.parser;

import org.modernash.renovatio.cobol.ir.model.CobolRecordLayout;
import org.modernash.renovatio.cobol.runtime.PicClause;
import org.modernash.renovatio.cobol.runtime.PicType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives the DISPLAY record layout of every group item in a DATA DIVISION region.
 *
 * <p>Every data description entry is scanned, not only the ones the data-item extractor
 * models, so a group whose subtree holds anything that cannot be encoded exactly (OCCURS,
 * REDEFINES, binary or packed usage, edited pictures, an entry this scanner cannot read) is
 * reported as opaque instead of silently getting a shorter layout.</p>
 */
final class CobolRecordLayoutExtractor {

    private static final Pattern ENTRY_START = Pattern.compile(
            "(?m)^[ \\t]*(\\d{1,2})[ \\t]+([A-Z0-9][A-Z0-9-]*)(?=[\\s.]|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTRY_END = Pattern.compile("\\.(?=\\s|$)");
    private static final Pattern PICTURE = Pattern.compile(
            "\\bPIC(?:TURE)?\\s+(?:IS\\s+)?(\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern USAGE_TOKEN = Pattern.compile(
            "\\b(COMP(?:UTATIONAL)?(?:-[0-9])?|BINARY|PACKED-DECIMAL|POINTER|INDEX)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OCCURS = Pattern.compile("\\bOCCURS\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern REDEFINES = Pattern.compile("\\bREDEFINES\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern SEPARATE_SIGN = Pattern.compile("\\bSEPARATE\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_LITERAL = Pattern.compile(
            "\\bVALUES?\\s+(?:IS\\s+|ARE\\s+)?('(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|SPACES?|ZEROS?|ZEROES)",
            Pattern.CASE_INSENSITIVE);
    /** Picture symbols other than 9, X, A, S and V: editing or scaling, not plain DISPLAY data. */
    private static final Pattern EDITED_OR_SCALED = Pattern.compile("[ZB0/,.+\\-*$CRDBP]");

    private record Entry(int level, String name, String text) {
        boolean condition() {
            return level == 88 || level == 66;
        }
    }

    private CobolRecordLayoutExtractor() {
    }

    static Map<String, CobolRecordLayout> extract(String dataSection) {
        List<Entry> entries = entries(dataSection);
        Map<String, Integer> nameCounts = new HashMap<>();
        for (Entry entry : entries) {
            if (!entry.condition() && !entry.name().equals("FILLER")) {
                nameCounts.merge(entry.name(), 1, Integer::sum);
            }
        }
        Map<String, CobolRecordLayout> layouts = new LinkedHashMap<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry group = entries.get(i);
            if (group.condition() || PICTURE.matcher(group.text()).find() || group.name().equals("FILLER")) {
                continue;
            }
            layouts.putIfAbsent(group.name(), layout(group, descendants(entries, i), nameCounts));
        }
        return layouts;
    }

    private static List<Entry> entries(String dataSection) {
        List<Entry> entries = new ArrayList<>();
        Matcher start = ENTRY_START.matcher(dataSection);
        List<int[]> spans = new ArrayList<>();
        while (start.find()) {
            spans.add(new int[] {start.start(), start.end()});
        }
        for (int k = 0; k < spans.size(); k++) {
            int from = spans.get(k)[0];
            int to = k + 1 < spans.size() ? spans.get(k + 1)[0] : dataSection.length();
            String raw = dataSection.substring(from, to);
            Matcher end = ENTRY_END.matcher(raw);
            String text = end.find() ? raw.substring(0, end.start()) : raw;
            Matcher head = ENTRY_START.matcher(text);
            if (!head.find()) {
                continue;
            }
            entries.add(new Entry(Integer.parseInt(head.group(1)), head.group(2).toUpperCase(Locale.ROOT),
                    text.substring(head.end()).replaceAll("\\s+", " ").trim()));
        }
        return entries;
    }

    /** Entries nested under {@code entries[index]}: those after it until a level not deeper than it. */
    private static List<Entry> descendants(List<Entry> entries, int index) {
        Entry group = entries.get(index);
        List<Entry> result = new ArrayList<>();
        for (int j = index + 1; j < entries.size(); j++) {
            Entry candidate = entries.get(j);
            if (candidate.condition()) {
                continue;
            }
            if (candidate.level() <= group.level() || candidate.level() == 77) {
                break;
            }
            result.add(candidate);
        }
        return result;
    }

    private static CobolRecordLayout layout(Entry group, List<Entry> descendants, Map<String, Integer> nameCounts) {
        if (OCCURS.matcher(group.text()).find() || REDEFINES.matcher(group.text()).find()) {
            return CobolRecordLayout.opaque(group.name(), "group has OCCURS or REDEFINES");
        }
        if (USAGE_TOKEN.matcher(group.text()).find()) {
            return CobolRecordLayout.opaque(group.name(), "group has non-DISPLAY usage");
        }
        if (descendants.isEmpty()) {
            return CobolRecordLayout.opaque(group.name(), "group has no elementary items");
        }
        List<CobolRecordLayout.Field> fields = new ArrayList<>();
        for (Entry entry : descendants) {
            String text = entry.text();
            if (OCCURS.matcher(text).find() || REDEFINES.matcher(text).find()) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " has OCCURS or REDEFINES");
            }
            if (USAGE_TOKEN.matcher(text).find()) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " is not USAGE DISPLAY");
            }
            Matcher picture = PICTURE.matcher(text);
            if (!picture.find()) {
                continue; // nested group header: its elementary items follow
            }
            String pic = picture.group(1).toUpperCase(Locale.ROOT);
            if (EDITED_OR_SCALED.matcher(pic.replaceAll("\\(\\d+\\)", "")).find()
                    || SEPARATE_SIGN.matcher(text).find()) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " has an edited, scaled or "
                        + "separate-sign picture");
            }
            PicType type;
            try {
                type = PicClause.parse(pic);
            } catch (RuntimeException ex) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " has an unreadable picture");
            }
            if (type.digits() <= 0) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " has an empty picture");
            }
            boolean filler = entry.name().equals("FILLER");
            if (!filler && nameCounts.getOrDefault(entry.name(), 0) > 1) {
                return CobolRecordLayout.opaque(group.name(), entry.name() + " is declared more than once");
            }
            fields.add(new CobolRecordLayout.Field(filler ? null : entry.name(), type, type.digits(),
                    filler ? fillerValue(text, type.digits()) : null));
        }
        return fields.isEmpty()
                ? CobolRecordLayout.opaque(group.name(), "group has no elementary items")
                : CobolRecordLayout.composable(group.name(), fields);
    }

    /** FILLER content: its alphanumeric VALUE, ZEROS or (by default) spaces, fitted to the length. */
    private static String fillerValue(String text, int length) {
        Matcher value = VALUE_LITERAL.matcher(text);
        String content = "";
        if (value.find()) {
            String literal = value.group(1);
            char quote = literal.charAt(0);
            if (quote == '\'' || quote == '"') {
                content = literal.substring(1, literal.length() - 1)
                        .replace(String.valueOf(quote) + quote, String.valueOf(quote));
            } else if (literal.toUpperCase(Locale.ROOT).startsWith("ZERO")) {
                content = "0".repeat(length);
            }
        }
        if (content.length() >= length) {
            return content.substring(0, length);
        }
        return content + " ".repeat(length - content.length());
    }
}
