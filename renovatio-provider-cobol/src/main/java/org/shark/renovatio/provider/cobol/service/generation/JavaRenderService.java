package org.shark.renovatio.provider.cobol.service.generation;

import com.squareup.javapoet.*;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.provider.cobol.service.TemplateCodeGenerationService;
import org.shark.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.lang.model.element.Modifier;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for rendering Java code from generation plans.
 * This is the fourth stage of the generation pipeline.
 */
@Service
public class JavaRenderService {

    private static final Logger log = LoggerFactory.getLogger(JavaRenderService.class);

    private final TemplateCodeGenerationService templateService;
    private final CobolSemanticTranspiler semanticTranspiler;

    public JavaRenderService(TemplateCodeGenerationService templateService,
                             CobolSemanticTranspiler semanticTranspiler) {
        this.templateService = templateService;
        this.semanticTranspiler = semanticTranspiler;
    }

    /**
     * Render a DTO class from field definitions.
     *
     * @param classBase the base class name
     * @param fields the field definitions
     * @return the rendered DTO class source code
     */
    public String renderDto(String classBase, List<JavaProjectService.FieldDefinition> fields) {
        log.debug("Rendering DTO for class: {}", classBase);

        String className = classBase + "DTO";

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Data Transfer Object generated from COBOL program: $L\n", classBase);

        // Add default constructor
        classBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .build());

        // Add fields
        for (JavaProjectService.FieldDefinition field : fields) {
            addFieldToClass(classBuilder, field);
        }

        TypeSpec classSpec = classBuilder.build();
        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", classSpec)
                .build();

        return javaFile.toString();
    }

    /**
     * Render a service interface.
     *
     * @param classBase the base class name
     * @param entryPoints the ENTRY points
     * @return the rendered interface source code
     */
    public String renderServiceInterface(String classBase, List<Map<String, Object>> entryPoints) {
        log.debug("Rendering service interface for class: {}", classBase);

        String interfaceName = classBase + "Service";
        String dtoName = classBase + "DTO";

        ClassName dtoClass = ClassName.get("org.shark.renovatio.generated.cobol", dtoName);

        TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Service interface for COBOL program: $L\n", classBase);

        if (entryPoints != null && !entryPoints.isEmpty()) {
            // Generate a method for each ENTRY point
            for (Map<String, Object> entry : entryPoints) {
                String entryName = (String) entry.get("name");
                if (entryName != null && !entryName.isEmpty()) {
                    String methodName = toCamelCase(entryName);
                    MethodSpec entryMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .addParameter(dtoClass, "input")
                            .returns(dtoClass)
                            .addJavadoc("COBOL ENTRY point: $L\n", entryName)
                            .addJavadoc("@param input Input data structure\n")
                            .addJavadoc("@return Processed output data structure\n")
                            .build();
                    interfaceBuilder.addMethod(entryMethod);
                }
            }
            // Always include a default process method
            addProcessMethod(interfaceBuilder, dtoClass);
        } else {
            addProcessMethod(interfaceBuilder, dtoClass);
        }

        // Add validation method
        MethodSpec validateMethod = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoClass, "input")
                .returns(boolean.class)
                .addJavadoc("Validate input data structure\n")
                .addJavadoc("@param input Input data to validate\n")
                .addJavadoc("@return true if valid, false otherwise\n")
                .build();

        interfaceBuilder.addMethod(validateMethod);

        TypeSpec interfaceSpec = interfaceBuilder.build();
        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", interfaceSpec)
                .build();

        return javaFile.toString();
    }

    /**
     * Render a service implementation.
     *
     * @param classBase the base class name
     * @param entryPoints the ENTRY points
     * @param fields the field definitions
     * @param model the intermediate model
     * @return the rendered implementation source code
     */
    public String renderServiceImpl(String classBase, List<Map<String, Object>> entryPoints,
                                    List<JavaProjectService.FieldDefinition> fields,
                                    CobolIntermediateModel model) {
        log.debug("Rendering service implementation for class: {}", classBase);

        String className = classBase + "ServiceImpl";
        String interfaceName = classBase + "Service";
        String dtoName = classBase + "DTO";

        ClassName interfaceClass = ClassName.get("org.shark.renovatio.generated.cobol", interfaceName);
        ClassName dtoClass = ClassName.get("org.shark.renovatio.generated.cobol", dtoName);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(interfaceClass)
                .addAnnotation(ClassName.get("org.springframework.stereotype", "Service"))
                .addJavadoc("Implementation of $L\n", interfaceName)
                .addJavadoc("Generated from COBOL program: $L\n", classBase);

        if (entryPoints != null && !entryPoints.isEmpty()) {
            // Generate implementation for each ENTRY point
            String defaultEntryMethod = null;
            for (Map<String, Object> entry : entryPoints) {
                String entryName = (String) entry.get("name");
                if (entryName != null && !entryName.isEmpty()) {
                    String methodName = toCamelCase(entryName);
                    if (defaultEntryMethod == null) {
                        defaultEntryMethod = methodName;
                    }
                    MethodSpec entryMethod = MethodSpec.methodBuilder(methodName)
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .addParameter(dtoClass, "input")
                            .returns(dtoClass)
                            .addStatement("// TODO: Implement COBOL business logic for ENTRY $L", entryName)
                            .addStatement("$T out = new $T()", dtoClass, dtoClass)
                            .addStatement("// Placeholder setter to be replaced by semantic transpiler if available")
                            .addStatement("out.setResult(null)")
                            .addStatement("return out")
                            .build();
                    classBuilder.addMethod(entryMethod);
                }
            }
            classBuilder.addMethod(buildProcessMethod(dtoClass, defaultEntryMethod));
        } else {
            classBuilder.addMethod(buildProcessMethod(dtoClass, null));
        }

        // Implement validate method
        MethodSpec validateMethod = buildValidateMethod(dtoClass, fields);
        classBuilder.addMethod(validateMethod);

        TypeSpec classSpec = classBuilder.build();
        JavaFile javaFile = JavaFile.builder("org.shark.renovatio.generated.cobol", classSpec)
                .build();

        String implementation = javaFile.toString();

        // Enrich with semantic transpiler if model is available
        if (model != null) {
            implementation = semanticTranspiler.enrichServiceImplementation(implementation, model);
        }

        return implementation;
    }

    private MethodSpec buildProcessMethod(ClassName dtoClass, String defaultEntryMethod) {
        MethodSpec.Builder processMethod = MethodSpec.methodBuilder("process")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoClass, "input")
                .returns(dtoClass);

        if (defaultEntryMethod != null) {
            processMethod.addStatement("return $L(input)", defaultEntryMethod);
        } else {
            processMethod.addStatement("$T output = new $T()", dtoClass, dtoClass)
                    .addStatement("return output");
        }
        return processMethod.build();
    }

    /**
     * Render a CICS controller.
     *
     * @param classBase the base class name
     * @param cicsCommands the CICS commands
     * @return the rendered controller source code
     */
    public String renderCicsController(String classBase, Set<String> cicsCommands) {
        log.debug("Rendering CICS controller for class: {}", classBase);

        Map<String, Object> tmplData = new HashMap<>();
        tmplData.put("className", classBase + "CicsController");
        tmplData.put("transactions", cicsCommands);

        try {
            return templateService.generateCicsController(tmplData);
        } catch (Exception e) {
            log.error("Failed to render CICS controller for classBase {}: {}", classBase, e.getMessage());
            throw new RuntimeException("Failed to render CICS controller", e);
        }
    }

    private void addProcessMethod(TypeSpec.Builder interfaceBuilder, ClassName dtoClass) {
        MethodSpec processMethod = MethodSpec.methodBuilder("process")
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .addParameter(dtoClass, "input")
                .returns(dtoClass)
                .addJavadoc("Process the COBOL program logic with given input\n")
                .addJavadoc("@param input Input data structure\n")
                .addJavadoc("@return Processed output data structure\n")
                .build();
        interfaceBuilder.addMethod(processMethod);
    }

    private void addFieldToClass(TypeSpec.Builder classBuilder, JavaProjectService.FieldDefinition field) {
        TypeName typeName = resolveTypeName(field.javaType());

        FieldSpec fieldSpec = FieldSpec.builder(typeName, field.name(), Modifier.PRIVATE)
                .build();

        classBuilder.addField(fieldSpec);

        // Add getter
        MethodSpec getter = MethodSpec.methodBuilder(getterName(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return $L", field.name())
                .build();
        classBuilder.addMethod(getter);

        // Add setter
        MethodSpec setter = MethodSpec.methodBuilder(setterName(field.name()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName, field.name())
                .addStatement("this.$L = $L", field.name(), field.name())
                .build();
        classBuilder.addMethod(setter);
    }

    private MethodSpec buildValidateMethod(ClassName dtoClass, List<JavaProjectService.FieldDefinition> fields) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("validate")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(dtoClass, "input")
                .returns(boolean.class);

        methodBuilder.addStatement("if (input == null) { return false; }");

        if (fields.isEmpty()) {
            methodBuilder.addStatement("return true");
            return methodBuilder.build();
        }

        for (JavaProjectService.FieldDefinition field : fields) {
            addValidationStatements(methodBuilder, field);
        }

        methodBuilder.addStatement("return true");
        return methodBuilder.build();
    }

    private void addValidationStatements(MethodSpec.Builder methodBuilder, JavaProjectService.FieldDefinition field) {
        String accessor = "input." + getterName(field.name());
        switch (field.javaType()) {
            case "String" -> addStringValidation(methodBuilder, field, accessor);
            case "Integer", "Long" -> addIntegerValidation(methodBuilder, field, accessor);
            case "BigDecimal" -> addBigDecimalValidation(methodBuilder, field, accessor);
            default -> methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        }
    }

    private void addStringValidation(MethodSpec.Builder methodBuilder, JavaProjectService.FieldDefinition field, String accessor) {
        CodeBlock.Builder condition = CodeBlock.builder();
        condition.add("$L == null", accessor);
        if (field.maxLength() != null && field.maxLength() > 0) {
            condition.add(" || $L.length() > $L", accessor, field.maxLength());
        } else {
            condition.add(" || $L.isBlank()", accessor);
        }
        methodBuilder.addStatement("if ($L) { return false; }", condition.build());
    }

    private void addIntegerValidation(MethodSpec.Builder methodBuilder, JavaProjectService.FieldDefinition field, String accessor) {
        methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        if (!field.allowsNegative()) {
            methodBuilder.addStatement("if ($L < 0) { return false; }", accessor);
        }
        if (field.precision() != null && field.precision() > 0) {
            int scale = field.scale() != null ? field.scale() : 0;
            int digits = field.precision() - scale;
            if (digits > 0) {
                methodBuilder.addStatement(
                        "if (String.valueOf(Math.abs($L)).length() > $L) { return false; }",
                        accessor, digits);
            }
        }
    }

    private void addBigDecimalValidation(MethodSpec.Builder methodBuilder, JavaProjectService.FieldDefinition field, String accessor) {
        methodBuilder.addStatement("if ($L == null) { return false; }", accessor);
        if (!field.allowsNegative()) {
            methodBuilder.addStatement("if ($L.compareTo($T.ZERO) < 0) { return false; }",
                    accessor, BigDecimal.class);
        }
    }

    private TypeName resolveTypeName(String javaType) {
        return switch (javaType) {
            case "String" -> ClassName.get(String.class);
            case "Integer" -> TypeName.INT.box();
            case "Long" -> TypeName.LONG.box();
            case "BigDecimal" -> ClassName.get(BigDecimal.class);
            case "Boolean" -> TypeName.BOOLEAN.box();
            default -> ClassName.get(Object.class);
        };
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;

        for (char c : input.toCharArray()) {
            if (c == '_' || c == '-' || c == ' ') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    private String getterName(String fieldName) {
        return "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private String setterName(String fieldName) {
        return "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
}
