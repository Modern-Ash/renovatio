package org.shark.renovatio.cobol.ir.context;

import java.math.BigDecimal;
import java.util.Locale;

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
        if (normalized.matches("9\\(\\d+\\)")) {
            int digits = Integer.parseInt(normalized.replaceAll("[^0-9]", ""));
            if (digits <= 9) {
                return Integer.class.getSimpleName();
            }
            if (digits <= 18) {
                return Long.class.getSimpleName();
            }
            return BigDecimal.class.getSimpleName();
        }
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
