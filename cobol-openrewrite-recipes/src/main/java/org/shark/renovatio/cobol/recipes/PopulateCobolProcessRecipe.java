package org.shark.renovatio.cobol.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.model.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PopulateCobolProcessRecipe extends org.openrewrite.Recipe {

    public static final String CONTEXT_KEY = "renovatio.cobol.ir";

    @Option(displayName = "Method name",
            description = "Name of the method to populate with COBOL logic.",
            example = "process",
            required = false)
    @Nullable
    private final String methodName;

    public PopulateCobolProcessRecipe() {
        this("process");
    }

    public PopulateCobolProcessRecipe(@Nullable String methodName) {
        this.methodName = methodName == null ? "process" : methodName;
    }

    @Override
    public String getDisplayName() {
        return "Populate COBOL service method";
    }

    @Override
    public String getDescription() {
        return "Replaces TODO markers in generated service methods with statements derived from the COBOL IR.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(3);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new TargetMethodPresent(), new PopulateVisitor());
    }

    private class TargetMethodPresent extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (isTargetMethod(method) && method.getBody() != null && containsTodo(method.getBody())) {
                return method;
            }
            return method;
        }
    }

    private class PopulateVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            CobolIntermediateModel model = ctx.getMessage(CONTEXT_KEY);
            if (model == null) {
                return method;
            }
            if (!isTargetMethod(method) || method.getBody() == null) {
                return method;
            }
            if (!containsTodo(method.getBody())) {
                return method;
            }
            List<String> rendered = renderParagraph(model.getEntryParagraph());
            if (rendered.isEmpty()) {
                return method;
            }
            String returnType = method.getReturnTypeExpression() != null
                    ? method.getReturnTypeExpression().printTrimmed(getCursor())
                    : "void";
            String dtoType = !"void".equals(returnType) ? returnType : inferDtoTypeFromParameters(method);
            if (dtoType == null) {
                return method;
            }
            String bodyTemplate = buildBody(rendered, dtoType);
            return method.withBody(JavaTemplateSupport.applyTemplate(getCursor(), method.getBody(), bodyTemplate));
        }
    }

    private boolean isTargetMethod(J.MethodDeclaration method) {
        return method.getSimpleName().equals(methodName);
    }

    private boolean containsTodo(J.Block body) {
        return body.getStatements().stream()
                .map(stmt -> stmt.printTrimmed())
                .anyMatch(text -> text != null && text.contains("TODO"));
    }

    private List<String> renderParagraph(CobolParagraph paragraph) {
        List<String> lines = new ArrayList<>();
        for (CobolStatement statement : paragraph.getStatements()) {
            if (statement instanceof MoveStatement move) {
                lines.add(renderMove(move));
                continue;
            }
            if (statement instanceof ComputeStatement compute) {
                lines.add(renderCompute(compute));
                continue;
            }
            if (statement instanceof IfStatement ifStatement) {
                lines.addAll(renderIf(ifStatement));
                continue;
            }
            if (statement instanceof PerformStatement perform) {
                lines.add(renderPerform(perform));
                continue;
            }
            if (statement instanceof Db2Statement db2) {
                lines.add(renderDb2(db2));
            }
        }
        return lines;
    }

    private List<String> renderIf(IfStatement ifStatement) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(Locale.ROOT, "if (%s) {", translateCondition(ifStatement.getCondition())));
        for (CobolStatement stmt : ifStatement.getThenStatements()) {
            lines.add(indent(renderSingle(stmt)));
        }
        if (!ifStatement.getElseStatements().isEmpty()) {
            lines.add("} else {");
            for (CobolStatement stmt : ifStatement.getElseStatements()) {
                lines.add(indent(renderSingle(stmt)));
            }
        }
        lines.add("}");
        return lines;
    }

    private String renderSingle(CobolStatement statement) {
        if (statement instanceof MoveStatement move) {
            return renderMove(move);
        }
        if (statement instanceof ComputeStatement compute) {
            return renderCompute(compute);
        }
        if (statement instanceof Db2Statement db2) {
            return renderDb2(db2);
        }
        return "// Unhandled COBOL statement";
    }

    private String renderMove(MoveStatement move) {
        return String.format(Locale.ROOT, "output.%s(%s);",
                toSetter(move.getTarget()), toJavaExpression(move.getSource()));
    }

    private String renderCompute(ComputeStatement compute) {
        return String.format(Locale.ROOT, "output.%s(%s);",
                toSetter(compute.getTarget()), translateExpression(compute.getExpression()));
    }

    private String renderPerform(PerformStatement perform) {
        return String.format(Locale.ROOT, "// TODO: PERFORM %s", perform.getParagraph());
    }

    private String renderDb2(Db2Statement db2) {
        return String.format(Locale.ROOT, "// EXEC SQL %s", db2.getSql());
    }

    private String translateCondition(String condition) {
        String javaCondition = condition
                .replace("=", "==")
                .replace("<>", "!=")
                .replace("NOT =", "!=")
                .replace("THEN", "");
        return toJavaExpression(javaCondition.trim());
    }

    private String translateExpression(String expression) {
        return expression
                .replace("**,", "Math.pow")
                .replace("\n", " ")
                .trim();
    }

    private String toSetter(String cobolName) {
        return "set" + toPascal(cobolName);
    }

    private String toJavaExpression(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("'")) {
            return trimmed;
        }
        if (trimmed.matches("\".*\"")) {
            return trimmed;
        }
        if (trimmed.matches("[0-9]+")) {
            return trimmed;
        }
        if (trimmed.equalsIgnoreCase("TRUE") || trimmed.equalsIgnoreCase("FALSE")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return String.format(Locale.ROOT, "input.get%s()", toPascal(trimmed));
    }

    private String toPascal(String cobolName) {
        String normalized = cobolName.replace(".", "").replace("-", " ").trim();
        String[] parts = normalized.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            sb.append(part.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    private String indent(String value) {
        return "    " + value;
    }

    private String buildBody(List<String> statements, String dtoType) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append(String.format(Locale.ROOT, "    %s output = new %s();\n", dtoType, dtoType));
        for (String statement : statements) {
            builder.append("    ").append(statement).append('\n');
        }
        builder.append("    return output;\n");
        builder.append("}");
        return builder.toString();
    }

    private String inferDtoTypeFromParameters(J.MethodDeclaration method) {
        if (method.getParameters().isEmpty()) {
            return null;
        }
        J first = method.getParameters().get(0);
        if (first instanceof J.VariableDeclarations declarations && declarations.getTypeExpression() != null) {
            return declarations.getTypeExpression().printTrimmed(getCursor());
        }
        return null;
    }
}
