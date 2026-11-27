package org.modernash.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import com.squareup.javapoet.ClassName;
import org.modernash.renovatio.cobol.ir.model.CobolDataItem;
import org.modernash.renovatio.cobol.ir.model.CobolRecordLayout;
import org.modernash.renovatio.cobol.ir.model.CobolRedefinesView;
import org.modernash.renovatio.cobol.runtime.PicType;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Adds group-item accessors to a generated DTO: a getter that composes the group's fixed-width
 * DISPLAY record from its elementary fields and a setter that splits a record back into them.
 * This is how COBOL itself treats a group (MOVE of a group, a file record, a two-byte FILE
 * STATUS), so translated statements can read or write a group as one string.
 *
 * <p>Numbers use zoned-decimal DISPLAY form: zero-padded digits with the implied decimal point
 * removed and, for signed pictures, the sign embedded in the last digit (IBM convention
 * {@code {A-I} / }J-R}; ASCII {@code p-y} is also accepted when decoding).</p>
 */
public final class RecordAccessorGenerator {

    private static final String POSITIVE_ZONES = "{ABCDEFGHI";
    private static final String NEGATIVE_ZONES = "}JKLMNOPQR";

    private RecordAccessorGenerator() {
    }

    /**
     * Adds accessors for every composable group whose fields all exist in the DTO.
     *
     * @param fieldTypes DTO field name to Java type ({@code String}, {@code Integer}, {@code Long},
     *                   {@code BigDecimal})
     * @param fieldName  COBOL name to DTO field name
     * @return names of the groups that received accessors
     */
    public static List<String> addTo(TypeSpec.Builder dto, CobolIntermediateModel model,
                                     Map<String, String> fieldTypes, Function<String, String> fieldName) {
        List<String> generated = new ArrayList<>();
        if (model == null) {
            return generated;
        }
        for (CobolRecordLayout layout : model.getRecordLayouts().values()) {
            if (!layout.composable() || !allFieldsDeclared(layout, fieldTypes, fieldName)) {
                continue;
            }
            String property = fieldName.apply(layout.groupName());
            if (fieldTypes.containsKey(property)) {
                continue;
            }
            dto.addMethod(getter(layout, property, fieldTypes, fieldName));
            dto.addMethod(setter(layout, property, fieldTypes, fieldName));
            generated.add(layout.groupName());
        }
        int views = addRedefinesViews(dto, model, fieldTypes, fieldName, generated);
        if (!generated.isEmpty() || views > 0) {
            addHelpers(dto);
            addBinaryHelpers(dto);
        }
        return generated;
    }

    /**
     * REDEFINES views: accessors with no field of their own that decode / re-encode their bytes
     * inside the base item's storage (its composed record, or its own encoding when elementary).
     *
     * @return number of views generated
     */
    private static int addRedefinesViews(TypeSpec.Builder dto, CobolIntermediateModel model,
                                         Map<String, String> fieldTypes, Function<String, String> fieldName,
                                         List<String> recordAccessors) {
        int count = 0;
        for (CobolRedefinesView view : model.getRedefinesViews().values()) {
            if (!view.supported()) {
                continue;
            }
            String base = fieldName.apply(view.base());
            String storage;
            String write;
            if (view.baseIsGroup()) {
                if (!recordAccessors.contains(view.base())) continue;
                storage = "get" + capitalize(base) + "()";
                write = "set" + capitalize(base) + "(storage)";
            } else {
                Optional<CobolDataItem> item = item(model, view.base());
                String type = fieldTypes.get(base);
                if (item.isEmpty() || type == null || item.get().picType() == null) continue;
                PicType pic = item.get().picType();
                int length = CobolRecordLayout.storageLength(pic);
                storage = encode(base, type, pic, length);
                write = "this." + base + " = " + decode("storage", type, pic);
            }
            String property = fieldName.apply(view.name());
            String type = view.picType() == null ? "String"
                    : item(model, view.name()).map(CobolDataItem::javaType).orElse(null);
            if (type == null) continue;
            String slice = "storage.substring(" + view.offset() + ", " + (view.offset() + view.length()) + ")";
            ClassName javaType = ClassName.get(type.equals("BigDecimal") ? "java.math" : "java.lang", type);
            dto.addMethod(MethodSpec.methodBuilder("get" + capitalize(property))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(javaType)
                    .addJavadoc("$L REDEFINES $L: bytes $L-$L of its storage.\n", view.name(), view.base(),
                            view.offset(), view.offset() + view.length() - 1)
                    .addStatement("String storage = $L", storage)
                    .addStatement("return $L", view.picType() == null ? slice : decode(slice, type, view.picType()))
                    .build());
            String encoded = view.picType() == null ? "fitText(value, " + view.length() + ")"
                    : encode("value", type, view.picType(), view.length());
            dto.addMethod(MethodSpec.methodBuilder("set" + capitalize(property))
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(javaType, "value")
                    .addStatement("String storage = $L", storage)
                    .addStatement("storage = storage.substring(0, $L) + $L + storage.substring($L)",
                            view.offset(), encoded, view.offset() + view.length())
                    .addStatement(write)
                    .build());
            count++;
        }
        return count;
    }

    private static Optional<CobolDataItem> item(CobolIntermediateModel model, String name) {
        return model.getDataItems().stream().filter(item -> item.name().equalsIgnoreCase(name)).findFirst();
    }

    /** Encoder call for a value of DTO type {@code type} stored with picture {@code pic}. */
    private static String encode(String expression, String type, PicType pic, int length) {
        if ("String".equals(type)) {
            return "fitText(" + expression + ", " + length + ")";
        }
        String args = expression + ", " + length + ", " + pic.scale() + ", " + pic.signed() + ")";
        return switch (pic.usage()) {
            case COMP, COMP_5 -> "fitBinary(" + args;
            case COMP_3 -> "fitPacked(" + args;
            default -> "fitNumber(" + args;
        };
    }

    /** Decoder call turning encoded {@code bytes} back into DTO type {@code type}. */
    private static String decode(String bytes, String type, PicType pic) {
        if ("String".equals(type)) {
            return bytes;
        }
        String decoder = switch (pic.usage()) {
            case COMP, COMP_5 -> "parseBinary(";
            case COMP_3 -> "parsePacked(";
            default -> "parseNumber(";
        };
        String decoded = decoder + bytes + ", " + pic.scale() + ", " + pic.signed() + ")";
        return switch (type) {
            case "Integer" -> decoded + ".intValue()";
            case "Long" -> decoded + ".longValue()";
            default -> decoded;
        };
    }

    private static boolean allFieldsDeclared(CobolRecordLayout layout, Map<String, String> fieldTypes,
                                             Function<String, String> fieldName) {
        return layout.fields().stream()
                .filter(field -> !field.filler())
                .allMatch(field -> fieldTypes.containsKey(fieldName.apply(field.name())));
    }

    private static MethodSpec getter(CobolRecordLayout layout, String property, Map<String, String> fieldTypes,
                                     Function<String, String> fieldName) {
        List<String> parts = new ArrayList<>();
        for (CobolRecordLayout.Field field : layout.fields()) {
            if (field.filler()) {
                parts.add(javaString(field.fillerValue()));
                continue;
            }
            String name = fieldName.apply(field.name());
            String ref = element(name, field);
            String numberArgs = ref + ", " + field.length() + ", " + field.picType().scale() + ", "
                    + field.picType().signed() + ")";
            parts.add("String".equals(fieldTypes.get(name))
                    ? "fitText(" + ref + ", " + field.length() + ")"
                    : switch (field.picType().usage()) {
                        case COMP, COMP_5 -> "fitBinary(" + numberArgs;
                        case COMP_3 -> "fitPacked(" + numberArgs;
                        default -> "fitNumber(" + numberArgs;
                    });
        }
        return MethodSpec.methodBuilder("get" + capitalize(property))
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addJavadoc("COBOL group $L as its $L-character DISPLAY record.\n", layout.groupName(),
                        layout.length())
                .addStatement("return $L", String.join(" + ", parts))
                .build();
    }

    private static MethodSpec setter(CobolRecordLayout layout, String property, Map<String, String> fieldTypes,
                                     Function<String, String> fieldName) {
        MethodSpec.Builder setter = MethodSpec.methodBuilder("set" + capitalize(property))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "record")
                .addJavadoc("Splits a $L-character DISPLAY record into the fields of COBOL group $L.\n",
                        layout.length(), layout.groupName())
                .addStatement("String r = fitText(record, $L)", layout.length());
        int offset = 0;
        for (CobolRecordLayout.Field field : layout.fields()) {
            int end = offset + field.length();
            if (!field.filler()) {
                String name = fieldName.apply(field.name());
                String slice = "r.substring(" + offset + ", " + end + ")";
                String decoder = switch (field.picType().usage()) {
                    case COMP, COMP_5 -> "parseBinary(";
                    case COMP_3 -> "parsePacked(";
                    default -> "parseNumber(";
                };
                String decoded = decoder + slice + ", " + field.picType().scale() + ", "
                        + field.picType().signed() + ")";
                String value = switch (fieldTypes.get(name)) {
                    case "String" -> slice;
                    case "Integer" -> decoded + ".intValue()";
                    case "Long" -> decoded + ".longValue()";
                    default -> decoded;
                };
                setter.addStatement("this.$L = $L", element(name, field), value);
            }
            offset = end;
        }
        return setter.build();
    }

    private static void addHelpers(TypeSpec.Builder dto) {
        dto.addMethod(MethodSpec.methodBuilder("fitText")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(String.class, "value")
                .addParameter(int.class, "length")
                .addStatement("String text = value == null ? \"\" : value")
                .addStatement("return text.length() >= length ? text.substring(0, length) "
                        + ": text + \" \".repeat(length - text.length())")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("fitNumber")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(Number.class, "value")
                .addParameter(int.class, "length")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO "
                        + ": new java.math.BigDecimal(value.toString())")
                .addStatement("String digits = number.setScale(scale, java.math.RoundingMode.DOWN)"
                        + ".unscaledValue().abs().toString()")
                .addStatement("digits = digits.length() > length ? digits.substring(digits.length() - length) "
                        + ": \"0\".repeat(length - digits.length()) + digits")
                .beginControlFlow("if (!signed)")
                .addStatement("return digits")
                .endControlFlow()
                .addStatement("int last = digits.charAt(length - 1) - '0'")
                .addStatement("char zone = number.signum() < 0 ? $S.charAt(last) : $S.charAt(last)",
                        NEGATIVE_ZONES, POSITIVE_ZONES)
                .addStatement("return digits.substring(0, length - 1) + zone")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("parseNumber")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(java.math.BigDecimal.class)
                .addParameter(String.class, "text")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("boolean negative = false")
                .addStatement("StringBuilder digits = new StringBuilder()")
                .beginControlFlow("for (char c : text.toCharArray())")
                .beginControlFlow("if (c >= '0' && c <= '9')")
                .addStatement("digits.append(c)")
                .nextControlFlow("else if (signed && $S.indexOf(c) >= 0)", POSITIVE_ZONES)
                .addStatement("digits.append((char) ('0' + $S.indexOf(c)))", POSITIVE_ZONES)
                .nextControlFlow("else if (signed && $S.indexOf(c) >= 0)", NEGATIVE_ZONES)
                .addStatement("digits.append((char) ('0' + $S.indexOf(c)))", NEGATIVE_ZONES)
                .addStatement("negative = true")
                .nextControlFlow("else if (signed && c >= 'p' && c <= 'y')")
                .addStatement("digits.append((char) ('0' + (c - 'p')))")
                .addStatement("negative = true")
                .nextControlFlow("else")
                .addComment("Spaces or other non-digits in a numeric field read as zero.")
                .addStatement("digits.append('0')")
                .endControlFlow()
                .endControlFlow()
                .addStatement("java.math.BigDecimal number = new java.math.BigDecimal("
                        + "new java.math.BigInteger(digits.toString()), scale)")
                .addStatement("return negative ? number.negate() : number")
                .build());
    }

    /**
     * Binary (COMP/BINARY/COMP-5) and packed-decimal (COMP-3) encoders. Each byte travels as one
     * char (ISO-8859-1), so binary fields survive inside a record string.
     */
    private static void addBinaryHelpers(TypeSpec.Builder dto) {
        dto.addMethod(MethodSpec.methodBuilder("fitBinary")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(Number.class, "value")
                .addParameter(int.class, "length")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO "
                        + ": new java.math.BigDecimal(value.toString())")
                .addStatement("long unscaled = number.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue()"
                        + ".longValue()")
                .beginControlFlow("if (!signed)")
                .addStatement("unscaled = Math.abs(unscaled)")
                .endControlFlow()
                .addStatement("char[] bytes = new char[length]")
                .beginControlFlow("for (int i = length - 1; i >= 0; i--)")
                .addStatement("bytes[i] = (char) (unscaled & 0xFF)")
                .addStatement("unscaled >>= 8")
                .endControlFlow()
                .addStatement("return new String(bytes)")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("parseBinary")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(java.math.BigDecimal.class)
                .addParameter(String.class, "bytes")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("long unscaled = signed && bytes.length() > 0 && (bytes.charAt(0) & 0x80) != 0 ? -1L : 0L")
                .beginControlFlow("for (int i = 0; i < bytes.length(); i++)")
                .addStatement("unscaled = (unscaled << 8) | (bytes.charAt(i) & 0xFF)")
                .endControlFlow()
                .addStatement("return java.math.BigDecimal.valueOf(unscaled, scale)")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("fitPacked")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(String.class)
                .addParameter(Number.class, "value")
                .addParameter(int.class, "length")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("java.math.BigDecimal number = value == null ? java.math.BigDecimal.ZERO "
                        + ": new java.math.BigDecimal(value.toString())")
                .addStatement("String digits = number.setScale(scale, java.math.RoundingMode.DOWN).unscaledValue()"
                        + ".abs().toString()")
                .addStatement("int width = length * 2 - 1")
                .addStatement("digits = digits.length() > width ? digits.substring(digits.length() - width) "
                        + ": \"0\".repeat(width - digits.length()) + digits")
                .addStatement("int sign = !signed ? 0xF : number.signum() < 0 ? 0xD : 0xC")
                .addStatement("char[] bytes = new char[length]")
                .beginControlFlow("for (int i = 0; i < length; i++)")
                .addStatement("int high = digits.charAt(2 * i) - '0'")
                .addStatement("int low = i == length - 1 ? sign : digits.charAt(2 * i + 1) - '0'")
                .addStatement("bytes[i] = (char) ((high << 4) | low)")
                .endControlFlow()
                .addStatement("return new String(bytes)")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("parsePacked")
                .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                .returns(java.math.BigDecimal.class)
                .addParameter(String.class, "bytes")
                .addParameter(int.class, "scale")
                .addParameter(boolean.class, "signed")
                .addStatement("StringBuilder digits = new StringBuilder()")
                .addStatement("int sign = 0xF")
                .beginControlFlow("for (int i = 0; i < bytes.length(); i++)")
                .addStatement("int b = bytes.charAt(i) & 0xFF")
                .addStatement("digits.append(Math.min(b >> 4, 9))")
                .beginControlFlow("if (i == bytes.length() - 1)")
                .addStatement("sign = b & 0x0F")
                .nextControlFlow("else")
                .addStatement("digits.append(Math.min(b & 0x0F, 9))")
                .endControlFlow()
                .endControlFlow()
                .addStatement("java.math.BigDecimal number = new java.math.BigDecimal("
                        + "new java.math.BigInteger(digits.length() == 0 ? \"0\" : digits.toString()), scale)")
                .addStatement("return signed && (sign == 0xD || sign == 0xB) ? number.negate() : number")
                .build());
    }

    /** The DTO storage of a record field: the field itself, or its array element for an OCCURS entry. */
    private static String element(String name, CobolRecordLayout.Field field) {
        return field.occurrence() > 0 ? name + "[" + (field.occurrence() - 1) + "]" : name;
    }

    private static String javaString(String value) {
        return com.squareup.javapoet.CodeBlock.of("$S", value).toString();
    }

    private static String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
