package org.shark.renovatio.cobol.ir.context;

import org.shark.renovatio.cobol.runtime.PicClause;
import org.shark.renovatio.cobol.runtime.PicType;

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
     * Legacy mapping to a target Java type name. Kept for backward
     * compatibility; now derived from {@link #picType(String)}.
     */
    public static String picToJavaType(String pic) {
        PicType type = picType(pic);
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
