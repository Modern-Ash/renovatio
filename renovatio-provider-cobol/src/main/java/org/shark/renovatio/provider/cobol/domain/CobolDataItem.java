package org.shark.renovatio.provider.cobol.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a COBOL data item (field/variable)
 */
@Getter
@Setter
public class CobolDataItem {
    private String name;
    private int level;
    private String picture;
    private String usage;
    private String value;
    private boolean isGroup;
    private List<CobolDataItem> children;
    private Map<String, Object> attributes;

    public CobolDataItem() {
    }

    public CobolDataItem(String name, int level, String picture) {
        this.name = name;
        this.level = level;
        this.picture = picture;
    }


    /**
     * Converts COBOL picture clause to Java type
     */
    public String getJavaType() {
        if (picture == null) return "Object";
        String pic = picture.trim().toUpperCase();

        // Alphanumeric
        if (pic.startsWith("X") || pic.startsWith("A")) {
            return "String";
        }

        // Numeric with implied decimal
        if (pic.contains("V") && pic.startsWith("9")) {
            return "BigDecimal";
        }

        // Pure numeric
        if (pic.startsWith("9")) {
            int totalDigits = estimateNumericDigits(pic);
            if (totalDigits <= 9) {
                return "Integer";
            } else {
                return "Long";
            }
        }

        return "Object";
    }

    private static final Pattern PIC_NUM_PAREN = Pattern.compile("9\\((\\d+)\\)");

    private int estimateNumericDigits(String pic) throws NumberFormatException {
        // Handles patterns like 9(5), 9(12), fallback to count of '9' chars if present
        Matcher m = PIC_NUM_PAREN.matcher(pic);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        // Fallback: count literal 9s
        int count = 0;
        for (char c : pic.toCharArray()) {
            if (c == '9') count++;
        }
        return count > 0 ? count : 1;
    }
}

/**
 * Represents a COBOL paragraph
 */
@Getter
@Setter
class CobolParagraph {
    private String name;
    private List<CobolStatement> statements;
}

/**
 * Represents a COBOL section
 */
@Getter
@Setter
class CobolSection {
    private String name;
    private List<CobolParagraph> paragraphs;
}

/**
 * Represents a COBOL statement
 */
@Getter
@Setter
class CobolStatement {
    private StatementType type;
    private String sourceCode;
    private Map<String, Object> attributes;


    public enum StatementType {
        MOVE, COMPUTE, IF, PERFORM, CALL, READ, WRITE, OPEN, CLOSE,
        DISPLAY, ACCEPT, EXIT, STOP, OTHER
    }
}