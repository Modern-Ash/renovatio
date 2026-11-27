package org.modernash.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.lang.model.element.Modifier;
import java.util.Map;

/**
 * Adds to a program DTO what a migrated program needs to end its run from any paragraph and,
 * for an online (CICS) program, its screen request and response.
 *
 * <p>An online program becomes a screen handler: the request carries what CICS gave the task
 * (the key pressed, the COMMAREA, the received map data) and the response collects what the
 * task decided (the map or text to show, the program to transfer to, the transaction and
 * COMMAREA to resume with). They are plain values; the controller and the web layer decide how
 * they travel.</p>
 */
public final class ScreenTypesGenerator {

    private ScreenTypesGenerator() {
    }

    /** {@code ProgramExit} and {@code endRun()}: ends the run unit from a nested paragraph. */
    public static void addProgramExit(TypeSpec.Builder dto) {
        dto.addType(TypeSpec.classBuilder("ProgramExit")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .superclass(RuntimeException.class)
                .addJavadoc("Ends the program run (GOBACK, STOP RUN, CICS XCTL or RETURN) from any paragraph.\n")
                .addMethod(MethodSpec.constructorBuilder()
                        .addStatement("super(null, null, false, false)")
                        .build())
                .build());
        dto.addMethod(MethodSpec.methodBuilder("endRun")
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Ends the program run; {@code process} catches it and returns this DTO.\n")
                .addStatement("throw new ProgramExit()")
                .build());

    }

    public static void addScreenTypes(TypeSpec.Builder dto) {
        ClassName request = ClassName.get("", "ScreenRequest");
        ClassName response = ClassName.get("", "ScreenResponse");
        TypeName maps = ParameterizedTypeName.get(Map.class, String.class, String.class);

        dto.addType(TypeSpec.classBuilder("ScreenRequest")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Input of one task: the attention key (ENTER, CLEAR, PF1..PF24, PA1..PA3), the COMMAREA\n"
                        + "the previous task returned (empty on first entry) and the received map data by map name.\n")
                .addField(String.class, "aid", Modifier.PRIVATE, Modifier.FINAL)
                .addField(String.class, "commarea", Modifier.PRIVATE, Modifier.FINAL)
                .addField(maps, "maps", Modifier.PRIVATE, Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(String.class, "aid")
                        .addParameter(String.class, "commarea")
                        .addParameter(maps, "maps")
                        .addStatement("this.aid = aid")
                        .addStatement("this.commarea = commarea == null ? \"\" : commarea")
                        .addStatement("this.maps = maps == null ? $T.of() : $T.copyOf(maps)", Map.class, Map.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("firstEntry")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(request)
                        .addJavadoc("A transaction started from scratch: no COMMAREA (EIBCALEN = 0).\n")
                        .addStatement("return new ScreenRequest(\"ENTER\", \"\", $T.of())", Map.class)
                        .build())
                .addMethod(accessor("aid", String.class))
                .addMethod(accessor("commarea", String.class))
                .addMethod(MethodSpec.methodBuilder("commareaLength")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(Integer.class)
                        .addStatement("return commarea.length()")
                        .build())
                // shortcut: APPLID / SYSID come from the environment; carry them on the request when a web layer (#214) knows the region.
                .addMethod(MethodSpec.methodBuilder("applid")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addJavadoc("CICS ASSIGN APPLID: the region's application id (environment CICS_APPLID).\n")
                        .addStatement("return $T.getenv().getOrDefault(\"CICS_APPLID\", \"\")", System.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("sysid")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addJavadoc("CICS ASSIGN SYSID: the system id (environment CICS_SYSID).\n")
                        .addStatement("return $T.getenv().getOrDefault(\"CICS_SYSID\", \"\")", System.class)
                        .build())
                .addMethod(MethodSpec.methodBuilder("map")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(String.class)
                        .addParameter(String.class, "name")
                        .addStatement("return maps.getOrDefault(name, \"\")")
                        .build())
                .build());

        TypeSpec.Builder result = TypeSpec.classBuilder("ScreenResponse")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .addJavadoc("Outcome of one task: the screen sent (map and data, or plain text) and where the\n"
                        + "conversation goes next (transfer to a program, or resume a transaction with a COMMAREA).\n");
        for (String field : new String[] {"map", "mapset", "screen", "text", "nextProgram", "transid", "commarea"}) {
            result.addField(FieldSpec.builder(String.class, field, Modifier.PRIVATE).build());
            result.addMethod(accessor(field, String.class));
        }
        result.addField(FieldSpec.builder(boolean.class, "ended", Modifier.PRIVATE).build())
                .addMethod(MethodSpec.methodBuilder("ended")
                        .addModifiers(Modifier.PUBLIC).returns(boolean.class).addStatement("return ended").build())
                .addMethod(MethodSpec.methodBuilder("sendMap")
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc("SEND MAP: show {@code screen} (the symbolic output map) with map/mapset.\n")
                        .addParameter(String.class, "map").addParameter(String.class, "mapset")
                        .addParameter(String.class, "screen")
                        .addStatement("this.map = map").addStatement("this.mapset = mapset")
                        .addStatement("this.screen = screen").addStatement("this.text = null")
                        .build())
                .addMethod(MethodSpec.methodBuilder("sendText")
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc("SEND TEXT: show plain text instead of a map.\n")
                        .addParameter(String.class, "text")
                        .addStatement("this.text = text").addStatement("this.map = null").addStatement("this.screen = null")
                        .build())
                .addMethod(MethodSpec.methodBuilder("transfer")
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc("XCTL: hand the conversation to {@code program} with {@code commarea}.\n")
                        .addParameter(String.class, "program").addParameter(String.class, "commarea")
                        .addStatement("this.nextProgram = program").addStatement("this.commarea = commarea")
                        .addStatement("this.ended = true")
                        .build())
                .addMethod(MethodSpec.methodBuilder("returnTo")
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc("RETURN TRANSID: resume with {@code transid} and {@code commarea} on the next input.\n")
                        .addParameter(String.class, "transid").addParameter(String.class, "commarea")
                        .addStatement("this.transid = transid").addStatement("this.commarea = commarea")
                        .addStatement("this.ended = true")
                        .build())
                .addMethod(MethodSpec.methodBuilder("end")
                        .addModifiers(Modifier.PUBLIC)
                        .addJavadoc("RETURN without TRANSID: the conversation is over.\n")
                        .addStatement("this.ended = true")
                        .build());
        dto.addType(result.build());

        dto.addField(FieldSpec.builder(request, "screenRequest", Modifier.PRIVATE)
                .initializer("ScreenRequest.firstEntry()").build());
        dto.addField(FieldSpec.builder(response, "screenResponse", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new ScreenResponse()").build());
        dto.addMethod(MethodSpec.methodBuilder("getScreenRequest").addModifiers(Modifier.PUBLIC)
                .returns(request).addStatement("return screenRequest").build());
        dto.addMethod(MethodSpec.methodBuilder("setScreenRequest")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(request, "screenRequest")
                .addStatement("this.screenRequest = screenRequest")
                .build());
        dto.addMethod(MethodSpec.methodBuilder("getScreenResponse").addModifiers(Modifier.PUBLIC)
                .returns(response).addStatement("return screenResponse").build());
        dto.addMethod(MethodSpec.methodBuilder("abend")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "code")
                .addJavadoc("CICS ABEND: the task fails with the abend code; nothing is returned.\n")
                .addStatement("throw new $T(\"CICS ABEND \" + code)", IllegalStateException.class)
                .build());
    }

    private static MethodSpec accessor(String field, Object type) {
        MethodSpec.Builder method = MethodSpec.methodBuilder(field).addModifiers(Modifier.PUBLIC)
                .addStatement("return $L", field);
        return type instanceof Class<?> c ? method.returns(c).build() : method.returns((TypeName) type).build();
    }
}
