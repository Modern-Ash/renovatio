package org.modernash.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;

/**
 * Adds to a program DTO the COBOL numeric editing a MOVE into a numeric-edited item performs:
 * zero suppression ({@code Z}, {@code *}), insertion ({@code , . B 0 /}), fixed and floating
 * signs and currency ({@code + - $}), and {@code CR} / {@code DB}. The picture is passed with
 * its repetitions written out ({@code ZZZ9.99}); {@code V} marks an implied decimal point.
 */
public final class NumericEditGenerator {

    private NumericEditGenerator() {
    }

    public static void addEditMethod(TypeSpec.Builder dto) {
        dto.addMethod(MethodSpec.methodBuilder("editNumeric")
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addParameter(BigDecimal.class, "value")
                .addParameter(String.class, "picture")
                .addJavadoc("COBOL numeric editing of {@code value} into {@code picture} (repetitions expanded).\n")
                .addCode(BODY.replace("$", "$$"))
                .build());
    }

    private static final String BODY = """
            boolean negative = value.signum() < 0;
            java.math.BigDecimal magnitude = value.abs();
            char floating = 0;
            int floatStart = -1;
            int floatEnd = -1;
            for (int i = 0; i + 1 < picture.length() && floating == 0; i++) {
              char c = picture.charAt(i);
              if ((c == '+' || c == '-' || c == '$') && picture.indexOf(c, i + 1) > i) {
                int j = i + 1;
                int last = i;
                while (j < picture.length() && (picture.charAt(j) == c || ",B0/".indexOf(picture.charAt(j)) >= 0)) {
                  if (picture.charAt(j) == c) last = j;
                  j++;
                }
                if (last > i) {
                  floating = c;
                  floatStart = i;
                  floatEnd = last;
                }
              }
            }
            int point = picture.indexOf('.') >= 0 ? picture.indexOf('.') : picture.indexOf('V');
            int digitCount = 0;
            int scale = 0;
            for (int i = 0; i < picture.length(); i++) {
              char c = picture.charAt(i);
              boolean digit = c == '9' || c == 'Z' || c == '*' || (c == floating && i > floatStart && i <= floatEnd);
              if (digit) {
                digitCount++;
                if (point >= 0 && i > point) scale++;
              }
            }
            String digits = magnitude.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue().toString();
            digits = digits.length() >= digitCount ? digits.substring(digits.length() - digitCount)
                : "0".repeat(digitCount - digits.length()) + digits;
            char fill = picture.indexOf('*') >= 0 ? '*' : ' ';
            boolean allZero = digits.chars().allMatch(ch -> ch == '0');
            StringBuilder out = new StringBuilder();
            int[] position = new int[picture.length()];
            boolean significant = false;
            int d = 0;
            for (int i = 0; i < picture.length(); i++) {
              char c = picture.charAt(i);
              position[i] = out.length();
              if (c == 'V') continue;
              boolean digit = c == '9' || c == 'Z' || c == '*' || (c == floating && i > floatStart && i <= floatEnd);
              if (digit) {
                char ch = digits.charAt(d++);
                if (!significant && (ch != '0' || c == '9')) significant = true;
                out.append(significant ? ch : (c == '*' ? '*' : ' '));
              } else if (c == '.') {
                significant = significant || !allZero || picture.indexOf('9') >= 0;
                out.append('.');
              } else if (",B0/".indexOf(c) >= 0) {
                out.append(significant ? (c == 'B' ? ' ' : c) : fill);
              } else if (c == floating && i == floatStart) {
                out.append(' ');
              } else if (c == '+') {
                out.append(negative ? '-' : '+');
              } else if (c == '-') {
                out.append(negative ? '-' : ' ');
              } else if (c == '$') {
                out.append('$');
              } else if ((c == 'C' || c == 'D') && i + 1 < picture.length()) {
                out.append(negative ? picture.substring(i, i + 2) : "  ");
                i++;
              } else {
                out.append(c);
              }
            }
            if (floating != 0) {
              int symbol = position[floatEnd];
              for (int i = floatStart + 1; i <= floatEnd; i++) {
                char ch = out.charAt(position[i]);
                if (ch != ' ' && ch != fill) {
                  symbol = position[i] - 1;
                  break;
                }
              }
              if (symbol >= position[floatEnd] && out.charAt(position[floatEnd]) == ' ') symbol = position[floatEnd];
              char sign = floating == '$' ? '$' : floating == '+' ? (negative ? '-' : '+') : (negative ? '-' : ' ');
              out.setCharAt(Math.max(symbol, position[floatStart]), sign);
            }
            if (allZero && picture.indexOf('9') < 0) {
              // A zero value with every digit suppressed is all spaces (or asterisks, keeping the point).
              StringBuilder blank = new StringBuilder();
              for (int i = 0; i < out.length(); i++) blank.append(fill == '*' && out.charAt(i) == '.' ? '.' : fill);
              return blank.toString();
            }
            return out.toString();
            """;
}
