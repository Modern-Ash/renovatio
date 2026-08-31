package org.shark.renovatio.cobol.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationApplicationOutcome;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationApplicator;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;

import java.util.*;

public class PopulateCobolProcessRecipe extends Recipe {

    public static final String CONTEXT_KEY = "renovatio.cobol.ir";
    public static final String ANNOTATED_CONTEXT_KEY = AnnotatedCobolContext.CONTEXT_KEY;

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
    public @NonNull String getDisplayName() {
        return "Populate COBOL service method";
    }

    @Override
    public @NonNull String getDescription() {
        return "Replaces TODO markers in generated service methods with statements derived from the COBOL IR.";
    }

    @Override
    public @NonNull TreeVisitor<?, ExecutionContext> getVisitor() {
        // Apply to all methods; internal logic will decide which methods to transform
        return new PopulateVisitor();
    }

    // Helper to check whether the current method matches the configured target name
    private boolean isTargetMethod(J.MethodDeclaration method) {
        return method != null && method.getSimpleName().equals(methodName);
    }

    private class PopulateVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public @NonNull J.CompilationUnit visitCompilationUnit(@NonNull J.CompilationUnit compilationUnit,
                                                              @NonNull ExecutionContext ctx) {
            J.CompilationUnit populated = super.visitCompilationUnit(compilationUnit, ctx);
            AnnotatedCobolContext annotated = ctx.getMessage(ANNOTATED_CONTEXT_KEY);
            CobolIntermediateModel model = resolveModel(ctx);
            if (annotated == null || model == null || annotated.baseModel() != model) {
                return populated;
            }

            AnnotationApplicationOutcome outcome = new AnnotationApplicator(model, annotated.sidecar())
                    .apply(populated, ctx);
            List<DroppedAnnotation> accumulated = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
            if (accumulated == null) {
                accumulated = new ArrayList<>();
                ctx.putMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY, accumulated);
            }
            accumulated.addAll(outcome.dropped());
            return outcome.tree();
        }

        @Override
        public @NonNull J.MethodDeclaration visitMethodDeclaration(@NonNull J.MethodDeclaration method, @NonNull ExecutionContext ctx) {
            CobolIntermediateModel model = resolveModel(ctx);
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
            
            List<String> rendered = renderParagraph(paragraph, model, new LinkedHashSet<>(), null);
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

            // Determine DTO variable name to use:
            // - For the target method (usually 'process'), reuse existing var if present (e.g., 'output').
            // - For ENTRY-mapped methods (e.g., add/subtract/...), always use 'out'.
            String dtoVarName;
            if (isTargetMethod(method)) {
                dtoVarName = findDtoVarName(method, dtoType);
                if (dtoVarName == null || dtoVarName.isBlank()) {
                    dtoVarName = "out";
                }
            } else {
                dtoVarName = "out";
            }

            // Re-render with the chosen variable name
            rendered = renderParagraph(paragraph, model, new LinkedHashSet<>(), dtoVarName);

            String bodyTemplate = buildBody(rendered, dtoType, dtoVarName);
            return JavaTemplateSupport.replaceMethodBody(getCursor(), method, bodyTemplate);
        }

        private CobolIntermediateModel resolveModel(ExecutionContext ctx) {
            CobolIntermediateModel legacy = ctx.getMessage(CONTEXT_KEY);
            AnnotatedCobolContext annotated = ctx.getMessage(ANNOTATED_CONTEXT_KEY);
            if (annotated == null) return legacy;
            if (legacy != annotated.baseModel()) {
                // Invalid or independently reconstructed wrappers are ignored; orchestration owns diagnostics.
                return legacy;
            }
            return annotated.baseModel();
        }

        private CobolParagraph findParagraphForMethod(J.MethodDeclaration method, CobolIntermediateModel model) {
            String methodName = method.getSimpleName();
            String cobolName = camelCaseToCobolName(methodName);
            return model.findParagraph(cobolName).orElse(null);
        }

        private String camelCaseToCobolName(String camelCase) {
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

        // Infer DTO type from the first parameter using non-deprecated print
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

        private @Nullable String findDtoVarName(J.MethodDeclaration method, String dtoType) {
            return Optional.ofNullable(method.getBody())
                    .map(body -> {
                        String fromReturn = findReturnIdentifierVarName(body);
                        return (fromReturn != null && !fromReturn.isBlank())
                                ? fromReturn
                                : findDeclaredDtoVarName(body, dtoType);
                    })
                    .orElse(null);
        }

        private @Nullable String findReturnIdentifierVarName(J.Block body) {
            return body.getStatements().stream()
                    .filter(s -> s instanceof J.Return)
                    .map(s -> (J.Return) s)
                    .map(J.Return::getExpression)
                    .filter(Objects::nonNull)
                    .filter(expr -> expr instanceof J.Identifier)
                    .map(expr -> ((J.Identifier) expr).getSimpleName())
                    .findFirst()
                    .orElse(null);
        }

        private @Nullable String findDeclaredDtoVarName(J.Block body, String dtoType) {
            String simpleDto = simpleName(dtoType);
            return body.getStatements().stream()
                    .filter(s -> s instanceof J.VariableDeclarations)
                    .map(s -> (J.VariableDeclarations) s)
                    .map(v -> extractVarNameIfMatches(v, simpleDto))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }

        private @Nullable String extractVarNameIfMatches(J.VariableDeclarations v, String simpleDto) {
            if (v.getVariables().isEmpty()) {
                return null;
            }
            String declaredType = v.getTypeExpression() != null ? simpleName(v.getTypeExpression().printTrimmed(getCursor())) : null;
            if (declaredType != null && declaredType.equals(simpleDto)) {
                return v.getVariables().get(0).getName().getSimpleName();
            }
            if (v.getVariables().get(0).getInitializer() instanceof J.NewClass nc) {
                String initType = nc.getClazz() != null ? simpleName(nc.getClazz().printTrimmed(getCursor())) : null;
                if (initType != null && initType.equals(simpleDto)) {
                    return v.getVariables().get(0).getName().getSimpleName();
                }
            }
            return null;
        }

        private List<String> renderParagraph(CobolParagraph paragraph, CobolIntermediateModel model, Set<String> visitedParagraphs, @Nullable String varName) {
            if (paragraph == null) {
                return List.of();
            }

            String upperName = paragraph.name().toUpperCase(Locale.ROOT);
            if (!visitedParagraphs.add(upperName)) {
                return List.of(String.format(Locale.ROOT,
                        "// Recursive PERFORM of paragraph %s detected, skipping expansion", upperName));
            }

            try {
                List<String> lines = new ArrayList<>();
                for (CobolStatement statement : paragraph.statements()) {
                    lines.addAll(renderStatement(statement, model, visitedParagraphs, varName));
                }
                return lines;
            } finally {
                visitedParagraphs.remove(upperName);
            }
        }

        private List<String> renderStatement(CobolStatement statement,
                                             CobolIntermediateModel model,
                                             Set<String> visitedParagraphs,
                                             @Nullable String varName) {
            if (statement instanceof MoveStatement move) {
                return List.of(renderMove(move, varName));
            }
            if (statement instanceof ComputeStatement compute) {
                return List.of(renderCompute(compute, varName));
            }
            if (statement instanceof IfStatement ifStatement) {
                return renderIf(ifStatement, model, visitedParagraphs, varName);
            }
            if (statement instanceof PerformStatement perform) {
                return renderPerform(perform, model, visitedParagraphs, varName);
            }
            if (statement instanceof EvaluateStatement evaluate) {
                return renderEvaluate(evaluate, model, visitedParagraphs, varName);
            }
            if (statement instanceof Db2Statement db2) {
                return List.of(renderDb2(db2));
            }
            if (statement instanceof CallStatement call) {
                return List.of(renderCall(call));
            }
            if (statement instanceof FileOperationStatement fileOp) {
                return List.of(renderFileOperation(fileOp));
            }
            return List.of("// Unhandled COBOL statement");
        }

        private List<String> renderIf(IfStatement ifStatement,
                                      CobolIntermediateModel model,
                                      Set<String> visitedParagraphs,
                                      @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            lines.add(String.format(Locale.ROOT, "if (%s) {", translateCondition(ifStatement.condition())));
            for (CobolStatement stmt : ifStatement.thenStatements()) {
                for (String rendered : renderStatement(stmt, model, visitedParagraphs, varName)) {
                    lines.add(indent(rendered));
                }
            }
            if (!ifStatement.elseStatements().isEmpty()) {
                lines.add("} else {");
                for (CobolStatement stmt : ifStatement.elseStatements()) {
                    for (String rendered : renderStatement(stmt, model, visitedParagraphs, varName)) {
                        lines.add(indent(rendered));
                    }
                }
            }
            lines.add("}");
            return lines;
        }

        private String renderMove(MoveStatement move, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            return String.format(Locale.ROOT, "%s.%s(%s);",
                    targetVar, toSetter(move.target()), toJavaExpression(move.source()));
        }

        private String renderCompute(ComputeStatement compute, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            return String.format(Locale.ROOT, "%s.%s(%s);",
                    targetVar, toSetter(compute.target()), translateExpression(compute.expression()));
        }

        private List<String> renderPerform(PerformStatement perform,
                                           CobolIntermediateModel model,
                                           Set<String> visitedParagraphs,
                                           @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            if (perform.paragraph() == null || perform.paragraph().isBlank()) {
                lines.add("// PERFORM with unnamed paragraph");
                return lines;
            }

            model.findParagraph(perform.paragraph()).ifPresentOrElse(target -> {
                List<String> nested = renderParagraph(target, model, new LinkedHashSet<>(visitedParagraphs), varName);
                if (nested.isEmpty()) {
                    lines.add(String.format(Locale.ROOT,
                            "// PERFORM %s (paragraph is empty)", perform.paragraph()));
                } else {
                    lines.addAll(nested);
                }
            }, () -> lines.add(String.format(Locale.ROOT,
                    "// PERFORM %s (paragraph not found)", perform.paragraph())));

            if (perform.throughParagraph() != null) {
                lines.add(String.format(Locale.ROOT,
                        "// PERFORM THRU %s not yet expanded", perform.throughParagraph()));
            }
            return lines;
        }

        private List<String> renderEvaluate(EvaluateStatement evaluate,
                                            CobolIntermediateModel model,
                                            Set<String> visitedParagraphs,
                                            @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            String selector = toJavaExpression(evaluate.expression());
            lines.add(String.format(Locale.ROOT, "switch (%s) {", selector));
            for (EvaluateStatement.EvaluateWhenBranch branch : evaluate.branches()) {
                String label = branch.condition().equalsIgnoreCase("OTHER")
                        ? "default"
                        : "case " + toJavaExpression(branch.condition());
                lines.add(indent(label + " -> {"));
                for (CobolStatement stmt : branch.statements()) {
                    for (String rendered : renderStatement(stmt, model, visitedParagraphs, varName)) {
                        lines.add(indent(indent(rendered)));
                    }
                }
                lines.add(indent("}"));
            }
            lines.add("}");
            return lines;
        }

        private String renderDb2(Db2Statement db2) {
            return String.format(Locale.ROOT, "// EXEC SQL %s", db2.sql());
        }

        private String renderCall(CallStatement call) {
            StringBuilder builder = new StringBuilder();
            builder.append("// CALL ").append(call.target());
            if (!call.arguments().isEmpty()) {
                builder.append(" USING ")
                        .append(String.join(", ", call.arguments()));
            }
            return builder.toString();
        }

        private String renderFileOperation(FileOperationStatement fileOp) {
            return String.format(Locale.ROOT, "// %s %s", fileOp.operationType(), fileOp.fileName());
        }

        private String translateCondition(String condition) {
            if (condition == null) return "false";
            String raw = condition.replace("THEN", "").trim();
            // Normalize common COBOL operators
            raw = raw.replaceAll("(?i)NOT =", "<>");
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
            if (expression == null || expression.isBlank()) {
                return "null";
            }
            // First, replace power operator and normalize whitespace
            String normalized = expression
                    .replace("**", "Math.pow")
                    .replace("\n", " ")
                    .trim();
            
            // Split by operators while preserving them
            // Pattern matches: +, -, *, /, (, ), and whitespace
            String[] tokens = normalized.split("(?<=[-+*/()])|(?=[-+*/()])");
            StringBuilder result = new StringBuilder();
            
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                // Keep operators and parentheses as-is
                if (trimmed.matches("[-+*/()]")) {
                    result.append(" ").append(trimmed).append(" ");
                }
                // Keep numeric literals as-is
                else if (trimmed.matches("[0-9]+\\.?[0-9]*")) {
                    result.append(trimmed);
                }
                // Keep quoted strings as-is
                else if (trimmed.startsWith("\"") || trimmed.startsWith("'")) {
                    result.append(trimmed);
                }
                // Convert COBOL variable names to Java getter calls
                else if (trimmed.matches("[A-Za-z][A-Za-z0-9-]*")) {
                    result.append(toJavaIdentifierRef(trimmed));
                }
                // Keep other tokens as-is (e.g., Math.pow)
                else {
                    result.append(trimmed);
                }
            }
            
            return result.toString().trim();
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

        private String buildBody(List<String> statements, String dtoType, String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            builder.append(String.format(Locale.ROOT, "    %s %s = new %s();\n", dtoType, targetVar, dtoType));
            for (String statement : statements) {
                builder.append("    ").append(statement).append('\n');
            }
            builder.append(String.format(Locale.ROOT, "    return %s;\n", targetVar));
            builder.append("}");
            return builder.toString();
        }

        private String simpleName(String fqOrSimple) {
            int idx = fqOrSimple.lastIndexOf('.');
            return idx >= 0 ? fqOrSimple.substring(idx + 1) : fqOrSimple;
        }
    }
}
