package org.modernash.renovatio.cobol.ir.model;

import org.modernash.renovatio.cobol.runtime.PicType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Byte layout of a COBOL group item as a fixed-width DISPLAY record: its elementary
 * descendants in storage order. A group is {@link #composable()} when every descendant can be
 * encoded to and decoded from characters exactly (USAGE DISPLAY, no OCCURS, REDEFINES, edited
 * or scaled pictures, no ambiguous names); only then may generated code read or write the
 * group as one string, the way COBOL moves a group or a file record.
 *
 * @param groupName       COBOL name of the group item
 * @param fields          elementary descendants in storage order (empty when not composable)
 * @param nonComposableReason why the group cannot be treated as a record, when it cannot
 */
public record CobolRecordLayout(String groupName, List<Field> fields, Optional<String> nonComposableReason) {

    public CobolRecordLayout {
        groupName = Objects.requireNonNull(groupName, "groupName");
        fields = List.copyOf(fields == null ? List.of() : fields);
        nonComposableReason = nonComposableReason == null ? Optional.empty() : nonComposableReason;
    }

    public static CobolRecordLayout composable(String groupName, List<Field> fields) {
        return new CobolRecordLayout(groupName, fields, Optional.empty());
    }

    public static CobolRecordLayout opaque(String groupName, String reason) {
        return new CobolRecordLayout(groupName, List.of(), Optional.of(reason));
    }

    public boolean composable() {
        return nonComposableReason.isEmpty() && !fields.isEmpty();
    }

    /**
     * Bytes an elementary item occupies (IBM sizes): DISPLAY one per digit or character; binary
     * 2, 4 or 8 by digit count; packed decimal two digits per byte plus the sign nibble.
     */
    public static int storageLength(PicType type) {
        return switch (type.usage()) {
            case DISPLAY -> type.digits();
            case COMP, COMP_5 -> type.digits() <= 4 ? 2 : type.digits() <= 9 ? 4 : 8;
            case COMP_3 -> type.digits() / 2 + 1;
        };
    }

    /** Record length in characters. */
    public int length() {
        return fields.stream().mapToInt(Field::length).sum();
    }

    /**
     * One elementary storage position of the record.
     *
     * @param name        COBOL name, or {@code null} for FILLER
     * @param picType     parsed picture
     * @param length      characters occupied in DISPLAY form
     * @param fillerValue content of a FILLER (its VALUE literal, padded), {@code null} for named fields
     * @param occurrence  1-based OCCURS index of a table element, 0 for a scalar field
     */
    public record Field(String name, PicType picType, int length, String fillerValue, int occurrence) {
        public Field {
            Objects.requireNonNull(picType, "picType");
            if (length <= 0) {
                throw new IllegalArgumentException("field length must be positive");
            }
        }

        public Field(String name, PicType picType, int length, String fillerValue) {
            this(name, picType, length, fillerValue, 0);
        }

        /** 1-based OCCURS index of a table element, 0 for a scalar field. */
        public int occurrence() {
            return occurrence;
        }

        public boolean filler() {
            return name == null;
        }
    }
}
