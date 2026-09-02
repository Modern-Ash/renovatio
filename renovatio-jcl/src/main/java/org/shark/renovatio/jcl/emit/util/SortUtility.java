package org.shark.renovatio.jcl.emit.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Executable common SORT subset used by emitted tasklets and characterization fixtures. */
public final class SortUtility {
    private static final Pattern FIELDS = Pattern.compile("(?:SORT|MERGE)\\s+FIELDS=\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILTER = Pattern.compile("(INCLUDE|OMIT)\\s+COND=\\((\\d+),(\\d+),([A-Z]+),(EQ|NE|GT|GE|LT|LE),([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    public List<String> execute(List<String> records, String controlStatements) {
        SortSpec spec = parse(controlStatements);
        return records.stream().filter(spec::include).sorted(spec.comparator()).toList();
    }

    public SortSpec parse(String controlStatements) {
        String controls = controlStatements == null ? "" : controlStatements.replace('\n', ' ').trim();
        Matcher fields = FIELDS.matcher(controls);
        if (!fields.find()) throw new UnsupportedOperationException("Manual action: unsupported SORT without FIELDS");
        List<String> tokens = split(fields.group(1));
        List<Field> parsedFields = new ArrayList<>();
        for (int index = 0; index + 3 < tokens.size(); index += 4) {
            parsedFields.add(new Field(Integer.parseInt(tokens.get(index)), Integer.parseInt(tokens.get(index + 1)),
                    Format.valueOf(tokens.get(index + 2).toUpperCase(Locale.ROOT)),
                    tokens.get(index + 3).equalsIgnoreCase("D")));
        }
        if (parsedFields.isEmpty()) throw new UnsupportedOperationException("Manual action: unsupported SORT FIELDS subgrammar");
        Matcher filter = FILTER.matcher(controls);
        Optional<Filter> parsedFilter = filter.find() ? Optional.of(new Filter(filter.group(1).equalsIgnoreCase("OMIT"),
                Integer.parseInt(filter.group(2)), Integer.parseInt(filter.group(3)),
                Format.valueOf(filter.group(4).toUpperCase(Locale.ROOT)), Operator.valueOf(filter.group(5).toUpperCase(Locale.ROOT)),
                unquote(filter.group(6).trim()))) : Optional.empty();
        return new SortSpec(parsedFields, parsedFilter);
    }

    private static List<String> split(String value) {
        return java.util.Arrays.stream(value.split(",")).map(String::trim).filter(token -> !token.isEmpty()).toList();
    }
    private static String unquote(String value) {
        if (value.length() >= 3 && (value.startsWith("C'") || value.startsWith("c'")) && value.endsWith("'"))
            return value.substring(2, value.length() - 1);
        return value.length() >= 2 && value.startsWith("'") && value.endsWith("'")
                ? value.substring(1, value.length() - 1) : value;
    }
    private static String slice(String record, int position, int length) {
        int start = position - 1;
        if (start >= record.length()) return "";
        return record.substring(start, Math.min(record.length(), start + length));
    }
    private static Comparable<?> value(String record, int position, int length, Format format) {
        String field = slice(record, position, length);
        if (format == Format.CH) return field;
        String numeric = field.trim().replaceAll("[^0-9+.-]", "");
        return numeric.isEmpty() ? BigDecimal.ZERO : new BigDecimal(numeric);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compare(Comparable left, Comparable right) { return left.compareTo(right); }

    public record SortSpec(List<Field> fields, Optional<Filter> filter) {
        public SortSpec { fields = List.copyOf(fields); filter = filter == null ? Optional.empty() : filter; }
        public boolean include(String record) { return filter.map(value -> value.include(record)).orElse(true); }
        public Comparator<String> comparator() {
            return (left, right) -> {
                for (Field field : fields) {
                    int result = compare(value(left, field.position(), field.length(), field.format()),
                            value(right, field.position(), field.length(), field.format()));
                    if (result != 0) return field.descending() ? -result : result;
                }
                return 0;
            };
        }
    }
    public record Field(int position, int length, Format format, boolean descending) {
        public Field { if (position < 1 || length < 1) throw new IllegalArgumentException("invalid SORT field"); }
    }
    public record Filter(boolean omit, int position, int length, Format format, Operator operator, String expected) {
        public boolean include(String record) {
            Comparable<?> actual = value(record, position, length, format);
            Comparable<?> target = format == Format.CH ? expected : new BigDecimal(expected);
            int comparison = compare(actual, target);
            boolean matches = switch (operator) {
                case EQ -> comparison == 0; case NE -> comparison != 0; case GT -> comparison > 0;
                case GE -> comparison >= 0; case LT -> comparison < 0; case LE -> comparison <= 0;
            };
            return omit ? !matches : matches;
        }
    }
    public enum Format { CH, BI, ZD, PD }
    public enum Operator { EQ, NE, GT, GE, LT, LE }
}
