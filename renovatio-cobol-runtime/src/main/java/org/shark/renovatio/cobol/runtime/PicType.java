package org.shark.renovatio.cobol.runtime;

/**
 * Rich descriptor of a COBOL PICTURE clause.
 *
 * <p>Unlike a plain target-language type name, this carries the information a
 * faithful transliteration needs: number of digits, decimal scale, sign
 * presence and physical storage ({@link Usage}).</p>
 *
 * @param category logical category of the item
 * @param digits   total number of digits (numeric) or character length (text)
 * @param scale    number of fractional digits (0 for integers and text)
 * @param signed   whether the picture carries a leading {@code S}
 * @param usage    physical storage / representation
 */
public record PicType(Category category, int digits, int scale, boolean signed, Usage usage) {

    public enum Category { NUMERIC, ALPHANUMERIC, ALPHABETIC }

    public enum Usage { DISPLAY, COMP, COMP_3, COMP_5 }

    /** Digits left of the implied decimal point. */
    public int integerDigits() {
        return digits - scale;
    }
}
