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
        List<Entry> open = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry group = entries.get(i);
            if (group.condition()) {
                continue;
            }
            open.removeIf(ancestor -> ancestor.level() >= group.level() || group.level() == 1 || group.level() == 77);
            boolean insideTable = open.stream().anyMatch(ancestor -> OCCURS.matcher(ancestor.text()).find());
            open.add(group);
            if (PICTURE.matcher(group.text()).find() || group.name().equals("FILLER")) {
                continue;
            }
            layouts.putIfAbsent(group.name(), insideTable
                    ? CobolRecordLayout.opaque(group.name(), "group is an element of an OCCURS table")
                    : layout(group, descendants(entries, i), nameCounts));
        }
        return layouts;
    }

    private static final Pattern ELEMENTARY_VALUE = Pattern.compile(
            "\\bVALUES?\\s+(?:IS\\s+|ARE\\s+)?('(?:[^']|'')*'|\"(?:[^\"]|\"\")*\"|[+-]?[0-9]*\\.?[0-9]+"
                    + "|SPACES?|ZEROS?|ZEROES|LOW-VALUES?|HIGH-VALUES?)",
            Pattern.CASE_INSENSITIVE);

    /**
     * VALUE literal of every uniquely named elementary item (raw COBOL text: quoted literal,
     * numeric literal or figurative constant). Items declared under an OCCURS or REDEFINES, and
     * names declared more than once, are left out: their initial state is not a single value.
     */
    static Map<String, String> initialValues(String dataSection) {
        List<Entry> entries = entries(dataSection);
        Map<String, Integer> nameCounts = new HashMap<>();
        for (Entry entry : entries) {
            if (!entry.condition()) {
                nameCounts.merge(entry.name(), 1, Integer::sum);
            }
        }
        Map<String, String> values = new LinkedHashMap<>();
        List<Entry> open = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.condition()) {
                continue;
            }
            open.removeIf(ancestor -> ancestor.level() >= entry.level() || entry.level() == 1 || entry.level() == 77);
            boolean repeated = OCCURS.matcher(entry.text()).find() || REDEFINES.matcher(entry.text()).find()
                    || open.stream().anyMatch(ancestor -> OCCURS.matcher(ancestor.text()).find()
                    || REDEFINES.matcher(ancestor.text()).find());
            open.add(entry);
            if (repeated || entry.name().equals("FILLER") || nameCounts.getOrDefault(entry.name(), 0) > 1
                    || !PICTURE.matcher(entry.text()).find()) {
                continue;
            }
            // VALUE must come from the entry itself, not from a PIC literal: drop the PIC token first.
            Matcher value = ELEMENTARY_VALUE.matcher(PICTURE.matcher(entry.text()).replaceFirst(" "));
            if (value.find()) {
                values.put(entry.name(), value.group(1));
            }
        }
        return values;
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
        String reason = appendFields(descendants, 0, nameCounts, fields);
        if (reason != null) {
            return CobolRecordLayout.opaque(group.name(), reason);
        }
        return fields.isEmpty()
                ? CobolRecordLayout.opaque(group.name(), "group has no elementary items")
                : CobolRecordLayout.composable(group.name(), fields);
    }

    private static final Pattern FIXED_OCCURS = Pattern.compile(
            "(?i)\\bOCCURS\\s+(\\d+)(?:\\s+TIMES)?\\b(?!\\s+TO\\b)");

    /**
     * Appends the elementary fields of {@code entries} in storage order. A fixed one-level OCCURS
     * repeats its subtree once per occurrence (fields carry the 1-based index); variable or nested
     * OCCURS, and REDEFINES, return the reason the layout is opaque.
     */
    private static String appendFields(List<Entry> entries, int occurrence, Map<String, Integer> nameCounts,
                                       List<CobolRecordLayout.Field> fields) {
        for (int k = 0; k < entries.size(); k++) {
            Entry entry = entries.get(k);
            String text = entry.text();
            if (REDEFINES.matcher(text).find()) {
                return entry.name() + " has REDEFINES";
            }
            if (OCCURS.matcher(text).find()) {
                Matcher fixed = FIXED_OCCURS.matcher(text);
                if (occurrence != 0 || !fixed.find() || text.toUpperCase(Locale.ROOT).contains("DEPENDING")) {
                    return entry.name() + " has a variable or nested OCCURS";
                }
                int times = Integer.parseInt(fixed.group(1));
                List<Entry> subtree = new ArrayList<>();
                if (PICTURE.matcher(text).find()) {
                    subtree.add(new Entry(entry.level(), entry.name(), OCCURS_CLAUSE.matcher(text).replaceAll(" ")));
                } else {
                    for (int j = k + 1; j < entries.size() && entries.get(j).level() > entry.level(); j++) {
                        subtree.add(entries.get(j));
                    }
                }
                for (int index = 1; index <= times; index++) {
                    String reason = appendFields(subtree, index, nameCounts, fields);
                    if (reason != null) {
                        return reason;
                    }
                }
                k += PICTURE.matcher(text).find() ? 0 : subtree.size();
                continue;
            }
            String reason = appendElementary(entry, occurrence, nameCounts, fields);
            if (reason != null) {
                return reason;
            }
        }
        return null;
    }

    private static final Pattern OCCURS_CLAUSE = Pattern.compile(
            "(?i)\\bOCCURS\\s+\\d+(?:\\s+TIMES)?(?:\\s+INDEXED\\s+BY\\s+[A-Z0-9-]+)?");

    private static String appendElementary(Entry entry, int occurrence, Map<String, Integer> nameCounts,
                                           List<CobolRecordLayout.Field> fields) {
        String text = entry.text();
        Matcher usage = USAGE_TOKEN.matcher(text);
        String usageToken = usage.find() ? usage.group(1).toUpperCase(Locale.ROOT) : null;
        if (usageToken != null && !ENCODABLE_USAGE.matcher(usageToken).matches()) {
            return entry.name() + " has USAGE " + usageToken + " (no exact character encoding)";
        }
        Matcher picture = PICTURE.matcher(text);
        if (!picture.find()) {
            return usageToken != null ? entry.name() + " has group-level USAGE" : null;
        }
        String pic = picture.group(1).toUpperCase(Locale.ROOT);
        if (EDITED_OR_SCALED.matcher(pic.replaceAll("\\(\\d+\\)", "")).find()
                || SEPARATE_SIGN.matcher(text).find()) {
            return entry.name() + " has an edited, scaled or separate-sign picture";
        }
        PicType type;
        try {
            type = PicClause.parse(usageToken == null ? pic : pic + " " + usageToken);
        } catch (RuntimeException ex) {
            return entry.name() + " has an unreadable picture";
        }
        if (type.digits() <= 0) {
            return entry.name() + " has an empty picture";
        }
        if (type.usage() != PicType.Usage.DISPLAY && type.category() != PicType.Category.NUMERIC) {
            return entry.name() + " has binary usage on a non-numeric picture";
        }
        if (type.usage() != PicType.Usage.DISPLAY && type.digits() > 18) {
            return entry.name() + " has more than 18 binary digits";
        }
        boolean filler = entry.name().equals("FILLER");
        if (!filler && nameCounts.getOrDefault(entry.name(), 0) > 1) {
            return entry.name() + " is declared more than once";
        }
        int length = storageLength(type);
        fields.add(new CobolRecordLayout.Field(filler ? null : entry.name(), type, length,
                filler ? (type.usage() == PicType.Usage.DISPLAY ? fillerValue(text, length)
                        : "\0".repeat(length)) : null, occurrence));
        return null;
    }

    /**
     * Number of occurrences of every named elementary item inside exactly one fixed OCCURS
     * (its own or an ancestor's); 0 marks a table this generation does not support (variable
     * length, OCCURS DEPENDING ON or more than one dimension). Scalars are absent.
     */
    static Map<String, Integer> tableDimensions(String dataSection) {
        List<Entry> entries = entries(dataSection);
        Map<String, Integer> dimensions = new LinkedHashMap<>();
        List<Entry> open = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.condition()) {
                continue;
            }
            open.removeIf(ancestor -> ancestor.level() >= entry.level() || entry.level() == 1 || entry.level() == 77);
            open.add(entry);
            if (entry.name().equals("FILLER") || !PICTURE.matcher(entry.text()).find()) {
                continue;
            }
            List<Entry> tables = open.stream().filter(e -> OCCURS.matcher(e.text()).find()).toList();
            if (tables.isEmpty()) {
                continue;
            }
            Matcher fixed = FIXED_OCCURS.matcher(tables.get(0).text());
            boolean supported = tables.size() == 1 && fixed.find()
                    && !tables.get(0).text().toUpperCase(Locale.ROOT).contains("DEPENDING");
            dimensions.put(entry.name(), supported ? Integer.parseInt(fixed.group(1)) : 0);
        }
        return dimensions;
    }

    /** USAGE clauses whose bytes have an exact encoding: binary integers and packed decimal. */
    private static final Pattern ENCODABLE_USAGE = Pattern.compile(
            "COMP|COMP-4|COMP-5|COMPUTATIONAL|COMPUTATIONAL-4|COMPUTATIONAL-5|BINARY|COMP-3|COMPUTATIONAL-3"
                    + "|PACKED-DECIMAL");

    /**
     * Bytes an elementary item occupies (IBM sizes): DISPLAY one per digit or character; binary
     * 2, 4 or 8 by digit count; packed decimal two digits per byte plus the sign nibble.
     */
    static int storageLength(PicType type) {
        return switch (type.usage()) {
            case DISPLAY -> type.digits();
            case COMP, COMP_5 -> type.digits() <= 4 ? 2 : type.digits() <= 9 ? 4 : 8;
            case COMP_3 -> type.digits() / 2 + 1;
        };
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
