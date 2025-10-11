package org.shark.renovatio.cobol.ir.context;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CobolTypeMapper {

    private CobolTypeMapper() {
    }

    public static String picToJavaType(String pic) {
        if (pic == null || pic.isBlank()) {
            return String.class.getSimpleName();
        }
        String normalized = pic.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("PIC")) {
            normalized = normalized.substring(3).trim();
        }
        // Integer/Long/BigDecimal based on number of digits in 9(n)
        Matcher intMatcher = Pattern.compile("9\\((\\d+)\\)").matcher(normalized);
        if (intMatcher.matches()) {
            int digits = Integer.parseInt(intMatcher.group(1));
            if (digits <= 9) {
                return Integer.class.getSimpleName();
            }
            if (digits <= 18) {
                return Long.class.getSimpleName();
            }
            return BigDecimal.class.getSimpleName();
        }
        // Decimal types e.g., 9(n)V9+
        if (normalized.matches("9\\(\\d+\\)V9+")) {
            return BigDecimal.class.getSimpleName();
        }
        if (normalized.startsWith("X") || normalized.startsWith("A")) {
            return String.class.getSimpleName();
        }
        if (normalized.contains("COMP")) {
            return Integer.class.getSimpleName();
        }
        return String.class.getSimpleName();
    }
}
