package org.shark.renovatio.cobol.ir.annotated;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Minimal RFC 8785 writer for the closed annotated-IR identity projections. */
final class CanonicalJson {

    private CanonicalJson() {
    }

    static String write(Object value) {
        StringBuilder result = new StringBuilder();
        append(result, value);
        return result.toString();
    }

    private static void append(StringBuilder target, Object value) {
        if (value == null) {
            target.append("null");
        } else if (value instanceof String text) {
            appendString(target, text);
        } else if (value instanceof Boolean bool) {
            target.append(bool);
        } else if (value instanceof Number number) {
            appendNumber(target, number);
        } else if (value instanceof Enum<?> enumeration) {
            appendString(target, enumeration.name());
        } else if (value instanceof Map<?, ?> map) {
            appendMap(target, map);
        } else if (value instanceof Iterable<?> iterable) {
            appendIterable(target, iterable);
        } else if (value.getClass().isArray()) {
            List<Object> elements = new ArrayList<>();
            for (int index = 0; index < Array.getLength(value); index++) elements.add(Array.get(value, index));
            appendIterable(target, elements);
        } else {
            throw new IllegalArgumentException("Unsupported canonical JSON value: " + value.getClass().getName());
        }
    }

    private static void appendMap(StringBuilder target, Map<?, ?> map) {
        List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(entry -> requireStringKey(entry.getKey())));
        target.append('{');
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) target.append(',');
            Map.Entry<?, ?> entry = entries.get(index);
            appendString(target, requireStringKey(entry.getKey()));
            target.append(':');
            append(target, entry.getValue());
        }
        target.append('}');
    }

    private static String requireStringKey(Object key) {
        if (key instanceof String text) return text;
        throw new IllegalArgumentException("Canonical JSON object keys must be strings");
    }

    private static void appendIterable(StringBuilder target, Iterable<?> values) {
        target.append('[');
        boolean separator = false;
        for (Object value : values) {
            if (separator) target.append(',');
            append(target, value);
            separator = true;
        }
        target.append(']');
    }

    private static void appendNumber(StringBuilder target, Number number) {
        if (number instanceof Byte || number instanceof Short || number instanceof Integer
                || number instanceof Long || number instanceof BigInteger) {
            target.append(number);
            return;
        }
        if (number instanceof BigDecimal decimal) {
            appendDecimal(target, decimal);
            return;
        }
        if (!(number instanceof Float || number instanceof Double)) {
            throw new IllegalArgumentException("Unsupported canonical JSON number: " + number.getClass().getName());
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Canonical JSON numbers must be finite");
        if (value == 0d) {
            target.append('0');
            return;
        }
        BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
        double magnitude = Math.abs(value);
        String text = magnitude >= 1e-6 && magnitude < 1e21
                ? decimal.toPlainString()
                : decimal.toString();
        text = text.replace("E+", "e+").replace("E-", "e-").replace("E", "e");
        target.append(text);
    }

    private static void appendDecimal(StringBuilder target, BigDecimal number) {
        if (number.signum() == 0) {
            target.append('0');
            return;
        }
        BigDecimal decimal = number.stripTrailingZeros();
        int exponent = decimal.precision() - decimal.scale() - 1;
        if (exponent >= -6 && exponent < 21) {
            target.append(decimal.toPlainString());
            return;
        }
        String digits = decimal.unscaledValue().abs().toString();
        if (decimal.signum() < 0) target.append('-');
        target.append(digits.charAt(0));
        if (digits.length() > 1) target.append('.').append(digits, 1, digits.length());
        target.append('e');
        if (exponent >= 0) target.append('+');
        target.append(exponent);
    }

    private static void appendString(StringBuilder target, String value) {
        target.append('"');
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> target.append("\\\"");
                case '\\' -> target.append("\\\\");
                case '\b' -> target.append("\\b");
                case '\f' -> target.append("\\f");
                case '\n' -> target.append("\\n");
                case '\r' -> target.append("\\r");
                case '\t' -> target.append("\\t");
                default -> {
                    if (current <= 0x1f) target.append(String.format("\\u%04x", (int) current));
                    else target.append(current);
                }
            }
        }
        target.append('"');
    }
}
