package org.modernash.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.modernash.renovatio.architecture.java.JavaArchitectureLayoutPlanner;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates, for each program a COBOL program CALLs by name, an outbound port the translated
 * service depends on and a default adapter.
 *
 * <p>USING parameters travel as DISPLAY strings in an array the implementation may update in
 * place, which is how BY REFERENCE behaves. The default adapter of an ordinary program reports it
 * as not available with {@link UnsupportedOperationException}: the translated CALL runs its
 * ON EXCEPTION statements then, as COBOL does when the program cannot be loaded. Language
 * Environment's abend service {@code CEE3ABD} gets an adapter that ends the run.</p>
 */
public final class ProgramPortGenerator {

    private ProgramPortGenerator() {
    }

    public record ProgramPort(String programName, String portType, String adapterType, String fieldName) {
        public static ProgramPort of(String classBase, String programName) {
            String base = JavaArchitectureLayoutPlanner.filePortBase(programName) + "Program";
            String stem = classBase + base;
            return new ProgramPort(programName, stem + "Port", stem + "Adapter",
                    Character.toLowerCase(base.charAt(0)) + base.substring(1));
        }
    }

    public static List<ProgramPort> ports(String classBase, List<String> programNames) {
        List<ProgramPort> ports = new ArrayList<>();
        for (String name : programNames) {
            ports.add(ProgramPort.of(classBase, name));
        }
        return ports;
    }

    public static String port(ProgramPort port) {
        TypeSpec type = TypeSpec.interfaceBuilder(port.portType())
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Outbound port for {@code CALL '$L'}.\n", port.programName())
                .addMethod(MethodSpec.methodBuilder("call")
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                        .addParameter(ArrayTypeName.of(String.class), "using")
                        .addJavadoc("Runs the program. {@code using} holds the USING parameters as DISPLAY "
                                + "strings; the program may replace elements (BY REFERENCE).\n")
                        .addJavadoc("@throws UnsupportedOperationException when the program is not available "
                                + "(COBOL ON EXCEPTION)\n")
                        .build())
                .build();
        return JavaFile.builder(FilePortGenerator.PACKAGE, type).build().toString();
    }

    public static String adapter(ProgramPort port) {
        MethodSpec.Builder call = MethodSpec.methodBuilder("call")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ArrayTypeName.of(String.class), "using");
        String javadoc;
        if (port.programName().toUpperCase(Locale.ROOT).equals("CEE3ABD")) {
            javadoc = "Language Environment abend: ends the run unit with the given abend code.\n";
            call.addStatement("throw new $T(\"CEE3ABD abend code \" + (using.length > 0 ? using[0].trim() : \"\"))",
                    IllegalStateException.class);
        } else {
            javadoc = "Default wiring: the called program is not available until an implementation of "
                    + "{@link " + port.portType() + "} is provided.\n";
            call.addStatement("throw new $T($S)", UnsupportedOperationException.class,
                    "CALL '" + port.programName() + "' is not wired: provide a " + port.portType()
                            + " implementation");
        }
        TypeSpec type = TypeSpec.classBuilder(port.adapterType())
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ClassName.get(FilePortGenerator.PACKAGE, port.portType()))
                .addJavadoc(javadoc.replace("$", "$$"))
                .addMethod(call.build())
                .build();
        return JavaFile.builder(FilePortGenerator.PACKAGE, type).build().toString();
    }
}
