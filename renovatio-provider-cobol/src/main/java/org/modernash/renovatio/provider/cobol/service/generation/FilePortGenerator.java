package org.modernash.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.modernash.renovatio.architecture.java.JavaArchitectureLayoutPlanner;
import org.modernash.renovatio.cobol.ir.model.CobolCicsDataset;
import org.modernash.renovatio.cobol.ir.model.CobolFileDescriptor;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.model.CobolRecordLayout;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Generates, for each file a COBOL program performs I/O on, an outbound port the translated
 * service depends on and an in-memory adapter implementing it.
 *
 * <p>The port keeps the observable contract of the COBOL file: records are fixed-width DISPLAY
 * strings (see {@link RecordAccessorGenerator}) and every operation returns the two-character
 * FILE STATUS code COBOL would set ({@code 00} success, {@code 10} end of file, {@code 22}
 * duplicate key, {@code 23} record not found, {@code 4x} logic errors). Persistence
 * technology is the adapter's concern: the in-memory adapter serves golden-master runs and
 * tests; a JPA, JDBC or flat-file adapter implements the same port.</p>
 */
public final class FilePortGenerator {

    public static final String PACKAGE = "org.modernash.renovatio.generated.cobol";

    private FilePortGenerator() {
    }

    /** One file port: names derived from the program class base and the SELECT name. */
    public record FilePort(String selectName, String portType, String adapterType, String fieldName) {
        public static FilePort of(String classBase, String selectName) {
            String stem = classBase + JavaArchitectureLayoutPlanner.filePortBase(selectName);
            String base = JavaArchitectureLayoutPlanner.filePortBase(selectName);
            return new FilePort(selectName, stem + "Port", stem + "InMemoryAdapter",
                    Character.toLowerCase(base.charAt(0)) + base.substring(1));
        }
    }

    public static List<FilePort> ports(String classBase, List<String> selectNames) {
        List<FilePort> ports = new ArrayList<>();
        for (String name : selectNames) {
            ports.add(FilePort.of(classBase, name));
        }
        return ports;
    }

    public static String port(FilePort port, CobolIntermediateModel model) {
        Optional<CobolFileDescriptor> descriptor = model == null ? Optional.empty() : model.findFile(port.selectName());
        ClassName self = ClassName.get(PACKAGE, port.portType());
        ClassName readResult = self.nestedClass("ReadResult");
        TypeSpec result = TypeSpec.classBuilder("ReadResult")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Outcome of a READ: the FILE STATUS code and the record ({@code null} unless "
                        + "the status is successful).\n")
                .addField(String.class, "status", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "record", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "status")
                        .addParameter(String.class, "record")
                        .addStatement("this.status = status")
                        .addStatement("this.record = record")
                        .build())
                .addMethod(MethodSpec.methodBuilder("status")
                        .addModifiers(Modifier.PUBLIC).returns(String.class)
                        .addStatement("return status").build())
                .addMethod(MethodSpec.methodBuilder("record")
                        .addModifiers(Modifier.PUBLIC).returns(String.class)
                        .addStatement("return record").build())
                .addMethod(MethodSpec.methodBuilder("successful")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(boolean.class)
                        .addStatement("return status.startsWith(\"0\")")
                        .build())
                .build();
        TypeSpec.Builder type = TypeSpec.interfaceBuilder(port.portType())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Outbound port for COBOL file $L$L.\n", port.selectName(),
                        descriptor.map(file -> " (" + file.organization() + ", " + file.accessMode() + " access"
                                + (file.assignTarget() == null ? "" : ", assigned to " + file.assignTarget()) + ")")
                                .orElse(""))
                .addJavadoc("Records are fixed-width DISPLAY strings; every operation returns the FILE STATUS "
                        + "code.\n")
                .addType(result);
        type.addMethod(abstractMethod("open", String.class, "Opens the file; mode is INPUT, OUTPUT, I-O or EXTEND.")
                .addParameter(String.class, "mode").build());
        type.addMethod(abstractMethod("close", String.class, "Closes the file.").build());
        type.addMethod(abstractMethod("read", readResult, "Reads the next record (sequential or READ NEXT).")
                .build());
        type.addMethod(abstractMethod("read", readResult, "Reads the record with the given key.")
                .addParameter(String.class, "key").build());
        type.addMethod(abstractMethod("write", String.class, "Writes a new record.")
                .addParameter(String.class, "record").build());
        type.addMethod(abstractMethod("rewrite", String.class, "Replaces the record last read (or with the "
                + "record's key).").addParameter(String.class, "record").build());
        type.addMethod(abstractMethod("delete", String.class, "Deletes the record with the given key.")
                .addParameter(String.class, "key").build());
        type.addMethod(abstractMethod("start", String.class, "Positions sequential reading at the first "
                + "record whose key is not less than the given key.").addParameter(String.class, "key").build());
        if (cicsDataset(port, model) != null) {
            type.addMethod(abstractMethod("readPrevious", readResult, "Reads the previous record of a browse "
                    + "(CICS READPREV); right after start, the record start positioned at.").build());
            type.addMethod(MethodSpec.methodBuilder("resp")
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .returns(int.class)
                    .addParameter(String.class, "status")
                    .addJavadoc("The CICS RESP condition for a FILE STATUS code (DFHRESP values).\n")
                    .addStatement("return switch (status) { case \"00\", \"02\" -> 0; case \"10\" -> 20; "
                            + "case \"23\" -> 13; case \"22\" -> 14; default -> 1; }")
                    .build());
        }
        return JavaFile.builder(PACKAGE, type.build()).build().toString();
    }

    /** The CICS dataset a port stands for, when the program has no FD of that name. */
    private static CobolCicsDataset cicsDataset(FilePort port, CobolIntermediateModel model) {
        if (model == null || model.findFile(port.selectName()).isPresent()) return null;
        return model.cicsDatasets().get(port.selectName().toUpperCase(java.util.Locale.ROOT));
    }

    private static MethodSpec.Builder abstractMethod(String name, Object returns, String doc) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addJavadoc(doc + "\n");
        return returns instanceof Class<?> type ? method.returns(type) : method.returns((TypeName) returns);
    }

    /**
     * In-memory adapter: sequential files keep insertion order; indexed and relative files are
     * kept sorted by the record key, located inside the record by the FD record layout.
     */
    public static String inMemoryAdapter(FilePort port, CobolIntermediateModel model) {
        Optional<CobolFileDescriptor> descriptor = model == null ? Optional.empty() : model.findFile(port.selectName());
        CobolCicsDataset dataset = cicsDataset(port, model);
        int[] key = dataset != null && dataset.keyed() ? new int[] {dataset.keyOffset(), dataset.keyLength()}
                : descriptor.flatMap(model::recordKeySpan).orElse(null);
        boolean keyed = (dataset != null || descriptor.map(CobolFileDescriptor::keyed).orElse(false)) && key != null;
        ClassName portType = ClassName.get(PACKAGE, port.portType());
        ClassName readResult = portType.nestedClass("ReadResult");
        TypeName records = ParameterizedTypeName.get(TreeMap.class, String.class, String.class);
        TypeName list = ParameterizedTypeName.get(List.class, String.class);

        FieldSpec.Builder mode = FieldSpec.builder(String.class, "mode", Modifier.PRIVATE);
        if (dataset != null) {
            mode.initializer("$S", "I-O"); // CICS owns the files of an online program and keeps them open.
        }
        TypeSpec.Builder type = TypeSpec.classBuilder(port.adapterType())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(portType)
                .addJavadoc("In-memory $L for golden-master runs and tests.\n", port.portType())
                .addField(FieldSpec.builder(records, "records", Modifier.PRIVATE, Modifier.FINAL)
                        .initializer("new $T<>()", TreeMap.class).build())
                .addField(mode.build())
                .addField(FieldSpec.builder(String.class, "position", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(String.class, "lastRead", Modifier.PRIVATE).build())
                .addField(FieldSpec.builder(long.class, "sequence", Modifier.PRIVATE).build());

        boolean keyAtConstruction = dataset != null && keyed;
        if (keyAtConstruction) {
            // shortcut: the key position is inferred from RIDFLD in the program; an alternate index path keys elsewhere,
            // so it can be given here (read it from the IDCAMS DEFINE KEYS when the JCL is part of the intake).
            type.addField(int.class, "keyOffset", Modifier.PRIVATE, Modifier.FINAL);
            type.addField(int.class, "keyLength", Modifier.PRIVATE, Modifier.FINAL);
            type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                    .addStatement("this($T.of())", List.class).build());
            type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Starts with the given records already in the file.\n")
                    .addParameter(list, "initialRecords")
                    .addStatement("this(initialRecords, $L, $L)", key[0], key[1]).build());
            type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Starts with the given records, keyed by the characters at "
                            + "{@code keyOffset} (an alternate index path keys on another field).\n")
                    .addParameter(list, "initialRecords")
                    .addParameter(int.class, "keyOffset")
                    .addParameter(int.class, "keyLength")
                    .addStatement("this.keyOffset = keyOffset")
                    .addStatement("this.keyLength = keyLength")
                    .beginControlFlow("for (String record : initialRecords)")
                    .addStatement("records.put(keyOf(record), record)")
                    .endControlFlow()
                    .build());
        } else {
            type.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());
            type.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addJavadoc("Starts with the given records already in the file.\n")
                    .addParameter(list, "initialRecords")
                    .beginControlFlow("for (String record : initialRecords)")
                    .addStatement("records.put(keyOf(record), record)")
                    .endControlFlow()
                    .build());
        }
        type.addMethod(MethodSpec.methodBuilder("records")
                .addModifiers(Modifier.PUBLIC)
                .returns(list)
                .addJavadoc("Current file content in key (or write) order.\n")
                .addStatement("return $T.copyOf(records.values())", List.class)
                .build());
        MethodSpec.Builder keyOf = MethodSpec.methodBuilder("keyOf")
                .addModifiers(Modifier.PRIVATE)
                .returns(String.class)
                .addParameter(String.class, "record");
        if (keyAtConstruction) {
            keyOf.addStatement("String padded = record.length() >= keyOffset + keyLength ? record "
                    + ": record + $S.repeat(keyOffset + keyLength - record.length())", " ");
            keyOf.addStatement("return padded.substring(keyOffset, keyOffset + keyLength)");
        } else if (keyed) {
            keyOf.addStatement("String padded = record.length() >= $L ? record : record + $S.repeat($L - record.length())",
                    key[0] + key[1], " ", key[0] + key[1]);
            keyOf.addStatement("return padded.substring($L, $L)", key[0], key[0] + key[1]);
        } else {
            keyOf.addStatement("return String.format(\"%019d\", sequence++)");
        }
        type.addMethod(keyOf.build());
        type.addMethod(MethodSpec.methodBuilder("open")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .addParameter(String.class, "mode")
                .beginControlFlow("if (this.mode != null)")
                .addStatement("return \"41\"")
                .endControlFlow()
                .beginControlFlow("if (\"OUTPUT\".equals(mode))")
                .addStatement("records.clear()")
                .endControlFlow()
                .addStatement("this.mode = mode")
                .addStatement("this.position = null")
                .addStatement("this.lastRead = null")
                .addStatement("return \"00\"")
                .build());
        type.addMethod(MethodSpec.methodBuilder("close")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .beginControlFlow("if (mode == null)")
                .addStatement("return \"42\"")
                .endControlFlow()
                .addStatement("mode = null")
                .addStatement("return \"00\"")
                .build());
        type.addMethod(MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(readResult)
                .beginControlFlow("if (!\"INPUT\".equals(mode) && !\"I-O\".equals(mode))")
                .addStatement("return new $T(\"47\", null)", readResult)
                .endControlFlow()
                .addCode(dataset != null ? "startedAt = null;\n" : "")
                .addStatement("String next = position == null ? (records.isEmpty() ? null : records.firstKey()) "
                        + ": records.higherKey(position)")
                .beginControlFlow("if (next == null)")
                .addStatement("return new $T(\"10\", null)", readResult)
                .endControlFlow()
                .addStatement("position = next")
                .addStatement("lastRead = next")
                .addStatement("return new $T(\"00\", records.get(next))", readResult)
                .build());
        type.addMethod(MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(readResult)
                .addParameter(String.class, "key")
                .beginControlFlow("if (!\"INPUT\".equals(mode) && !\"I-O\".equals(mode))")
                .addStatement("return new $T(\"47\", null)", readResult)
                .endControlFlow()
                .addStatement("String record = records.get(key)")
                .beginControlFlow("if (record == null)")
                .addStatement("return new $T(\"23\", null)", readResult)
                .endControlFlow()
                .addStatement("position = key")
                .addStatement("lastRead = key")
                .addStatement("return new $T(\"00\", record)", readResult)
                .build());
        type.addMethod(MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .addParameter(String.class, "record")
                .beginControlFlow("if (!\"OUTPUT\".equals(mode) && !\"EXTEND\".equals(mode) && !\"I-O\".equals(mode))")
                .addStatement("return \"48\"")
                .endControlFlow()
                .addStatement("String key = keyOf(record)")
                .beginControlFlow("if (records.containsKey(key))")
                .addStatement("return \"22\"")
                .endControlFlow()
                .addStatement("records.put(key, record)")
                .addStatement("return \"00\"")
                .build());
        type.addMethod(MethodSpec.methodBuilder("rewrite")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .addParameter(String.class, "record")
                .beginControlFlow("if (!\"I-O\".equals(mode))")
                .addStatement("return \"49\"")
                .endControlFlow()
                .addStatement("String key = $L", keyed ? "keyOf(record)" : "lastRead")
                .beginControlFlow("if (key == null)")
                .addStatement("return \"43\"")
                .endControlFlow()
                .beginControlFlow("if (!records.containsKey(key))")
                .addStatement("return \"23\"")
                .endControlFlow()
                .addStatement("records.put(key, record)")
                .addStatement("return \"00\"")
                .build());
        type.addMethod(MethodSpec.methodBuilder("delete")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .addParameter(String.class, "key")
                .beginControlFlow("if (!\"I-O\".equals(mode))")
                .addStatement("return \"49\"")
                .endControlFlow()
                .addStatement("String target = key == null ? lastRead : key")
                .beginControlFlow("if (target == null || records.remove(target) == null)")
                .addStatement("return \"23\"")
                .endControlFlow()
                .addStatement("return \"00\"")
                .build());
        type.addMethod(MethodSpec.methodBuilder("start")
                .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(String.class)
                .addParameter(String.class, "key")
                .beginControlFlow("if (!\"INPUT\".equals(mode) && !\"I-O\".equals(mode))")
                .addStatement("return \"47\"")
                .endControlFlow()
                // shortcut: a failed start keeps the previous position (COBOL 46, CICS INVREQ not modeled); track browse state when a program reads on after it.
                .addStatement("String first = records.ceilingKey(key)")
                .beginControlFlow("if (first == null)")
                .addStatement("return \"23\"")
                .endControlFlow()
                .addStatement("position = records.lowerKey(first)")
                .addCode(dataset != null ? "startedAt = first;\n" : "")
                .addStatement("return \"00\"")
                .build());
        if (dataset != null) {
            type.addField(FieldSpec.builder(String.class, "startedAt", Modifier.PRIVATE).build());
            type.addMethod(MethodSpec.methodBuilder("readPrevious")
                    .addAnnotation(Override.class).addModifiers(Modifier.PUBLIC).returns(readResult)
                    .addStatement("String previous = startedAt != null ? startedAt : position == null "
                            + "? (records.isEmpty() ? null : records.lastKey()) : records.lowerKey(position)")
                    .addStatement("startedAt = null")
                    .beginControlFlow("if (previous == null)")
                    .addStatement("return new $T(\"10\", null)", readResult)
                    .endControlFlow()
                    .addStatement("position = previous")
                    .addStatement("lastRead = previous")
                    .addStatement("return new $T(\"00\", records.get(previous))", readResult)
                    .build());
        }
        return JavaFile.builder(PACKAGE, type.build()).build().toString();
    }
}
