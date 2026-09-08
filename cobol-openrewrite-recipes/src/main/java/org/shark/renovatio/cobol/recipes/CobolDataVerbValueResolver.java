package org.shark.renovatio.cobol.recipes;

import org.shark.renovatio.cobol.ir.model.CobolDataItem;
import org.shark.renovatio.cobol.ir.model.Level88Condition;
import org.shark.renovatio.cobol.ir.model.Level88Value;
import org.shark.renovatio.cobol.runtime.PicType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Deterministic Java values for the supported COBOL data-verb subset. */
public final class CobolDataVerbValueResolver {

    private CobolDataVerbValueResolver() {
    }

    public static Optional<String> initialValue(CobolDataItem item) {
        PicType pic = item.picType();
        if (pic != null && pic.category() != PicType.Category.NUMERIC) {
            return Optional.of("\" \".repeat(" + Math.max(1, pic.digits()) + ")");
        }
        return switch (item.javaType()) {
            case "Integer" -> Optional.of("0");
            case "Long" -> Optional.of("0L");
            case "Double" -> Optional.of("0.0d");
            case "BigDecimal" -> Optional.of("java.math.BigDecimal.ZERO");
            case "String" -> Optional.of("\"\"");
            default -> Optional.empty();
        };
    }

    public static Optional<String> conditionValue(CobolDataItem parent,
                                                  Level88Condition condition,
                                                  boolean value) {
        if (value) {
            return renderCobolValue(condition.values().get(0).value(), parent);
        }
        List<String> candidates = new ArrayList<>();
        if (isNumeric(parent)) {
            candidates.addAll(List.of("0", "1", "2", "9"));
            if (parent.picType().signed()) {
                candidates.add("-1");
            }
        } else {
            candidates.addAll(List.of(" ", "0", "1", "A", "N", "X", "Z", "_"));
        }
        return candidates.stream()
                .filter(candidate -> !matches(condition, candidate, parent))
                .map(candidate -> renderCobolValue(candidate, parent))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private static boolean matches(Level88Condition condition, String candidate, CobolDataItem parent) {
        for (Level88Value value : condition.values()) {
            String lower = normalizeFigurative(value.value(), parent);
            if (!value.isRange() && compareCobolValues(candidate, lower, parent) == 0) {
                return true;
            }
            if (value.isRange()) {
                String upper = normalizeFigurative(value.through(), parent);
                if (compareCobolValues(candidate, lower, parent) >= 0
                        && compareCobolValues(candidate, upper, parent) <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int compareCobolValues(String left, String right, CobolDataItem parent) {
        if (isNumeric(parent)) {
            try {
                return new BigDecimal(left).compareTo(new BigDecimal(right));
            } catch (NumberFormatException ignored) {
                return left.compareTo(right);
            }
        }
        return left.compareTo(right);
    }

    private static Optional<String> renderCobolValue(String rawValue, CobolDataItem parent) {
        String value = normalizeFigurative(rawValue, parent);
        if (!isNumeric(parent)) {
            return Optional.of("\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
        }
        if (!value.matches("[+-]?\\d+(?:\\.\\d+)?")) {
            return Optional.empty();
        }
        return switch (parent.javaType()) {
            case "Integer" -> Optional.of(value);
            case "Long" -> Optional.of(value + "L");
            case "Double" -> Optional.of(value + "d");
            case "BigDecimal" -> Optional.of("new java.math.BigDecimal(\"" + value + "\")");
            default -> Optional.empty();
        };
    }

    private static String normalizeFigurative(String value, CobolDataItem parent) {
        String normalized = value == null ? "" : value;
        String keyword = normalized.strip();
        if (keyword.equalsIgnoreCase("ZERO") || keyword.equalsIgnoreCase("ZEROS")
                || keyword.equalsIgnoreCase("ZEROES")) {
            return isNumeric(parent) ? "0" : "0".repeat(picLength(parent));
        }
        if (keyword.equalsIgnoreCase("SPACE") || keyword.equalsIgnoreCase("SPACES")) {
            return " ".repeat(picLength(parent));
        }
        return normalized;
    }

    private static boolean isNumeric(CobolDataItem item) {
        return item.picType() != null && item.picType().category() == PicType.Category.NUMERIC;
    }

    private static int picLength(CobolDataItem item) {
        return item.picType() == null ? 1 : Math.max(1, item.picType().digits());
    }
}
