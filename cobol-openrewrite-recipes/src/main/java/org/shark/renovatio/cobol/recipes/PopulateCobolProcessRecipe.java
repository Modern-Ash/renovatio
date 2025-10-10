package org.shark.renovatio.cobol.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new TargetMethodPresent(), new PopulateVisitor());
    }

    private class TargetMethodPresent extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);
            if (isTargetMethod(m)) {
                return SearchResult.found(m);
            }
            return m;
        }
    }

    private class PopulateVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            CobolIntermediateModel model = ctx.getMessage(CONTEXT_KEY);
            if (model == null) {
                return method;
            }
            if (method.getBody() == null) {
                return method;
            }

            // Try to find a paragraph matching the method name
            CobolParagraph paragraph = findParagraphForMethod(method, model);
            if (paragraph == null) {
                // Fallback to default method name check and entry paragraph
                if (!isTargetMethod(method)) {
                    return method;
                }
                paragraph = model.getEntryParagraph();
            }
            
            List<String> rendered = renderParagraph(paragraph);
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
            return JavaTemplateSupport.replaceMethodBody(getCursor(), method, bodyTemplate);
        }
        
        private CobolParagraph findParagraphForMethod(J.MethodDeclaration method, CobolIntermediateModel model) {
            String methodName = method.getSimpleName();
            // Try to match method name to paragraph name (convert camelCase to UPPER-CASE)
            String cobolName = camelCaseToCobolName(methodName);
            return model.findParagraph(cobolName).orElse(null);
        }
        
        private String camelCaseToCobolName(String camelCase) {
            // Convert camelCase to COBOL-STYLE-NAME
            // e.g., "add" -> "ADD", "subtract" -> "SUBTRACT"
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < camelCase.length(); i++) {
                char c = camelCase.charAt(i);
                if (Character.isUpperCase(c) && i > 0) {
                    result.append('-');
                }
                result.append(Character.toUpperCase(c));
            }
            return result.toString();
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
        if (condition == null) return "false";
        String raw = condition.replace("THEN", "").trim();
        // Normalize common COBOL operators
        raw = raw.replaceAll("(?i)NOT \\=", "<>");
        // Pattern: LEFT OP RIGHT (supports identifiers with dashes/dots and numeric/string literals)
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<left>[A-Za-z0-9_.-]+)\\s*(?<op>>=|<=|<>|=|>|<)\\s*(?<right>.+)");
        java.util.regex.Matcher m = p.matcher(raw);
        if (m.matches()) {
            String left = m.group("left");
            String op = m.group("op");
            String right = m.group("right").trim();
            String javaOp = switch (op) {
                case "=" -> "==";
                case "<>" -> "!=";
                default -> op;
            };
            String leftExpr = toJavaIdentifierRef(left);
            String rightExpr = toJavaExpression(right);
            return leftExpr + " " + javaOp + " " + rightExpr;
        }
        // Fallback to previous behavior for complex expressions
        String javaCondition = raw
                .replace("=", "==")
                .replace("<>", "!=");
        return toJavaExpression(javaCondition.trim());
    }

    private String toJavaIdentifierRef(String ident) {
        if (ident == null || ident.isBlank()) return ident;
        // If it's a pure number or quoted string, delegate to toJavaExpression
        String t = ident.trim();
        if (t.matches("[0-9]+") || t.startsWith("\"") || t.startsWith("'")) {
            return toJavaExpression(t);
        }
        // Map COBOL variable name to getter on input
        return String.format(java.util.Locale.ROOT, "input.get%s()", toPascal(t));
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
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            String escaped = inner.replace("\\", "\\\\").replace("\"", "\\\"");
            return "\"" + escaped + "\"";
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
            return declarations.getTypeExpression().printTrimmed();
        }
        return null;
    }
}
