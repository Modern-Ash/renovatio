package org.modernash.renovatio.cobol.ir.context;

import org.modernash.renovatio.cobol.runtime.PicClause;
import org.modernash.renovatio.cobol.runtime.PicType;

public final class CobolTypeMapper {

    private CobolTypeMapper() {
    }

    /**
     * Parses a COBOL PICTURE clause into a rich {@link PicType} descriptor
     * (digits, scale, sign, usage). Returns {@code null} when the clause is
     * blank or cannot be interpreted as a picture.
     */
    public static PicType picType(String pic) {
        if (pic == null || pic.isBlank()) {
            return null;
        }
        if (isNumericEdited(pic)) {
            // A numeric-edited item holds its edited characters: stored and read as text.
            return new PicType(PicType.Category.ALPHANUMERIC, expandPicture(pic).replace("V", "").length(), 0, false,
                    PicType.Usage.DISPLAY);
        }
        try {
            PicType type = PicClause.parse(pic);
            if (type.category() == PicType.Category.NUMERIC && type.digits() == 0) {
                return null;
            }
            return type;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * True for a numeric-edited picture ({@code +ZZZ,ZZ9.99}, {@code Z(5)9-}, {@code $$$,$$9.99CR}):
     * digit positions with suppression, insertion or sign symbols, and no alphanumeric symbols.
     */
    public static boolean isNumericEdited(String pic) {
        if (pic == null) return false;
        String upper = pic.trim().toUpperCase(java.util.Locale.ROOT);
        if (upper.contains(" ") || upper.matches(".*[XA].*") || upper.matches(".*\\(\\d{4,}\\).*")) return false;
        String expanded = expandPicture(upper);
        return !expanded.contains("S") && expanded.matches("[9ZV*,.+\\-$B0/]*(CR|DB)?") && expanded.matches(".*[Z*,.+\\-$B/].*|.*(CR|DB)")
                && expanded.matches(".*[9Z*$+\\-].*");
    }

    /** The picture with repetition counts written out: {@code Z(3)9} becomes {@code ZZZ9}. */
    public static String expandPicture(String pic) {
        java.util.regex.Matcher repeat = java.util.regex.Pattern.compile("(.)\\((\\d+)\\)")
                .matcher(pic.trim().toUpperCase(java.util.Locale.ROOT));
        StringBuilder expanded = new StringBuilder();
        while (repeat.find()) {
            repeat.appendReplacement(expanded, repeat.group(1).repeat(Integer.parseInt(repeat.group(2))));
        }
        repeat.appendTail(expanded);
        return expanded.toString();
    }

    /**
     * Legacy mapping to a target Java type name. Kept for backward
     * compatibility; now derived from {@link #picType(String)}.
     */
    public static String picToJavaType(String pic) {
        return javaType(picType(pic));
    }

    /** Target Java type name for a parsed picture ({@code String} when unknown). */
    public static String javaType(PicType type) {
        if (type == null) {
            return String.class.getSimpleName();
        }
        switch (type.category()) {
            case ALPHANUMERIC:
            case ALPHABETIC:
                return String.class.getSimpleName();
            default:
                break;
        }
        if (type.scale() > 0) {
            return "BigDecimal";
        }
        int digits = type.digits();
        if (digits <= 9) {
            return Integer.class.getSimpleName();
        }
        if (digits <= 18) {
            return Long.class.getSimpleName();
        }
        return "BigDecimal";
    }
}
