package org.modernash.renovatio.cobol.recipes;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.modernash.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.modernash.renovatio.cobol.ir.model.*;
import org.modernash.renovatio.cobol.recipes.annotate.AnnotationApplicationOutcome;
import org.modernash.renovatio.cobol.recipes.annotate.AnnotationApplicator;
import org.modernash.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.modernash.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.modernash.renovatio.semantic.ir.SemanticProgram;

import java.util.*;

public class PopulateCobolProcessRecipe extends Recipe {

    private record ResolvedCondition(CobolDataItem parent, Level88Condition condition) { }

    private static final Set<String> FIGURATIVE_ZERO = Set.of("ZERO", "ZEROS", "ZEROES");
    private static final Set<String> FIGURATIVE_SPACE = Set.of("SPACE", "SPACES");

    public static final String CONTEXT_KEY = "renovatio.cobol.ir";
    /** One DISPLAY operand: a single- or double-quoted literal, or a run of non-space characters. */
    private static final java.util.regex.Pattern DISPLAY_OPERAND =
            java.util.regex.Pattern.compile("'[^']*'|\"[^\"]*\"|\\S+");
    public static final String ANNOTATED_CONTEXT_KEY = AnnotatedCobolContext.CONTEXT_KEY;
    public static final String SEMANTIC_DATA_INTENTS_KEY = "renovatio.semantic.data-intents";

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

        /**
         * COBOL paragraphs already rendered into stub methods. They are excluded from
         * PERFORM method extraction because they already exist as callable methods.
         */
        private final Set<String> renderedParagraphs = new LinkedHashSet<>();

        /** DTO type used by the first rendered service method (signature of extracted perform methods). */
        private String serviceDtoType;

        /** True while rendering extracted perform methods, where GOBACK/STOP RUN become {@code return;}. */
        private boolean voidMethodMode;

        /** Paragraph currently being rendered, used to detect PERFORM cycles within an SCC. */
        private String currentParagraphName;

        private PerformAnalysis performAnalysis;

        /** COBOL identifier -> Java expression aliases scoped to the current inline PERFORM body. */
        private Map<String, String> variableAliases = Map.of();

        /** COBOL data items written by any generated statement; reads of these go to the output DTO. */
        private Set<String> assignedVariables = Set.of();

        /** Output DTO variable name in scope for the paragraph being rendered (out inside perform methods). */
        private String currentOutputVar = "out";

        /** Pascal-cased names of COBOL data items present in the model; the DTO exposes getters for these. */
        private Set<String> knownDataNames = Set.of();

        /** Model of the program being rendered. */
        private CobolIntermediateModel currentModel;

        /** The program is online (EXEC CICS): EIB registers, DFH constants and screen I/O apply. */
        private boolean cicsProgram;

        /** The run can end inside a performed paragraph: GOBACK there calls endRun(). */
        private boolean endsFromParagraphs;

        /** Pascal-cased OCCURS items the DTO models as arrays, with their size. */
        private Map<String, Integer> tableSizes = Map.of();

        /** Java type of each known data item, keyed like {@link #knownDataNames}. */
        private Map<String, String> dataTypes = Map.of();

        private static final String UNRESOLVED_MARKER = "\u00A7RENO_UNRESOLVED\u00A7";

        private static final java.util.regex.Pattern UNRESOLVED_PATTERN =
                java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(UNRESOLVED_MARKER) + "([A-Za-z0-9-]+)");

        @Override
        public @NonNull J.CompilationUnit visitCompilationUnit(@NonNull J.CompilationUnit compilationUnit,
                                                              @NonNull ExecutionContext ctx) {
            CobolIntermediateModel model = resolveModel(ctx);
            if (model != null) {
                // Compute before super descends into method declarations so rendering sees the set.
                assignedVariables = computeAssignedVariables(model);
            }
            J.CompilationUnit populated = super.visitCompilationUnit(compilationUnit, ctx);
            if (model == null) {
                return populated;
            }
            AnnotatedCobolContext annotated = ctx.getMessage(ANNOTATED_CONTEXT_KEY);
            if (annotated != null && annotated.baseModel() != model) {
                return populated;
            }

            J.CompilationUnit tree = populated;
            if (annotated != null) {
                List<SemanticProgram.DataIntent> neutralDataIntents = ctx.getMessage(SEMANTIC_DATA_INTENTS_KEY);
                AnnotationApplicationOutcome outcome = new AnnotationApplicator(model, annotated.sidecar(),
                        neutralDataIntents)
                        .apply(populated, ctx);
                List<DroppedAnnotation> accumulated = ctx.getMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY);
                if (accumulated == null) {
                    accumulated = new ArrayList<>();
                    ctx.putMessage(AnnotationOutcomeKey.ANNOTATION_OUTCOMES_KEY, accumulated);
                }
                accumulated.addAll(outcome.dropped());
                tree = outcome.tree();
            }
            return extractPerformMethods(tree, model);
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

            // Re-render with the chosen variable name; local variable numbering restarts per method.
            fileResultCounter = 0;
            rendered = renderParagraph(paragraph, model, new LinkedHashSet<>(), dtoVarName);

            String bodyTemplate = buildBody(rendered, dtoType, dtoVarName);
            J.MethodDeclaration updated = JavaTemplateSupport.replaceMethodBody(getCursor(), method, bodyTemplate);
            renderedParagraphs.add(paragraph.name().toUpperCase(Locale.ROOT));
            if (serviceDtoType == null) {
                serviceDtoType = dtoType;
            }
            return updated;
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

            currentModel = model;
            String upperName = paragraph.name().toUpperCase(Locale.ROOT);
            if (!visitedParagraphs.add(upperName)) {
                return List.of(String.format(Locale.ROOT,
                        "// Recursive PERFORM of paragraph %s detected, skipping expansion", upperName));
            }

            String previousParagraph = currentParagraphName;
            String previousOutputVar = currentOutputVar;
            currentParagraphName = upperName;
            currentOutputVar = (varName == null || varName.isBlank()) ? "out" : varName;
            knownDataNames = knownDataNamesOf(model);
            try {
                List<String> lines = new ArrayList<>();
                for (CobolStatement statement : paragraph.statements()) {
                    lines.addAll(renderStatement(statement, model, visitedParagraphs, varName));
                    // A GOBACK/STOP RUN here, or one reached through an inlined PERFORM, ends the
                    // run unit: stop so no unreachable Java follows.
                    if (terminatesFlow(statement)
                            || (!lines.isEmpty() && lines.get(lines.size() - 1).startsWith("return "))) {
                        break;
                    }
                }
                return lines;
            } finally {
                currentParagraphName = previousParagraph;
                currentOutputVar = previousOutputVar;
                visitedParagraphs.remove(upperName);
            }
        }

        /**
         * True when {@code statement} unconditionally ends the run unit ({@code GOBACK} / {@code STOP
         * RUN}). Callers stop rendering later statements in the same block so the generated method
         * has no unreachable Java after the {@code return}.
         */
        private boolean terminatesFlow(CobolStatement statement) {
            return statement instanceof SimpleStatement simple
                    && (simple.kind() == SimpleStatement.Kind.GOBACK
                        || simple.kind() == SimpleStatement.Kind.STOP_RUN
                        || cicsProgram && simple.text().toUpperCase(Locale.ROOT)
                                .matches("EXEC CICS (XCTL|RETURN)\\b.*"));
        }

        private List<String> renderStatement(CobolStatement statement,
                                             CobolIntermediateModel model,
                                             Set<String> visitedParagraphs,
                                             @Nullable String varName) {
            List<String> lines = renderStatementLines(statement, model, visitedParagraphs, varName);
            if (lines.stream().anyMatch(line -> line.contains(UNRESOLVED_MARKER))) {
                Set<String> names = new LinkedHashSet<>();
                for (String line : lines) {
                    java.util.regex.Matcher matcher = UNRESOLVED_PATTERN.matcher(line);
                    while (matcher.find()) {
                        names.add(matcher.group(1));
                    }
                }
                String label = names.isEmpty() ? "unknown data item" : String.join(", ", names);
                return List.of("// COBOL not translated: " + truncate(label) + " (data item not modeled)");
            }
            return lines;
        }

        private List<String> renderStatementLines(CobolStatement statement,
                                                  CobolIntermediateModel model,
                                                  Set<String> visitedParagraphs,
                                                  @Nullable String varName) {
            if (statement instanceof MoveStatement move) {
                return List.of(renderMove(move, model, varName));
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
                return renderCall(call, model, visitedParagraphs, varName);
            }
            if (statement instanceof FileOperationStatement fileOp) {
                return renderFileOperation(fileOp, model, visitedParagraphs, varName);
            }
            if (statement instanceof InitializeStatement initialize) {
                return renderInitialize(initialize, model, varName);
            }
            if (statement instanceof SetConditionStatement setCondition) {
                return renderSetCondition(setCondition, model, varName);
            }
            if (statement instanceof SimpleStatement simple) {
                return renderSimple(simple, varName);
            }
            return List.of("// Unhandled COBOL statement");
        }

        private List<String> renderSimple(SimpleStatement simple, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            switch (simple.kind()) {
                case CONTINUE:
                    return List.of("; // CONTINUE");
                case GOBACK:
                case STOP_RUN:
                    if (voidMethodMode) {
                        // A performed paragraph ends the whole run, not just this method.
                        return List.of(endsFromParagraphs ? targetVar + ".endRun();" : "return;");
                    }
                    return List.of(String.format(Locale.ROOT, "return %s;", targetVar));
                case DISPLAY:
                    return List.of(String.format(Locale.ROOT, "System.out.println(%s);",
                            renderDisplayArguments(simple.text())));
                case UNTRANSLATED:
                default:
                    if (cicsProgram && simple.text().toUpperCase(Locale.ROOT).startsWith("EXEC CICS")) {
                        return renderCics(simple.text(), targetVar);
                    }
                    return List.of("// COBOL not translated: " + truncate(simple.text()));
            }
        }

        private String renderDisplayArguments(String operandText) {
            if (operandText == null || operandText.isBlank()) {
                return "";
            }
            List<String> parts = new ArrayList<>();
            java.util.regex.Matcher matcher = DISPLAY_OPERAND.matcher(operandText.trim());
            while (matcher.find()) {
                String token = matcher.group().trim();
                if (token.isEmpty()) {
                    continue;
                }
                parts.add(displayOperand(token));
            }
            return parts.isEmpty() ? "" : String.join(" + ", parts);
        }

        /**
         * DISPLAY form of an operand. An unsigned USAGE DISPLAY numeric item shows its zero-padded
         * digits without the implied decimal point, as COBOL prints it.
         * shortcut: signed, binary and packed items print Java's toString; IBM and GnuCOBOL differ
         * there (overpunch vs separate sign), pick one when a golden master needs it.
         */
        private String displayOperand(String token) {
            String expression = toJavaExpression(token);
            Optional<CobolDataItem> item = currentModel == null || isQuoted(token) ? Optional.empty()
                    : findDataItem(currentModel, stripSubscripts(unqualifiedOrSelf(token)));
            if (item.isEmpty() || item.get().picType() == null || expression.contains(UNRESOLVED_MARKER)) {
                return expression;
            }
            var pic = item.get().picType();
            if (pic.category() != org.modernash.renovatio.cobol.runtime.PicType.Category.NUMERIC || pic.signed()
                    || pic.usage() != org.modernash.renovatio.cobol.runtime.PicType.Usage.DISPLAY) {
                return expression;
            }
            String digits = "BigDecimal".equals(item.get().javaType())
                    ? expression + ".setScale(" + pic.scale() + ", java.math.RoundingMode.DOWN).unscaledValue()"
                    : expression;
            return "String.format(\"%0" + pic.digits() + "d\", " + digits + ")";
        }

        private String unqualifiedOrSelf(String reference) {
            String unqualified = unqualify(reference);
            return unqualified == null ? reference : unqualified;
        }

        private static String truncate(String value) {
            String text = value == null ? "" : value.replace('\n', ' ').trim();
            return text.length() > 120 ? text.substring(0, 120) : text;
        }

        private List<String> renderIf(IfStatement ifStatement,
                                      CobolIntermediateModel model,
                                      Set<String> visitedParagraphs,
                                      @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            lines.add(String.format(Locale.ROOT, "if (%s) {", translateCondition(ifStatement.condition())));
            renderBranch(ifStatement.thenStatements(), model, visitedParagraphs, varName, lines);
            if (!ifStatement.elseStatements().isEmpty()) {
                lines.add("} else {");
                renderBranch(ifStatement.elseStatements(), model, visitedParagraphs, varName, lines);
            }
            lines.add("}");
            return lines;
        }

        private void renderBranch(List<CobolStatement> statements, CobolIntermediateModel model,
                                  Set<String> visitedParagraphs, @Nullable String varName, List<String> lines) {
            for (CobolStatement stmt : statements) {
                for (String rendered : renderStatement(stmt, model, visitedParagraphs, varName)) {
                    lines.add(indent(rendered));
                }
                if (terminatesFlow(stmt)) {
                    break;
                }
            }
        }

        private String renderMove(MoveStatement move, CobolIntermediateModel model,
                                  @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            Optional<CobolDataItem> target = findDataItem(model, stripSubscripts(move.target()));
            String source = move.source().trim().toUpperCase(Locale.ROOT);
            int groupLength = target.isEmpty() ? textLength(move.target()) : 0;
            if (groupLength > 0 && (FIGURATIVE_SPACE.contains(source) || source.matches("(LOW|HIGH)-VALUES?"))) {
                // Group or group view not in the data items: fill its whole record.
                String fill = FIGURATIVE_SPACE.contains(source) ? " " : source.startsWith("LOW") ? "\\u0000" : "\\u00FF";
                return setterCall(targetVar, move.target(), quoted(fill.repeat(groupLength))) + ";";
            }
            String expression = moveSourceExpression(move.source().trim(), target.orElse(null));
            return setterCall(targetVar, move.target(), expression) + ";";
        }

        /**
         * Java expression for a MOVE sending operand, converted to the receiving item's Java type.
         * Handles figurative constants, signed/decimal numeric literals and data items of a
         * different Java type. PICTURE-exact truncation and padding are out of scope here.
         */
        private String moveSourceExpression(String source, @Nullable CobolDataItem target) {
            String targetType = target == null ? null : target.javaType();
            String upper = source.toUpperCase(Locale.ROOT);
            if (targetType != null && FIGURATIVE_ZERO.contains(upper)) {
                return switch (targetType) {
                    case "String" -> quoted("0".repeat(alphanumericLength(target)));
                    case "Long" -> "0L";
                    case "BigDecimal" -> "java.math.BigDecimal.ZERO";
                    default -> "0";
                };
            }
            if (targetType != null && FIGURATIVE_SPACE.contains(upper) && "String".equals(targetType)) {
                return quoted(" ".repeat(alphanumericLength(target)));
            }
            if ("String".equals(targetType) && upper.matches("(LOW|HIGH)-VALUES?")) {
                return quoted((upper.startsWith("LOW") ? "\\u0000" : "\\u00FF").repeat(alphanumericLength(target)));
            }
            if (source.matches("[+-]?[0-9]*\\.?[0-9]+")) {
                return numericLiteral(source, targetType);
            }
            String expression = toJavaExpression(source);
            if (targetType == null || expression.contains(UNRESOLVED_MARKER)) {
                return expression;
            }
            String sourceType = source.startsWith("'") || source.startsWith("\"")
                    ? "String"
                    : dataTypes.get(toPascal(stripSubscripts(source)));
            return convert(expression, sourceType, targetType);
        }

        private String numericLiteral(String literal, @Nullable String targetType) {
            String plain = literal.startsWith("+") ? literal.substring(1) : literal;
            if (targetType == null) {
                return plain;
            }
            String integral = new java.math.BigDecimal(plain).toBigInteger().toString();
            return switch (targetType) {
                case "String" -> quoted(plain.replace("-", ""));
                case "BigDecimal" -> "new java.math.BigDecimal(\"" + plain + "\")";
                case "Long" -> integral + "L";
                case "Integer" -> integral;
                default -> plain;
            };
        }

        /** Converts {@code expression} of {@code sourceType} to {@code targetType} (boxed types). */
        private String convert(String expression, @Nullable String sourceType, String targetType) {
            if (sourceType == null || sourceType.equals(targetType)) {
                return expression;
            }
            return switch (targetType) {
                case "String" -> "BigDecimal".equals(sourceType)
                        ? expression + ".toPlainString()"
                        : "String.valueOf(" + expression + ")";
                case "Integer" -> "String".equals(sourceType)
                        ? "Integer.valueOf(" + expression + ".trim())"
                        : expression + ".intValue()";
                case "Long" -> "String".equals(sourceType)
                        ? "Long.valueOf(" + expression + ".trim())"
                        : expression + ".longValue()";
                case "BigDecimal" -> "String".equals(sourceType)
                        ? "new java.math.BigDecimal(" + expression + ".trim())"
                        : "java.math.BigDecimal.valueOf(" + expression + ")";
                default -> expression;
            };
        }

        private int alphanumericLength(CobolDataItem item) {
            if (item.picType() != null && item.picType().digits() > 0) {
                return item.picType().digits();
            }
            return currentModel == null ? 1 : currentModel.findComposableLayout(item.name())
                    .map(CobolRecordLayout::length).orElse(1);
        }

        private String quoted(String value) {
            return "\"" + value + "\"";
        }

        private String renderCompute(ComputeStatement compute, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            Optional<CobolDataItem> target = currentModel == null ? Optional.empty()
                    : findDataItem(currentModel, stripSubscripts(compute.target()));
            Arithmetic value = new ArithmeticParser(compute.expression()).parse();
            String converted = target.isEmpty() || value == null ? null : toTargetType(value, target.orElseThrow());
            if (converted == null) {
                return "// COBOL not translated: COMPUTE " + truncate(compute.target() + " = " + compute.expression());
            }
            return setterCall(targetVar, compute.target(), converted) + ";";
        }

        /** A translated arithmetic expression: Java code and whether it is BigDecimal-valued. */
        private record Arithmetic(String code, boolean decimal) { }

        /**
         * Result of an arithmetic expression stored into a receiving item: decimal results are
         * truncated to the item's scale (COBOL truncates unless ROUNDED).
         */
        private @Nullable String toTargetType(Arithmetic value, CobolDataItem target) {
            int scale = target.picType() == null ? 0 : target.picType().scale();
            return switch (target.javaType()) {
                case "Integer" -> value.decimal() ? value.code() + ".intValue()" : "(int) (" + value.code() + ")";
                case "Long" -> value.decimal() ? value.code() + ".longValue()" : "(long) (" + value.code() + ")";
                case "BigDecimal" -> (value.decimal() ? value.code() : "java.math.BigDecimal.valueOf(" + value.code() + ")")
                        + ".setScale(" + scale + ", java.math.RoundingMode.DOWN)";
                default -> null;
            };
        }

        /** Recursive-descent translation of a COBOL arithmetic expression (+ - * / ** unary, parens). */
        private final class ArithmeticParser {
            private final List<String> tokens = new ArrayList<>();
            private int position;

            ArithmeticParser(String expression) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
                        "\\*\\*|[-+*/()]|[0-9]*\\.?[0-9]+|[A-Za-z0-9][A-Za-z0-9-]*(?:\\([^()]*\\))?|\\S")
                        .matcher(expression == null ? "" : expression);
                while (matcher.find()) {
                    tokens.add(matcher.group());
                }
            }

            @Nullable Arithmetic parse() {
                Arithmetic result = expression();
                return result != null && position == tokens.size() ? result : null;
            }

            private @Nullable String peek() {
                return position < tokens.size() ? tokens.get(position) : null;
            }

            private @Nullable Arithmetic expression() {
                Arithmetic left = term();
                while (left != null && ("+".equals(peek()) || "-".equals(peek()))) {
                    String op = tokens.get(position++);
                    Arithmetic right = term();
                    if (right == null) return null;
                    left = combine(left, op, right);
                }
                return left;
            }

            private @Nullable Arithmetic term() {
                Arithmetic left = factor();
                while (left != null && ("*".equals(peek()) || "/".equals(peek()))) {
                    String op = tokens.get(position++);
                    Arithmetic right = factor();
                    if (right == null) return null;
                    left = combine(left, op, right);
                }
                return left;
            }

            private @Nullable Arithmetic factor() {
                if ("-".equals(peek()) || "+".equals(peek())) {
                    boolean negate = "-".equals(tokens.get(position++));
                    Arithmetic operand = factor();
                    if (operand == null || !negate) return operand;
                    return new Arithmetic(operand.decimal() ? operand.code() + ".negate()" : "(-" + operand.code() + ")",
                            operand.decimal());
                }
                Arithmetic base = primary();
                if (base != null && "**".equals(peek())) {
                    position++;
                    String exponent = peek();
                    if (exponent == null || !exponent.matches("[0-9]+")) return null;
                    position++;
                    return new Arithmetic(asDecimal(base) + ".pow(" + exponent + ")", true);
                }
                return base;
            }

            private @Nullable Arithmetic primary() {
                String token = peek();
                if (token == null) return null;
                position++;
                if ("(".equals(token)) {
                    Arithmetic inner = expression();
                    if (inner == null || !")".equals(peek())) return null;
                    position++;
                    return new Arithmetic("(" + inner.code() + ")", inner.decimal());
                }
                if (token.matches("[0-9]*\\.[0-9]+")) {
                    return new Arithmetic("new java.math.BigDecimal(\"" + token + "\")", true);
                }
                if (token.matches("[0-9]+")) {
                    return new Arithmetic(token.length() > 9 ? token + "L" : token, false);
                }
                String upper = token.toUpperCase(Locale.ROOT);
                if (FIGURATIVE_ZERO.contains(upper)) {
                    return new Arithmetic("0", false);
                }
                String type = variableAliases.containsKey(upper) ? "Integer"
                        : dataTypes.get(toPascal(stripSubscripts(token)));
                if (type == null || "String".equals(type)) return null;
                String expression = toJavaIdentifierRef(token);
                if (expression.contains(UNRESOLVED_MARKER)) return null;
                return new Arithmetic(expression, "BigDecimal".equals(type));
            }

            private Arithmetic combine(Arithmetic left, String op, Arithmetic right) {
                if (!left.decimal() && !right.decimal() && !"/".equals(op)) {
                    return new Arithmetic(left.code() + " " + op + " " + right.code(), false);
                }
                String method = switch (op) {
                    case "+" -> "add";
                    case "-" -> "subtract";
                    case "*" -> "multiply";
                    default -> "divide";
                };
                String argument = asDecimal(right) + ("divide".equals(method) ? ", java.math.MathContext.DECIMAL128" : "");
                return new Arithmetic(asDecimal(left) + "." + method + "(" + argument + ")", true);
            }

            private String asDecimal(Arithmetic value) {
                return value.decimal() ? value.code() : "java.math.BigDecimal.valueOf(" + value.code() + ")";
            }
        }

        private List<String> renderInitialize(InitializeStatement initialize,
                                              CobolIntermediateModel model,
                                              @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            List<String> lines = new ArrayList<>();
            for (String target : initialize.targets()) {
                Optional<CobolDataItem> item = findDataItem(model, target);
                if (item.isEmpty()) {
                    lines.add("// COBOL not translated: INITIALIZE " + truncate(target)
                            + " (not an elementary data item)");
                    continue;
                }
                Optional<String> initialValue = CobolDataVerbValueResolver.initialValue(item.orElseThrow());
                if (initialValue.isEmpty()) {
                    lines.add("// COBOL not translated: INITIALIZE " + truncate(target)
                            + " (unsupported Java type " + item.orElseThrow().javaType() + ")");
                    continue;
                }
                lines.add(setterCall(targetVar, target, initialValue.orElseThrow()) + ";");
            }
            return lines;
        }

        private List<String> renderSetCondition(SetConditionStatement setCondition,
                                                CobolIntermediateModel model,
                                                @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            List<String> lines = new ArrayList<>();
            for (String conditionName : setCondition.conditionNames()) {
                Optional<ResolvedCondition> resolved = findCondition(model, conditionName);
                if (resolved.isEmpty()) {
                    lines.add("// COBOL not translated: SET " + truncate(conditionName)
                            + " TO " + setCondition.value() + " (unknown level-88 condition)");
                    continue;
                }
                ResolvedCondition condition = resolved.orElseThrow();
                Optional<String> value = CobolDataVerbValueResolver.conditionValue(
                        condition.parent(), condition.condition(), setCondition.value());
                if (value.isEmpty()) {
                    lines.add("// COBOL not translated: " + truncate(setCondition.sourceText())
                            + " (no representable level-88 value)");
                    continue;
                }
                lines.add(String.format(Locale.ROOT, "%s.%s(%s);", targetVar,
                        toSetter(condition.parent().name()), value.orElseThrow()));
            }
            return lines;
        }

        private Optional<CobolDataItem> findDataItem(CobolIntermediateModel model, String name) {
            return model.getDataItems().stream()
                    .filter(item -> item.name().equalsIgnoreCase(name))
                    .findFirst();
        }

        /**
         * Pascal-cased names of the elementary data items: the generated DTO carries one field per
         * elementary item. Group items have no field of their own, so a reference to one stays an
         * explicit "not translated" marker instead of an accessor that does not compile.
         */
        private Set<String> knownDataNamesOf(CobolIntermediateModel model) {
            Set<String> names = new LinkedHashSet<>();
            Map<String, String> types = new HashMap<>();
            for (CobolDataItem item : model.getDataItems()) {
                if (item.picture() == null || item.name().equalsIgnoreCase("FILLER")) {
                    continue;
                }
                String pascal = toPascal(item.name());
                names.add(pascal);
                types.putIfAbsent(pascal, item.javaType());
            }
            // A composable group is read and written as its DISPLAY record string.
            for (CobolRecordLayout layout : model.getRecordLayouts().values()) {
                if (layout.composable()) {
                    String pascal = toPascal(layout.groupName());
                    names.add(pascal);
                    types.putIfAbsent(pascal, "String");
                }
            }
            cicsProgram = model.usesCics();
            endsFromParagraphs = model.endsFromPerformedParagraphs();
            if (cicsProgram) {
                for (String register : CICS_TYPES.keySet()) {
                    names.add(toPascal(register));
                    types.putIfAbsent(toPascal(register), CICS_TYPES.get(register));
                }
            }
            dataTypes = types;
            Map<String, Integer> tables = new HashMap<>();
            model.getTableDimensions().forEach((name, size) -> {
                if (size > 0) {
                    tables.put(toPascal(name), size);
                } else {
                    names.remove(toPascal(name)); // tables generation does not model stay unresolved
                }
            });
            tableSizes = tables;
            for (CobolRedefinesView view : model.getRedefinesViews().values()) {
                if (!view.supported()) {
                    names.remove(toPascal(view.name())); // shared storage not modeled
                } else if (view.picType() == null) {
                    // A group view is read and written as text, like a composable group.
                    names.add(toPascal(view.name()));
                    types.putIfAbsent(toPascal(view.name()), "String");
                }
            }
            return names;
        }

        private Set<String> computeAssignedVariables(CobolIntermediateModel model) {
            Set<String> assigned = new LinkedHashSet<>();
            for (CobolParagraph paragraph : model.getParagraphs().values()) {
                collectAssignedStatements(paragraph.statements(), assigned, model);
            }
            // A group and its fields share storage: writing either one changes what the other
            // reads, so both must then be read from the output DTO.
            boolean changed = true;
            while (changed) {
                changed = false;
                for (CobolRecordLayout layout : model.getRecordLayouts().values()) {
                    if (!layout.composable()) {
                        continue;
                    }
                    String group = layout.groupName().toUpperCase(Locale.ROOT);
                    List<String> fields = layout.fields().stream()
                            .filter(field -> !field.filler())
                            .map(field -> field.name().toUpperCase(Locale.ROOT))
                            .toList();
                    if (assigned.contains(group)) {
                        changed |= assigned.addAll(fields);
                    } else if (fields.stream().anyMatch(assigned::contains)) {
                        changed |= assigned.add(group);
                    }
                }
                // A REDEFINES view and its base are the same bytes.
                for (CobolRedefinesView view : model.getRedefinesViews().values()) {
                    if (view.supported() && assigned.contains(view.name()) != assigned.contains(view.base())) {
                        changed |= assigned.add(view.name()) | assigned.add(view.base());
                    }
                }
            }
            return assigned;
        }

        private void collectAssignedStatements(List<CobolStatement> statements,
                                               Set<String> assigned,
                                               CobolIntermediateModel model) {
            for (CobolStatement statement : statements) {
                if (statement instanceof MoveStatement move) {
                    addAssigned(assigned, move.target());
                } else if (statement instanceof ComputeStatement compute) {
                    addAssigned(assigned, compute.target());
                } else if (statement instanceof InitializeStatement initialize) {
                    for (String target : initialize.targets()) {
                        addAssigned(assigned, target);
                    }
                } else if (statement instanceof SetConditionStatement setCondition) {
                    for (String conditionName : setCondition.conditionNames()) {
                        findCondition(model, conditionName)
                                .ifPresent(condition -> addAssigned(assigned, condition.parent().name()));
                    }
                } else if (statement instanceof CallStatement call) {
                    for (int index = 0; index < call.arguments().size(); index++) {
                        if (call.byReference().get(index)) {
                            addAssigned(assigned, call.arguments().get(index));
                        }
                    }
                    collectAssignedStatements(call.exceptionStatements(), assigned, model);
                    collectAssignedStatements(call.normalStatements(), assigned, model);
                } else if (statement instanceof FileOperationStatement file) {
                    model.findFile(file.fileName()).ifPresent(descriptor -> {
                        addAssigned(assigned, descriptor.fileStatus());
                        if (file.operationType() == FileOperationStatement.OperationType.READ
                                || file.recordArea() != null) {
                            addAssigned(assigned, descriptor.recordName());
                        }
                    });
                    if (file.operationType() == FileOperationStatement.OperationType.READ) {
                        addAssigned(assigned, file.recordArea());
                    }
                    collectAssignedStatements(file.exceptionStatements(), assigned, model);
                    collectAssignedStatements(file.normalStatements(), assigned, model);
                } else if (statement instanceof SimpleStatement simple
                        && CobolCicsCommand.parse(simple.text()) instanceof CobolCicsCommand cics) {
                    // CICS writes RECEIVE / READ INTO areas, RESP codes and the key a browse returns.
                    for (String option : List.of("INTO", "RESP", "RESP2")) {
                        addAssigned(assigned, cics.options().get(option));
                    }
                    if (List.of("ASKTIME", "FORMATTIME", "ASSIGN").contains(cics.verb())) {
                        cics.options().forEach((option, item) -> {
                            if (!List.of("ABSTIME", "DATESEP", "TIMESEP").contains(option)
                                    || cics.verb().equals("ASKTIME")) {
                                addAssigned(assigned, item);
                            }
                        });
                    }
                    if (cics.verb().startsWith("READ") && !cics.verb().equals("READ")) {
                        addAssigned(assigned, cics.options().get("RIDFLD"));
                    }
                    if (cics.verb().equals("RECEIVE") && cics.options().get("MAP") != null
                            && !cics.options().containsKey("INTO")) {
                        addAssigned(assigned, cics.options().get("MAP").replace("'", "") + "I");
                    }
                } else if (statement instanceof IfStatement branch) {
                    collectAssignedStatements(branch.thenStatements(), assigned, model);
                    collectAssignedStatements(branch.elseStatements(), assigned, model);
                } else if (statement instanceof EvaluateStatement evaluate) {
                    for (EvaluateStatement.EvaluateWhenBranch branch : evaluate.branches()) {
                        collectAssignedStatements(branch.statements(), assigned, model);
                    }
                } else if (statement instanceof PerformStatement perform) {
                    addAssigned(assigned, perform.varyingVariable());
                    for (PerformStatement.VaryingAxis axis : perform.varyingAfter()) {
                        addAssigned(assigned, axis.variable());
                    }
                    if (perform.isInline()) {
                        collectAssignedStatements(perform.inlineBody(), assigned, model);
                    }
                }
            }
        }

        private static void addAssigned(Set<String> assigned, String name) {
            if (name != null && !name.isBlank()) {
                assigned.add(stripSubscripts(name).trim().toUpperCase(Locale.ROOT));
            }
        }

        /** {@code OF group} / {@code IN group} qualifiers of a data reference. */
        private static final java.util.regex.Pattern QUALIFIER = java.util.regex.Pattern.compile(
                "(?i)\\s+(?:OF|IN)\\s+[A-Z0-9][A-Z0-9-]*");

        /**
         * The reference without OF/IN qualifiers (subscripts kept): DTO members are named by the
         * unqualified name. {@code null} when that name is declared more than once, since the DTO
         * then holds only one of them.
         */
        private @Nullable String unqualify(String reference) {
            String t = reference.trim();
            if (!QUALIFIER.matcher(t).find()) {
                return t;
            }
            String base = t.split("[\\s(]", 2)[0].toUpperCase(Locale.ROOT);
            if (currentModel != null && currentModel.getAmbiguousDataNames().contains(base)) {
                return null;
            }
            return QUALIFIER.matcher(t).replaceAll("");
        }

        /** Removes trailing COBOL subscripts (e.g. {@code (1)}, {@code (1:2)}) from a data reference. */
        private static String stripSubscripts(String cobolRef) {
            if (cobolRef == null) {
                return null;
            }
            String s = QUALIFIER.matcher(cobolRef.trim()).replaceAll("");
            while (s.length() > 2 && s.endsWith(")") && s.indexOf('(') >= 0) {
                int open = s.lastIndexOf('(');
                if (open <= 0) {
                    break;
                }
                String inner = s.substring(open + 1, s.length() - 1);
                if (inner.indexOf('(') >= 0 || inner.indexOf(')') >= 0) {
                    break;
                }
                s = s.substring(0, open).trim();
            }
            return s;
        }

        private Optional<ResolvedCondition> findCondition(CobolIntermediateModel model, String name) {
            for (CobolDataItem item : model.getDataItems()) {
                for (Level88Condition condition : item.level88Conditions()) {
                    if (condition.name().equalsIgnoreCase(name)) {
                        return Optional.of(new ResolvedCondition(item, condition));
                    }
                }
            }
            return Optional.empty();
        }

        private List<String> renderPerform(PerformStatement perform,
                                           CobolIntermediateModel model,
                                           Set<String> visitedParagraphs,
                                           @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            List<String> inner = new ArrayList<>();
            if (perform.isInline()) {
                Map<String, String> savedAliases = variableAliases;
                Map<String, String> loopAliases = new java.util.HashMap<>();
                if (perform.varyingVariable() != null && !perform.varyingVariable().isBlank()) {
                    loopAliases.put(perform.varyingVariable().toUpperCase(Locale.ROOT),
                            lowerCamel(perform.varyingVariable()));
                }
                for (PerformStatement.VaryingAxis axis : perform.varyingAfter()) {
                    if (axis.variable() != null && !axis.variable().isBlank()) {
                        loopAliases.put(axis.variable().toUpperCase(Locale.ROOT), lowerCamel(axis.variable()));
                    }
                }
                if (!loopAliases.isEmpty()) {
                    variableAliases = loopAliases;
                }
                try {
                    for (CobolStatement stmt : perform.inlineBody()) {
                        inner.addAll(renderStatement(stmt, model, visitedParagraphs, targetVar));
                        if (terminatesFlow(stmt)) {
                            break;
                        }
                    }
                } finally {
                    variableAliases = savedAliases;
                }
            } else if (perform.paragraph() != null && !perform.paragraph().isBlank()) {
                if (perform.throughParagraph() == null) {
                    inner.add(performCallLine(perform.paragraph(), targetVar, model));
                } else {
                    for (String name : performRangeNames(perform.paragraph(), perform.throughParagraph(), model)) {
                        inner.add(performCallLine(name, targetVar, model));
                    }
                    if (inner.isEmpty()) {
                        inner.add("// PERFORM THRU between " + perform.paragraph()
                                + " and " + perform.throughParagraph() + " (range not found)");
                    }
                }
            } else {
                inner.add("// PERFORM with unnamed paragraph");
            }
            return wrapPerform(perform, inner);
        }

        /** Structure of the perform call graph used to detect recursive PERFORM cycles. */
        private record PerformAnalysis(Map<String, List<String>> graph,
                                       Map<String, Integer> paragraphScc,
                                       Set<Integer> cyclicSccIds,
                                       Map<String, String> nextEdge) {

            boolean isCyclicCall(String caller, String target) {
                if (caller == null || target == null) {
                    return false;
                }
                Integer callerScc = paragraphScc.get(caller);
                Integer targetScc = paragraphScc.get(target);
                return callerScc != null && callerScc.equals(targetScc) && cyclicSccIds.contains(callerScc);
            }

            static PerformAnalysis of(CobolIntermediateModel model) {
                Map<String, List<String>> graph = new LinkedHashMap<>();
                for (CobolParagraph paragraph : model.getParagraphs().values()) {
                    List<String> targets = new ArrayList<>();
                    for (CobolStatement statement : paragraph.statements()) {
                        collectPerformTargetsInto(statement, model, targets);
                    }
                    List<String> edges = targets.stream()
                            .filter(name -> model.findParagraph(name).isPresent())
                            .sorted()
                            .collect(java.util.stream.Collectors.toList());
                    graph.put(paragraph.name().toUpperCase(Locale.ROOT), edges);
                }

                Map<String, Integer> scc = new LinkedHashMap<>();
                Set<Integer> cyclicIds = new LinkedHashSet<>();
                computeScc(graph, scc, cyclicIds);

                Map<String, String> nextEdge = new LinkedHashMap<>();
                List<String> cyclicNodes = scc.keySet().stream()
                        .filter(node -> cyclicIds.contains(scc.get(node)))
                        .sorted()
                        .collect(java.util.stream.Collectors.toList());
                for (String node : cyclicNodes) {
                    Integer nodeScc = scc.get(node);
                    String next = graph.getOrDefault(node, List.of()).stream()
                            .filter(neighbor -> nodeScc.equals(scc.get(neighbor)))
                            .sorted()
                            .findFirst()
                            .orElse(node);
                    nextEdge.put(node, next);
                }
                return new PerformAnalysis(graph, scc, cyclicIds, nextEdge);
            }

            private static void computeScc(Map<String, List<String>> graph,
                                           Map<String, Integer> scc,
                                           Set<Integer> cyclicIds) {
                Map<String, Integer> indexes = new HashMap<>();
                Map<String, Integer> lowLinks = new HashMap<>();
                ArrayDeque<String> stack = new ArrayDeque<>();
                Set<String> onStack = new HashSet<>();
                int[] indexCounter = {0};
                List<List<String>> components = new ArrayList<>();

                for (String node : graph.keySet()) {
                    if (!indexes.containsKey(node)) {
                        strongConnect(node, graph, indexes, lowLinks, stack, onStack,
                                indexCounter, components);
                    }
                }
                for (List<String> component : components) {
                    boolean cyclic = component.size() > 1
                            || graph.getOrDefault(component.get(0), List.of()).contains(component.get(0));
                    int id = -components.indexOf(component) - 1;
                    if (cyclic) {
                        cyclicIds.add(id);
                    }
                    for (String node : component) {
                        scc.put(node, id);
                    }
                }
            }

            private static void strongConnect(String vertex,
                                              Map<String, List<String>> graph,
                                              Map<String, Integer> indexes,
                                              Map<String, Integer> lowLinks,
                                              ArrayDeque<String> stack,
                                              Set<String> onStack,
                                              int[] indexCounter,
                                              List<List<String>> components) {
                indexes.put(vertex, indexCounter[0]);
                lowLinks.put(vertex, indexCounter[0]);
                indexCounter[0]++;
                stack.push(vertex);
                onStack.add(vertex);
                for (String neighbor : graph.getOrDefault(vertex, List.of())) {
                    if (!indexes.containsKey(neighbor)) {
                        strongConnect(neighbor, graph, indexes, lowLinks, stack, onStack,
                                indexCounter, components);
                        lowLinks.put(vertex, Math.min(lowLinks.get(vertex), lowLinks.get(neighbor)));
                    } else if (onStack.contains(neighbor)) {
                        lowLinks.put(vertex, Math.min(lowLinks.get(vertex), indexes.get(neighbor)));
                    }
                }
                if (Objects.equals(lowLinks.get(vertex), indexes.get(vertex))) {
                    List<String> component = new ArrayList<>();
                    String member;
                    do {
                        member = stack.pop();
                        onStack.remove(member);
                        component.add(member);
                    } while (!component.contains(vertex));
                    components.add(component);
                }
            }
        }

        private PerformAnalysis performAnalysis(CobolIntermediateModel model) {
            if (performAnalysis == null) {
                performAnalysis = PerformAnalysis.of(model);
            }
            return performAnalysis;
        }

        private String performCallLine(String target, String targetVar, CobolIntermediateModel model) {
            if (model.findParagraph(target).isEmpty()) {
                return "// PERFORM " + target + " (paragraph not found)";
            }
            if (renderedParagraphs.contains(target)) {
                return "// PERFORM " + target + " (rendered as service method)";
            }
            PerformAnalysis analysis = performAnalysis(model);
            if (analysis.isCyclicCall(currentParagraphName, target)) {
                String next = analysis.nextEdge().getOrDefault(target, target);
                return "// COBOL not translated: PERFORM cycle (" + target + " -> " + next + ")";
            }
            return "perform" + toPascal(target) + "(input, " + targetVar + ");";
        }

        /** Wraps the inner statements in the loop mandated by the PERFORM modifiers. */
        private List<String> wrapPerform(PerformStatement perform, List<String> inner) {
            Integer times = perform.timesCount();
            String until = perform.untilCondition();
            String varying = perform.varyingVariable();
            if (times != null) {
                List<String> wrapped = new ArrayList<>();
                wrapped.add("for (int i = 0; i < " + toJavaExpression(String.valueOf(times)) + "; i++) {");
                for (String line : inner) {
                    wrapped.add(indent(line));
                }
                wrapped.add("}");
                return wrapped;
            }
            if (until != null && !until.isBlank() && varying == null) {
                String cond = translateCondition(until);
                List<String> wrapped = new ArrayList<>();
                if (perform.testAfter()) {
                    wrapped.add("do {");
                    for (String line : inner) {
                        wrapped.add(indent(line));
                    }
                    wrapped.add("} while (!(" + cond + "));");
                } else {
                    wrapped.add("while (!(" + cond + ")) {");
                    for (String line : inner) {
                        wrapped.add(indent(line));
                    }
                    wrapped.add("}");
                }
                return wrapped;
            }
            if (varying != null && !varying.isBlank()) {
                // AFTER axes render as loops nested inside the primary axis, innermost last.
                List<String> body = inner;
                List<PerformStatement.VaryingAxis> after = perform.varyingAfter();
                for (int k = after.size() - 1; k >= 0; k--) {
                    PerformStatement.VaryingAxis axis = after.get(k);
                    body = varyingLoop(axis.variable(), axis.from(), axis.by(), axis.until(),
                            perform.testAfter(), body);
                }
                return varyingLoop(varying, perform.varyingFrom(), perform.varyingBy(), until,
                        perform.testAfter(), body);
            }
            return inner;
        }

        /** Renders a single {@code PERFORM VARYING} axis as a Java {@code for} loop. */
        private List<String> varyingLoop(String cobolVar, String fromClause, String byClause,
                                         String untilClause, boolean testAfter, List<String> inner) {
            String loopVar = lowerCamel(cobolVar);
            String start = toJavaExpression(fromClause);
            String step = toJavaExpression(byClause);
            String cond = untilClause != null && !untilClause.isBlank()
                    ? translateCondition(untilClause, java.util.Map.of(cobolVar.toUpperCase(Locale.ROOT), loopVar))
                    : null;
            List<String> wrapped = new ArrayList<>();
            if (testAfter) {
                wrapped.add("for (int " + loopVar + " = " + start + "; ; " + loopVar + " += " + step + ") {");
                for (String line : inner) {
                    wrapped.add(indent(line));
                }
                if (cond != null) {
                    wrapped.add(indent("if (" + cond + ") break;"));
                }
                wrapped.add("}");
            } else {
                String loopCond = cond != null ? "!(" + cond + ")" : "true";
                wrapped.add("for (int " + loopVar + " = " + start + "; " + loopCond + "; "
                        + loopVar + " += " + step + ") {");
                for (String line : inner) {
                    wrapped.add(indent(line));
                }
                wrapped.add("}");
            }
            return wrapped;
        }

        private String lowerCamel(String cobolName) {
            String normalized = cobolName.replace(".", "").replace("-", " ").trim().toLowerCase(Locale.ROOT);
            String[] parts = normalized.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                if (part.isBlank()) {
                    continue;
                }
                if (i == 0) {
                    sb.append(part);
                } else {
                    sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
                }
            }
            return sb.toString();
        }

        private static void collectPerformTargetsInto(CobolStatement statement, CobolIntermediateModel model,
                                               List<String> out) {
            if (statement instanceof PerformStatement perform) {
                if (!perform.isInline() && perform.paragraph() != null && !perform.paragraph().isBlank()) {
                    String start = perform.paragraph();
                    if (!out.contains(start)) {
                        out.add(start);
                    }
                    if (perform.throughParagraph() != null && !perform.throughParagraph().equals(start)) {
                        for (String name : performRangeNames(start, perform.throughParagraph(), model)) {
                            if (!out.contains(name)) {
                                out.add(name);
                            }
                        }
                    }
                }
                for (CobolStatement nested : perform.inlineBody()) {
                    collectPerformTargetsInto(nested, model, out);
                }
            } else if (statement instanceof IfStatement ifStatement) {
                for (CobolStatement nested : ifStatement.thenStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
                for (CobolStatement nested : ifStatement.elseStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
            } else if (statement instanceof EvaluateStatement evaluate) {
                for (EvaluateStatement.EvaluateWhenBranch branch : evaluate.branches()) {
                    for (CobolStatement nested : branch.statements()) {
                        collectPerformTargetsInto(nested, model, out);
                    }
                }
            } else if (statement instanceof CallStatement call) {
                for (CobolStatement nested : call.exceptionStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
                for (CobolStatement nested : call.normalStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
            } else if (statement instanceof FileOperationStatement file) {
                for (CobolStatement nested : file.exceptionStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
                for (CobolStatement nested : file.normalStatements()) {
                    collectPerformTargetsInto(nested, model, out);
                }
            }
        }

        /**
         * Returns the COBOL paragraph names spanned by {@code PERFORM start THRU through}, in model
         * insertion order (which respects source order). Falls back to the explicit pair when the
         * range cannot be resolved, so a non-existent END marker still yields a deterministic call.
         */
        private static List<String> performRangeNames(String start, String through, CobolIntermediateModel model) {
            List<String> names = new ArrayList<>();
            boolean capture = false;
            for (String name : model.getParagraphs().keySet()) {
                if (name.equals(start)) {
                    capture = true;
                }
                if (capture) {
                    names.add(name);
                }
                if (capture && name.equals(through)) {
                    break;
                }
            }
            if (!names.contains(through)) {
                names = new ArrayList<>();
                names.add(start);
                if (through != null && !names.contains(through)) {
                    names.add(through);
                }
            }
            return names;
        }

        /**
         * Appends one extracted method per PERFORM-referenced paragraph (excluding paragraphs already
         * rendered into stub methods) and guarantees a {@code GeneratedFrom} annotation type exists
         * so the produced Java compiles standalone.
         */
        private J.CompilationUnit extractPerformMethods(J.CompilationUnit cu, CobolIntermediateModel model) {
            if (serviceDtoType == null) {
                return cu;
            }
            List<String> referenced = referencedPerformParagraphs(model);
            List<String> toExtract = new ArrayList<>();
            for (String name : referenced) {
                if (renderedParagraphs.contains(name) || model.findParagraph(name).isEmpty()) {
                    continue;
                }
                toExtract.add(name);
            }
            if (toExtract.isEmpty()) {
                return cu;
            }

            List<String> templates = new ArrayList<>();
            for (String name : toExtract) {
                templates.add(extractedMethodTemplate(name, model));
            }

            J.ClassDeclaration serviceClass = findServiceClass(cu);
            if (serviceClass == null) {
                return cu;
            }
            J.ClassDeclaration withMethods = addExtractedMethods(serviceClass, templates, toExtract, model);
            cu = replaceClass(cu, withMethods);
            return ensureGeneratedFromAnnotationType(cu);
        }

        private List<String> referencedPerformParagraphs(CobolIntermediateModel model) {
            List<String> order = new ArrayList<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            Set<String> seen = new HashSet<>();
            for (CobolParagraph paragraph : model.getParagraphs().values()) {
                List<String> targets = new ArrayList<>();
                for (CobolStatement statement : paragraph.statements()) {
                    collectPerformTargetsInto(statement, model, targets);
                }
                for (String target : targets) {
                    if (seen.add(target)) {
                        queue.add(target);
                    }
                }
            }
            while (!queue.isEmpty()) {
                String current = queue.poll();
                order.add(current);
                CobolParagraph paragraph = model.findParagraph(current).orElse(null);
                if (paragraph == null) {
                    continue;
                }
                List<String> targets = new ArrayList<>();
                for (CobolStatement statement : paragraph.statements()) {
                    collectPerformTargetsInto(statement, model, targets);
                }
                for (String target : targets) {
                    if (seen.add(target)) {
                        queue.add(target);
                    }
                }
            }
            return order;
        }

        private String extractedMethodTemplate(String name, CobolIntermediateModel model) {
            CobolParagraph paragraph = model.findParagraph(name).orElseThrow();
            String lines = paragraphLines(name, model);
            voidMethodMode = true;
            fileResultCounter = 0;
            List<String> rendered = renderParagraph(paragraph, model, new LinkedHashSet<>(), "out");
            voidMethodMode = false;

            StringBuilder sb = new StringBuilder();
            sb.append("    @GeneratedFrom(paragraph = \"").append(name)
                    .append("\", lines = \"").append(lines).append("\")\n");
            sb.append("    private void perform").append(toPascal(name)).append("(")
                    .append(serviceDtoType).append(" input, ").append(serviceDtoType).append(" out) {\n");
            for (String line : rendered) {
                sb.append("        ").append(line).append('\n');
            }
            sb.append("    }");
            return sb.toString();
        }

        private J.ClassDeclaration findServiceClass(J.CompilationUnit cu) {
            for (J type : cu.getClasses()) {
                if (!(type instanceof J.ClassDeclaration clazz)) {
                    continue;
                }
                boolean hasTargetMethod = clazz.getBody().getStatements().stream()
                        .filter(statement -> statement instanceof J.MethodDeclaration)
                        .map(statement -> (J.MethodDeclaration) statement)
                        .anyMatch(declaration -> declaration.getSimpleName().equals(methodName));
                if (hasTargetMethod) {
                    return clazz;
                }
            }
            return null;
        }

        private J.ClassDeclaration addExtractedMethods(J.ClassDeclaration clazz, List<String> templates,
                                                       List<String> names, CobolIntermediateModel model) {
            String fileName = clazz.getSimpleName();
            J.CompilationUnit parsed = parseMethodSource(fileName, templates, names, model);
            J.ClassDeclaration parsedClass = parsed.getClasses().get(0);
            List<Statement> existing = new ArrayList<>(clazz.getBody().getStatements());
            existing.addAll(parsedClass.getBody().getStatements());
            return clazz.withBody(clazz.getBody().withStatements(existing));
        }

        private J.CompilationUnit parseMethodSource(String fileName, List<String> templates,
                                                    List<String> names, CobolIntermediateModel model) {
            List<String> finalTemplates = new ArrayList<>();
            List<String> stubbed = new ArrayList<>();
            for (int i = 0; i < templates.size(); i++) {
                if (safeParse("class X {\n" + templates.get(i) + "\n}\n") instanceof J.CompilationUnit) {
                    finalTemplates.add(templates.get(i));
                } else {
                    finalTemplates.add(stubExtractedMethodTemplate(names.get(i), model));
                    stubbed.add(names.get(i));
                }
            }
            String fakeSource = "class " + fileName + " {\n" + String.join("\n", finalTemplates) + "\n}\n";
            Object parsed = safeParse(fakeSource);
            if (parsed instanceof J.CompilationUnit cu) {
                if (!stubbed.isEmpty()) {
                    System.err.println("WARN(PopulateCobolProcessRecipe): paragraph bodies not representable as "
                            + "Java; emitting comment-stub methods for " + stubbed);
                }
                return cu;
            }
            System.err.println("WARN(PopulateCobolProcessRecipe): extracted methods join failed for "
                    + fileName + "; emitting comment-stub methods for " + names);
            StringBuilder stubs = new StringBuilder();
            for (String name : names) {
                stubs.append(stubExtractedMethodTemplate(name, model));
            }
            J.CompilationUnit allStubs = (J.CompilationUnit) safeParse(
                    "class " + fileName + " {\n" + stubs + "\n}\n");
            return allStubs;
        }

        private Object safeParse(String source) {
            // ReloadableJava21Parser is not reusable: reusing an instance across sequential
            // parse() calls throws "endPosTable already set" from javac, so build a fresh parser
            // for every snippet.
            return JavaParser.fromJavaVersion().build().parse(source).findFirst().orElseThrow();
        }

        private String stubExtractedMethodTemplate(String name, CobolIntermediateModel model) {
            String lines = paragraphLines(name, model);
            return "    @GeneratedFrom(paragraph = \"" + name + "\", lines = \"" + lines + "\")\n"
                    + "    private void perform" + toPascal(name) + "("
                    + serviceDtoType + " input, " + serviceDtoType + " out) {\n"
                    + "        // COBOL not translated: " + name + " (paragraph body not representable)\n"
                    + "    }\n";
        }

        private String paragraphLines(String name, CobolIntermediateModel model) {
            ParagraphLineRange span = model.findParagraphLineRange(name).orElse(null);
            return span != null ? span.startLine() + "-" + span.endLine() : "0-0";
        }

        private J.CompilationUnit replaceClass(J.CompilationUnit cu, J.ClassDeclaration replacement) {
            List<J.ClassDeclaration> classes = new ArrayList<>();
            for (J.ClassDeclaration type : cu.getClasses()) {
                if (type.getSimpleName().equals(replacement.getSimpleName())) {
                    classes.add(replacement);
                } else {
                    classes.add(type);
                }
            }
            return cu.withClasses(classes);
        }

        private J.CompilationUnit ensureGeneratedFromAnnotationType(J.CompilationUnit cu) {
            boolean declared = cu.getClasses().stream()
                    .anyMatch(clazz -> clazz.getSimpleName().equals("GeneratedFrom"));
            if (declared) {
                return cu;
            }
            String source = "@interface GeneratedFrom {\n"
                    + "    String paragraph();\n"
                    + "    String lines();\n"
                    + "}\n";
            J.CompilationUnit parsed = (J.CompilationUnit) safeParse(source);
            List<J.ClassDeclaration> classes = new ArrayList<>(cu.getClasses());
            classes.add(parsed.getClasses().get(0));
            return cu.withClasses(classes);
        }

        private List<String> renderEvaluate(EvaluateStatement evaluate,
                                            CobolIntermediateModel model,
                                            Set<String> visitedParagraphs,
                                            @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            if (evaluate.expression().trim().equalsIgnoreCase("TRUE")) {
                return renderEvaluateTrue(evaluate, model, visitedParagraphs, varName);
            }
            String selector = toJavaExpression(evaluate.expression());
            int selectorLength = textLength(evaluate.expression());
            lines.add(String.format(Locale.ROOT, "switch (%s) {", selector));
            for (EvaluateStatement.EvaluateWhenBranch branch : evaluate.branches()) {
                String label = branch.condition().equalsIgnoreCase("OTHER")
                        ? "default"
                        : "case " + java.util.Arrays.stream(branch.condition().split("(?i)\\s+OR\\s+"))
                                .map(value -> padLiteral(toJavaExpression(value), selectorLength))
                                .collect(java.util.stream.Collectors.joining(", "));
                lines.add(indent(label + " -> {"));
                for (CobolStatement stmt : branch.statements()) {
                    for (String rendered : renderStatement(stmt, model, visitedParagraphs, varName)) {
                        lines.add(indent(indent(rendered)));
                    }
                    if (terminatesFlow(stmt)) {
                        break;
                    }
                }
                lines.add(indent("}"));
            }
            lines.add("}");
            return lines;
        }

        /** EVALUATE TRUE: each WHEN is a condition, tested in order; WHEN OTHER is the else branch. */
        private List<String> renderEvaluateTrue(EvaluateStatement evaluate, CobolIntermediateModel model,
                                                Set<String> visitedParagraphs, @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            String keyword = "if";
            for (EvaluateStatement.EvaluateWhenBranch branch : evaluate.branches()) {
                if (branch.condition().equalsIgnoreCase("OTHER")) {
                    lines.add(lines.isEmpty() ? "{" : "} else {");
                } else {
                    lines.add((lines.isEmpty() ? "" : "} else ") + keyword + " (" + translateCondition(branch.condition()) + ") {");
                }
                renderBranch(branch.statements(), model, visitedParagraphs, varName, lines);
            }
            if (!lines.isEmpty()) {
                lines.add("}");
            }
            return lines;
        }

        /** EIB registers, DFHCOMMAREA and DFH constants an online program reads, with their types. */
        private static final Map<String, String> CICS_TYPES = cicsTypes();

        private static Map<String, String> cicsTypes() {
            Map<String, String> types = new LinkedHashMap<>();
            types.put("EIBCALEN", "Integer");
            types.put("EIBAID", "String");
            types.put("DFHCOMMAREA", "String");
            types.put("DFHRESP", "Integer");
            for (String aid : List.of("ENTER", "CLEAR", "PA1", "PA2", "PA3")) types.put("DFH" + aid, "String");
            for (int key = 1; key <= 24; key++) types.put("DFHPF" + key, "String");
            return types;
        }

        /** CICS response codes by condition name (DFHRESP). */
        private static final Map<String, Integer> CICS_RESP = Map.ofEntries(
                Map.entry("NORMAL", 0), Map.entry("ERROR", 1), Map.entry("NOTFND", 13), Map.entry("DUPREC", 14),
                Map.entry("DUPKEY", 15), Map.entry("INVREQ", 16), Map.entry("NOTOPEN", 19), Map.entry("ENDFILE", 20),
                Map.entry("LENGERR", 22), Map.entry("PGMIDERR", 27), Map.entry("MAPFAIL", 36), Map.entry("QIDERR", 44));

        /** Java for an EIB register, DFHCOMMAREA or DFH constant reference, else null. */
        private @Nullable String cicsRegister(String reference) {
            String upper = reference.toUpperCase(Locale.ROOT).replaceAll("\\s+", "");
            if (upper.equals("EIBCALEN")) return "input.getScreenRequest().commareaLength()";
            if (upper.equals("EIBAID")) return "input.getScreenRequest().aid()";
            // DFHCOMMAREA(1:EIBCALEN) is the whole COMMAREA the task received.
            if (upper.equals("DFHCOMMAREA") || upper.equals("DFHCOMMAREA(1:EIBCALEN)")) {
                return "input.getScreenRequest().commarea()";
            }
            java.util.regex.Matcher resp = java.util.regex.Pattern.compile("DFHRESP\\(([A-Z]+)\\)").matcher(upper);
            if (resp.matches()) {
                Integer code = CICS_RESP.get(resp.group(1));
                return code == null ? UNRESOLVED_MARKER + "DFHRESP" : String.valueOf(code);
            }
            if (upper.startsWith("DFH") && CICS_TYPES.containsKey(upper) && !upper.equals("DFHRESP")) {
                return "\"" + upper.substring(3) + "\"";
            }
            return null;
        }

        /**
         * One EXEC CICS command as screen-handler data: RECEIVE MAP reads the request, SEND MAP / TEXT
         * fill the response, XCTL and RETURN set the navigation and end the run. Other commands stay
         * explicit markers.
         */
        private List<String> renderCics(String text, String targetVar) {
            CobolCicsCommand command = CobolCicsCommand.parse(text);
            List<String> words = command.words();
            Map<String, String> options = command.options();
            String verb = command.verb();
            String marker = "// COBOL not translated: " + truncate(text);
            if (command.fileCommand()) {
                List<String> lines = renderCicsFile(command, targetVar);
                return lines == null || lines.stream().anyMatch(line -> line.contains(UNRESOLVED_MARKER))
                        ? List.of(marker) : lines;
            }
            List<String> lines = new ArrayList<>();
            String response = targetVar + ".getScreenResponse()";
            switch (verb) {
                case "RECEIVE" -> {
                    String map = options.get("MAP");
                    String target = options.containsKey("INTO") ? options.get("INTO")
                            : literal(map) != null ? literal(map) + "I" : null;
                    if (map == null || target == null) return List.of(marker);
                    String data = "input.getScreenRequest().map(" + cicsName(map) + ")";
                    lines.add(setterCall(targetVar, target, data) + ";");
                    if (options.containsKey("RESP")) {
                        // MAPFAIL when the terminal sent no data for the map.
                        lines.add(setterCall(targetVar, options.get("RESP"), data + ".isEmpty() ? 36 : 0") + ";");
                    }
                }
                case "SEND" -> {
                    String map = options.get("MAP");
                    if (words.contains("TEXT") || map == null) {
                        String from = options.get("FROM");
                        if (from == null) return List.of(marker);
                        lines.add(response + ".sendText(" + cicsText(from) + ");");
                    } else {
                        String mapset = options.getOrDefault("MAPSET", map);
                        String from = options.containsKey("FROM") ? options.get("FROM")
                                : literal(map) != null ? literal(map) + "O" : null;
                        if (from == null) return List.of(marker);
                        lines.add(response + ".sendMap(" + cicsName(map) + ", " + cicsName(mapset) + ", "
                                + cicsText(from) + ");");
                    }
                }
                case "ASKTIME" -> {
                    String abstime = options.get("ABSTIME");
                    if (abstime == null) return List.of(marker);
                    // ABSTIME counts milliseconds since 1900-01-01.
                    lines.add(setterCall(targetVar, abstime, convert("(System.currentTimeMillis() + 2208988800000L)",
                            "Long", typeOf(abstime))) + ";");
                }
                case "FORMATTIME" -> {
                    String abstime = options.get("ABSTIME");
                    if (abstime == null) return List.of(marker);
                    String instant = "java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli("
                            + convert(toJavaExpression(abstime), typeOf(abstime), "Long")
                            + " - 2208988800000L), java.time.ZoneId.systemDefault())";
                    String dateSep = separator(words, options, "DATESEP", "/");
                    String timeSep = separator(words, options, "TIMESEP", ":");
                    for (Map.Entry<String, String> option : options.entrySet()) {
                        String pattern = switch (option.getKey()) {
                            case "YYYYMMDD" -> "yyyy'S'MM'S'dd";
                            case "MMDDYYYY" -> "MM'S'dd'S'yyyy";
                            case "DDMMYYYY" -> "dd'S'MM'S'yyyy";
                            case "YYMMDD" -> "yy'S'MM'S'dd";
                            case "MMDDYY" -> "MM'S'dd'S'yy";
                            case "DDMMYY" -> "dd'S'MM'S'yy";
                            case "TIME" -> "HH'T'mm'T'ss";
                            case "ABSTIME", "DATESEP", "TIMESEP" -> "";
                            default -> null;
                        };
                        if (pattern == null) return List.of(marker);
                        if (pattern.isEmpty()) continue;
                        pattern = pattern.replace("'S'", dateSep.isEmpty() ? "" : "'" + dateSep + "'")
                                .replace("'T'", timeSep.isEmpty() ? "" : "'" + timeSep + "'");
                        lines.add(setterCall(targetVar, option.getValue(), instant
                                + ".format(java.time.format.DateTimeFormatter.ofPattern(\"" + pattern + "\"))") + ";");
                    }
                }
                case "ASSIGN" -> {
                    for (Map.Entry<String, String> option : options.entrySet()) {
                        if (!option.getKey().equals("APPLID") && !option.getKey().equals("SYSID")) return List.of(marker);
                        lines.add(setterCall(targetVar, option.getValue(), "input.getScreenRequest()."
                                + option.getKey().toLowerCase(Locale.ROOT) + "()") + ";");
                    }
                }
                case "ABEND" -> lines.add(targetVar + ".abend("
                        + (options.containsKey("ABCODE") ? cicsText(options.get("ABCODE")) : "\"\"") + ");");
                case "XCTL" -> {
                    String program = options.get("PROGRAM");
                    if (program == null) return List.of(marker);
                    String commarea = options.containsKey("COMMAREA") ? cicsText(options.get("COMMAREA")) : "null";
                    lines.add(response + ".transfer(" + cicsText(program) + ", " + commarea + ");");
                    lines.add(targetVar + ".endRun();");
                }
                case "RETURN" -> {
                    String commarea = options.containsKey("COMMAREA") ? cicsText(options.get("COMMAREA")) : "null";
                    lines.add(options.containsKey("TRANSID")
                            ? response + ".returnTo(" + cicsText(options.get("TRANSID")) + ", " + commarea + ");"
                            : response + ".end();");
                    lines.add(targetVar + ".endRun();");
                }
                default -> {
                    return List.of(marker);
                }
            }
            if (options.containsKey("RESP") && !verb.equals("RECEIVE")) {
                // Screen, time and environment commands always complete NORMAL here.
                lines.add(lines.size() - (verb.equals("XCTL") || verb.equals("RETURN") ? 1 : 0),
                        setterCall(targetVar, options.get("RESP"), "0") + ";");
            }
            if (lines.stream().anyMatch(line -> line.contains(UNRESOLVED_MARKER))) {
                return List.of(marker);
            }
            return lines;
        }

        /**
         * A CICS file-control command as a call on the dataset's file port: the key is RIDFLD in its
         * DISPLAY form, INTO receives the record, FROM supplies it, RESP gets the condition the FILE
         * STATUS maps to. Null when the dataset or an operand is not modeled.
         */
        private @Nullable List<String> renderCicsFile(CobolCicsCommand command, String targetVar) {
            String name = currentModel == null ? null : currentModel.cicsName(command.dataset());
            CobolCicsDataset dataset = name == null ? null : currentModel.cicsDatasets().get(name);
            if (dataset == null || command.words().stream().anyMatch(word -> word.equals("GENERIC")
                    || word.equals("GTEQ") && command.verb().equals("READ"))) {
                return null;
            }
            String port = filePortField(name);
            Map<String, String> options = command.options();
            String ridfld = options.get("RIDFLD");
            String key = ridfld == null ? null : displayOperand(ridfld);
            String into = options.get("INTO");
            String from = options.get("FROM");
            if (into != null && !isStringOperand(into) || from != null && !isStringOperand(from)) {
                return null;
            }
            List<String> lines = new ArrayList<>();
            String status = "fileStatus" + (++fileResultCounter);
            switch (command.verb()) {
                case "READ", "READNEXT", "READPREV" -> {
                    String call = switch (command.verb()) {
                        case "READ" -> key == null ? null : port + ".read(" + key + ")";
                        case "READNEXT" -> port + ".read()";
                        default -> port + ".readPrevious()";
                    };
                    if (call == null) return null;
                    String result = "fileRead" + fileResultCounter;
                    lines.add("var " + result + " = " + call + ";");
                    lines.add("String " + status + " = " + result + ".status();");
                    List<String> onSuccess = new ArrayList<>();
                    if (into != null) onSuccess.add(setterCall(targetVar, into, result + ".record()") + ";");
                    boolean ridfldInRecord = ridfld != null && dataset.recordName() != null
                            && currentModel.fieldSpan(dataset.recordName(), ridfld).isPresent();
                    if (!command.verb().equals("READ") && ridfld != null && !ridfldInRecord && dataset.keyed()) {
                        // A browse returns the key of the record it read in RIDFLD.
                        String type = dataTypes.get(toPascal(stripSubscripts(ridfld)));
                        String recordKey = result + ".record().substring(" + dataset.keyOffset() + ", "
                                + (dataset.keyOffset() + dataset.keyLength()) + ")";
                        onSuccess.add(setterCall(targetVar, ridfld, convert(recordKey, "String",
                                type == null ? "String" : type)) + ";");
                    }
                    if (!onSuccess.isEmpty()) {
                        lines.add("if (" + result + ".successful()) {");
                        onSuccess.forEach(line -> lines.add(indent(line)));
                        lines.add("}");
                    }
                }
                case "STARTBR" -> {
                    if (key == null) return null;
                    lines.add("String " + status + " = " + port + ".start(" + key + ");");
                }
                case "WRITE", "REWRITE" -> {
                    if (from == null) return null;
                    lines.add("String " + status + " = " + port + "." + command.verb().toLowerCase(Locale.ROOT)
                            + "(" + toJavaExpression(from) + ");");
                }
                case "DELETE" -> lines.add("String " + status + " = " + port + ".delete("
                        + (key == null ? "null" : key) + ");");
                case "ENDBR" -> {
                    return List.of("// EXEC CICS ENDBR " + name + ": the port keeps no browse to release");
                }
                default -> {
                    return null;
                }
            }
            if (options.containsKey("RESP")) {
                lines.add(setterCall(targetVar, options.get("RESP"), port + ".resp(" + status + ")") + ";");
            }
            return lines;
        }

        /** A CICS name argument (map, mapset) as Java: the literal, or the naming item's trimmed value. */
        private String cicsName(String value) {
            String literal = literal(value);
            return literal != null ? "\"" + literal + "\"" : cicsText(value) + ".trim()";
        }

        private @Nullable String typeOf(String operand) {
            return dataTypes.get(toPascal(stripSubscripts(operand)));
        }

        /** FORMATTIME separator: absent = none, bare keyword = the default, else its literal. */
        private static String separator(List<String> words, Map<String, String> options, String name, String standard) {
            if (options.containsKey(name)) {
                String literal = literal(options.get(name));
                return literal == null ? standard : literal;
            }
            return words.contains(name) ? standard : "";
        }

        /** Content of a quoted CICS literal ('COSGN0A'), or null for anything else. */
        private static @Nullable String literal(@Nullable String value) {
            return value != null && value.matches("'[^']*'|\"[^\"]*\"") ? value.substring(1, value.length() - 1) : null;
        }

        /** A CICS argument as a Java String: literal, or a data item / group in its DISPLAY form. */
        private String cicsText(String value) {
            String literal = literal(value);
            if (literal != null) {
                return "\"" + literal + "\"";
            }
            String type = dataTypes.get(toPascal(stripSubscripts(value)));
            String getter = getterCall(value);
            return type == null || getter.contains(UNRESOLVED_MARKER) ? UNRESOLVED_MARKER + value : convert(getter, type, "String");
        }

        private String renderDb2(Db2Statement db2) {
            return String.format(Locale.ROOT, "// EXEC SQL %s", db2.sql());
        }

        /**
         * CALL as a call on the program's port: USING parameters travel as DISPLAY strings and
         * BY REFERENCE ones are written back; ON EXCEPTION runs when the program is not available.
         */
        private List<String> renderCall(CallStatement call, CobolIntermediateModel model,
                                        Set<String> visitedParagraphs, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            if (call.dynamic()) {
                return List.of("// COBOL not translated: CALL " + call.target() + " (program name resolved at run time)");
            }
            List<String> values = new ArrayList<>();
            List<String> writeBack = new ArrayList<>();
            String args = "callUsing" + (++fileResultCounter);
            for (int index = 0; index < call.arguments().size(); index++) {
                String argument = call.arguments().get(index);
                if (isQuoted(argument)) {
                    values.add(toJavaExpression(argument));
                    continue;
                }
                String type = dataTypes.get(toPascal(stripSubscripts(argument)));
                String getter = argument.matches("[A-Z0-9][A-Z0-9-]*") ? toJavaExpression(argument) : UNRESOLVED_MARKER;
                if (type == null || getter.contains(UNRESOLVED_MARKER)) {
                    return List.of("// COBOL not translated: CALL '" + call.target() + "' USING " + truncate(argument)
                            + " (parameter not modeled)");
                }
                values.add(convert(getter, type, "String"));
                if (call.byReference().get(index)) {
                    writeBack.add(setterCall(targetVar, argument, convert(args + "[" + index + "]", "String", type)) + ";");
                }
            }
            String port = filePortField(call.target()) + "Program";
            List<String> lines = new ArrayList<>();
            lines.add("String[] " + args + " = {" + String.join(", ", values) + "};");
            if (call.exceptionStatements().isEmpty()) {
                lines.add(port + ".call(" + args + ");");
                lines.addAll(writeBack);
                if (!call.normalStatements().isEmpty()) {
                    renderBranch(call.normalStatements(), model, visitedParagraphs, varName, lines);
                }
                return lines;
            }
            lines.add("try {");
            lines.add(indent(port + ".call(" + args + ");"));
            writeBack.forEach(line -> lines.add(indent(line)));
            renderBranch(call.normalStatements(), model, visitedParagraphs, varName, lines);
            lines.add("} catch (UnsupportedOperationException programNotAvailable) {");
            renderBranch(call.exceptionStatements(), model, visitedParagraphs, varName, lines);
            lines.add("}");
            return lines;
        }

        /** Counter for READ result variables, unique within the compilation unit. */
        private int fileResultCounter;

        /**
         * Renders a file statement as a call on the program's port for that file. The FILE STATUS
         * item receives the returned code; READ fills the FD record (and the INTO area) on success;
         * AT END runs on a 1x status, INVALID KEY on a 2x status, the NOT clauses on success.
         */
        private List<String> renderFileOperation(FileOperationStatement fileOp, CobolIntermediateModel model,
                                                 Set<String> visitedParagraphs, @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            String port = filePortField(fileOp.fileName());
            Optional<CobolFileDescriptor> file = model.findFile(fileOp.fileName());
            String recordName = file.map(CobolFileDescriptor::recordName).orElse(fileOp.recordName());
            String record = recordName != null && model.findComposableLayout(recordName).isPresent()
                    ? recordName : null;
            String statusSetter = file.map(CobolFileDescriptor::fileStatus)
                    .map(status -> targetVar + "." + toSetter(status))
                    .orElse(null);
            List<String> lines = new ArrayList<>();
            String call;
            switch (fileOp.operationType()) {
                case OPEN -> call = port + ".open(\"" + openMode(fileOp.openMode()) + "\")";
                case CLOSE -> call = port + ".close()";
                case READ -> {
                    return renderRead(fileOp, model, port, file.orElse(null), record, statusSetter,
                            visitedParagraphs, varName);
                }
                case WRITE, REWRITE -> {
                    String source = fileOp.recordArea() != null ? fileOp.recordArea() : fileOp.recordName();
                    if (source == null || !isStringOperand(source)) {
                        return List.of("// COBOL not translated: " + fileOp.operationType() + " " + fileOp.fileName()
                                + " (record " + source + " has no composable DISPLAY layout)");
                    }
                    if (fileOp.recordArea() != null && record != null) {
                        lines.add(targetVar + "." + toSetter(record) + "(" + toJavaExpression(fileOp.recordArea()) + ");");
                    }
                    call = port + "." + fileOp.operationType().name().toLowerCase(Locale.ROOT)
                            + "(" + toJavaExpression(source) + ")";
                }
                case DELETE, START -> {
                    String key = keyExpression(fileOp, model, file.orElse(null), record);
                    if (key == null) {
                        return List.of("// COBOL not translated: " + fileOp.operationType() + " " + fileOp.fileName()
                                + " (record key position unknown)");
                    }
                    call = port + "." + fileOp.operationType().name().toLowerCase(Locale.ROOT) + "(" + key + ")";
                }
                default -> {
                    return List.of("// COBOL not translated: " + fileOp.operationType() + " " + fileOp.fileName());
                }
            }
            if (!fileOp.hasClauses()) {
                lines.add(statusSetter != null ? statusSetter + "(" + call + ");" : call + ";");
                return lines;
            }
            String status = "fileStatus" + (++fileResultCounter);
            lines.add("String " + status + " = " + call + ";");
            if (statusSetter != null) {
                lines.add(statusSetter + "(" + status + ");");
            }
            lines.addAll(clauseBranches(status, "2", fileOp, model, visitedParagraphs, varName));
            return lines;
        }

        private List<String> renderRead(FileOperationStatement read, CobolIntermediateModel model, String port,
                                        @Nullable CobolFileDescriptor file, @Nullable String record,
                                        @Nullable String statusSetter, Set<String> visitedParagraphs,
                                        @Nullable String varName) {
            String targetVar = (varName == null || varName.isBlank()) ? "out" : varName;
            boolean keyed = file != null && file.keyed() && !read.next()
                    && file.accessMode() != CobolFileDescriptor.AccessMode.SEQUENTIAL;
            String argument = "";
            if (keyed) {
                argument = keyExpression(read, model, file, record);
                if (argument == null) {
                    return List.of("// COBOL not translated: READ " + read.fileName() + " (record key position unknown)");
                }
            }
            String result = "fileRead" + (++fileResultCounter);
            List<String> lines = new ArrayList<>();
            lines.add("var " + result + " = " + port + ".read(" + argument + ");");
            if (statusSetter != null) {
                lines.add(statusSetter + "(" + result + ".status());");
            }
            List<String> onSuccess = new ArrayList<>();
            if (record != null) {
                onSuccess.add(targetVar + "." + toSetter(record) + "(" + result + ".record());");
            } else {
                onSuccess.add("// COBOL not translated: record of " + read.fileName()
                        + " has no composable DISPLAY layout");
            }
            if (read.recordArea() != null) {
                onSuccess.add(isStringOperand(read.recordArea())
                        ? targetVar + "." + toSetter(read.recordArea()) + "(" + result + ".record());"
                        : "// COBOL not translated: READ INTO " + read.recordArea() + " (not an alphanumeric item)");
            }
            lines.add("if (" + result + ".successful()) {");
            onSuccess.forEach(line -> lines.add(indent(line)));
            lines.add("}");
            if (read.hasClauses()) {
                lines.addAll(clauseBranches(result + ".status()", keyed ? "2" : "1", read, model,
                        visitedParagraphs, varName));
            }
            return lines;
        }

        /** if (exception status) { AT END / INVALID KEY } else if (success) { NOT ... }. */
        private List<String> clauseBranches(String statusExpression, String exceptionClass,
                                            FileOperationStatement fileOp, CobolIntermediateModel model,
                                            Set<String> visitedParagraphs, @Nullable String varName) {
            List<String> lines = new ArrayList<>();
            String opening = "if";
            if (!fileOp.exceptionStatements().isEmpty()) {
                lines.add("if (" + statusExpression + ".startsWith(\"" + exceptionClass + "\")) {");
                renderBranch(fileOp.exceptionStatements(), model, visitedParagraphs, varName, lines);
                opening = "} else if";
            }
            if (!fileOp.normalStatements().isEmpty()) {
                lines.add(opening + " (" + statusExpression + ".startsWith(\"0\")) {");
                renderBranch(fileOp.normalStatements(), model, visitedParagraphs, varName, lines);
            }
            lines.add("}");
            return lines;
        }

        /** The key as the port stores it: its characters inside the DISPLAY record. */
        private @Nullable String keyExpression(FileOperationStatement fileOp, CobolIntermediateModel model,
                                               @Nullable CobolFileDescriptor file, @Nullable String record) {
            if (file == null || record == null) {
                return null;
            }
            Optional<int[]> span = model.recordKeySpan(file);
            if (span.isEmpty()) {
                return null;
            }
            int[] position = span.orElseThrow();
            return toJavaExpression(record) + ".substring(" + position[0] + ", " + (position[0] + position[1]) + ")";
        }

        private static String openMode(@Nullable FileOperationStatement.OpenMode mode) {
            if (mode == null) {
                return "INPUT";
            }
            return mode == FileOperationStatement.OpenMode.I_O ? "I-O" : mode.name();
        }

        /** Field name of the port for a file: the generator's naming ("ACCTFILE-FILE" -> acctfileFile). */
        private String filePortField(String selectName) {
            StringBuilder result = new StringBuilder();
            for (String part : selectName.split("[^A-Za-z0-9]+")) {
                if (part.isEmpty()) continue;
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
            String base = result.isEmpty() || !Character.isLetter(result.charAt(0)) ? "File" + result : result.toString();
            return Character.toLowerCase(base.charAt(0)) + base.substring(1);
        }

        private String translateCondition(String condition) {
            return translateCondition(condition, Map.of());
        }

        private String translateCondition(String condition, Map<String, String> aliases) {
            if (condition == null) return "false";
            String raw = condition.replaceAll("(?i)(^|\\s)THEN(\\s|$)", " ").replace('\n', ' ').trim();
            ConditionContext context = new ConditionContext(aliases);
            String translated = translateDisjunction(raw, context);
            if (translated != null) {
                return translated;
            }
            String subject = raw.replaceFirst("^(?i)NOT\\s+", "").split("[^A-Za-z0-9-]", 2)[0];
            return UNRESOLVED_MARKER + (subject.isEmpty() ? "CONDITION" : subject);
        }

        /** Subject and operator of the last relation, inherited by abbreviated conditions. */
        private static final class ConditionContext {
            final Map<String, String> aliases;
            String subject;
            String operator;

            ConditionContext(Map<String, String> aliases) {
                this.aliases = aliases;
            }
        }

        /** Relational operators: word forms must stand alone, symbol forms may touch operands. */
        private static final String RELATIONAL_OPERATOR =
                "(?<op>(?:(?<=\\s)(?:NOT\\s+EQUAL(?:\\s+TO)?|NOT\\s+GREATER(?:\\s+THAN)?(?:\\s+OR\\s+EQUAL(?:\\s+TO)?)?"
                + "|NOT\\s+LESS(?:\\s+THAN)?(?:\\s+OR\\s+EQUAL(?:\\s+TO)?)?|GREATER\\s+THAN\\s+OR\\s+EQUAL(?:\\s+TO)?"
                + "|GREATER(?:\\s+THAN)?|LESS\\s+THAN\\s+OR\\s+EQUAL(?:\\s+TO)?|LESS(?:\\s+THAN)?|EQUALS?(?:\\s+TO)?"
                + "|NOT\\s*(?:=|>|<))(?![A-Z0-9-]))|>=|<=|<>|=|>|<)";
        private static final java.util.regex.Pattern RELATION = java.util.regex.Pattern.compile(
                "(?i)^(?<left>.+?)\\s*(?:(?<=\\s)IS\\s+)?" + RELATIONAL_OPERATOR + "\\s*(?<right>.+)$");
        private static final java.util.regex.Pattern OPERATOR_ONLY = java.util.regex.Pattern.compile(
                "(?i)^(?:IS\\s+)?(?<op>NOT\\s+EQUAL(?:\\s+TO)?|NOT\\s*=|NOT\\s*>|NOT\\s*<"
                + "|GREATER(?:\\s+THAN)?(?:\\s+OR\\s+EQUAL(?:\\s+TO)?)?|LESS(?:\\s+THAN)?(?:\\s+OR\\s+EQUAL(?:\\s+TO)?)?"
                + "|EQUALS?(?:\\s+TO)?|>=|<=|<>|=|>|<)\\s*(?<right>.+)$");
        private static final java.util.regex.Pattern CLASS_OR_SIGN = java.util.regex.Pattern.compile(
                "(?i)^(?<subject>[A-Z0-9][A-Z0-9-]*(?:\\s+(?:OF|IN)\\s+[A-Z0-9][A-Z0-9-]*)*(?:\\([^)]*\\))?)\\s+(?:IS\\s+)?(?<not>NOT\\s+)?"
                + "(?<test>NUMERIC|ALPHABETIC|POSITIVE|NEGATIVE|ZERO)$");

        private @Nullable String translateDisjunction(String text, ConditionContext context) {
            List<String> parts = splitTopLevel(text, "OR");
            List<String> translated = new ArrayList<>();
            for (String part : parts) {
                String value = translateConjunction(part, context);
                if (value == null) return null;
                translated.add(value);
            }
            return translated.size() == 1 ? translated.get(0) : "(" + String.join(" || ", translated) + ")";
        }

        private @Nullable String translateConjunction(String text, ConditionContext context) {
            List<String> parts = splitTopLevel(text, "AND");
            List<String> translated = new ArrayList<>();
            for (String part : parts) {
                String value = translateNegation(part.trim(), context);
                if (value == null) return null;
                translated.add(value);
            }
            return translated.size() == 1 ? translated.get(0) : "(" + String.join(" && ", translated) + ")";
        }

        private @Nullable String translateNegation(String text, ConditionContext context) {
            String upper = text.toUpperCase(Locale.ROOT);
            if (upper.startsWith("NOT ") && !OPERATOR_ONLY.matcher(text).matches()) {
                String inner = translateNegation(text.substring(4).trim(), context);
                return inner == null ? null : "!(" + inner + ")";
            }
            if (text.startsWith("(") && matchingParen(text, 0) == text.length() - 1) {
                String inner = translateDisjunction(text.substring(1, text.length() - 1).trim(), context);
                return inner == null ? null : "(" + inner + ")";
            }
            return translateSimpleCondition(text, context);
        }

        private @Nullable String translateSimpleCondition(String text, ConditionContext context) {
            java.util.regex.Matcher classOrSign = CLASS_OR_SIGN.matcher(text);
            if (classOrSign.matches()) {
                String test = classOrSignTest(classOrSign.group("subject"), classOrSign.group("test"), context);
                if (test == null) return null;
                return classOrSign.group("not") != null ? "!(" + test + ")" : test;
            }
            java.util.regex.Matcher relation = RELATION.matcher(text);
            if (relation.matches()) {
                String left = relation.group("left").trim();
                context.subject = left;
                context.operator = normalizeOperator(relation.group("op"));
                return relationTest(left, context.operator, relation.group("right").trim(), context);
            }
            java.util.regex.Matcher operatorOnly = OPERATOR_ONLY.matcher(text);
            if (operatorOnly.matches() && context.subject != null) {
                context.operator = normalizeOperator(operatorOnly.group("op"));
                return relationTest(context.subject, context.operator, operatorOnly.group("right").trim(), context);
            }
            String bare = text.trim();
            Optional<ResolvedCondition> level88 = currentModel == null ? Optional.empty()
                    : findCondition(currentModel, bare);
            if (level88.isPresent()) {
                return level88Test(level88.orElseThrow());
            }
            if (context.subject != null && context.operator != null && !bare.contains(" ")) {
                // Abbreviated combined relation: "A = 1 OR 2" -> the object 2 keeps subject and operator.
                return relationTest(context.subject, context.operator, bare, context);
            }
            return null;
        }

        private static boolean isQuoted(String operand) {
            return operand.startsWith("'") || operand.startsWith("\"");
        }

        private static String normalizeOperator(String op) {
            String o = op.toUpperCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            if (o.startsWith("NOT")) {
                String rest = o.substring(3).trim();
                if (rest.startsWith("=") || rest.startsWith("EQUAL")) return "!=";
                if (rest.startsWith(">") || rest.startsWith("GREATER")) return "<=";
                if (rest.startsWith("<") || rest.startsWith("LESS")) return ">=";
            }
            if (o.equals("<>")) return "!=";
            if (o.startsWith("GREATER") && o.contains("EQUAL")) return ">=";
            if (o.startsWith("LESS") && o.contains("EQUAL")) return "<=";
            if (o.startsWith("GREATER")) return ">";
            if (o.startsWith("LESS")) return "<";
            if (o.startsWith("EQUAL") || o.equals("=")) return "==";
            return o;
        }

        /** Java test for one relation, typed by its operands. */
        private @Nullable String relationTest(String left, String op, String right, ConditionContext context) {
            String leftType = operandType(left, context);
            String upperRight = right.toUpperCase(Locale.ROOT);
            String leftExpr = operandExpression(left, context);
            if (leftExpr == null) return null;
            if (FIGURATIVE_SPACE.contains(upperRight) || FIGURATIVE_ZERO.contains(upperRight)
                    || upperRight.equals("LOW-VALUE") || upperRight.equals("LOW-VALUES")
                    || upperRight.equals("HIGH-VALUE") || upperRight.equals("HIGH-VALUES")) {
                return figurativeTest(leftExpr, leftType, op, upperRight);
            }
            String rightType = operandType(right, context);
            String rightExpr = operandExpression(right, context);
            if (rightExpr == null || leftType == null || rightType == null) return null;
            if ("String".equals(leftType) || "String".equals(rightType)) {
                String l = "NUM".equals(leftType) ? quoted(left) : asText(leftExpr, leftType);
                String r = "NUM".equals(rightType) ? quoted(right.replace("+", "")) : asText(rightExpr, rightType);
                // COBOL compares alphanumerics padded with spaces to the longer operand.
                if (isQuoted(right) || "NUM".equals(rightType)) r = padLiteral(r, textLength(left));
                if (isQuoted(left) || "NUM".equals(leftType)) l = padLiteral(l, textLength(right));
                return switch (op) {
                    case "==" -> "java.util.Objects.equals(" + l + ", " + r + ")";
                    case "!=" -> "!java.util.Objects.equals(" + l + ", " + r + ")";
                    default -> "String.valueOf(" + l + ").compareTo(String.valueOf(" + r + ")) " + op + " 0";
                };
            }
            if ("BigDecimal".equals(leftType) || "BigDecimal".equals(rightType)
                    || "NUM".equals(rightType) && right.contains(".") || "NUM".equals(leftType) && left.contains(".")) {
                return asDecimal(leftExpr, leftType, left) + ".compareTo(" + asDecimal(rightExpr, rightType, right)
                        + ") " + op + " 0";
            }
            boolean bothItems = !"NUM".equals(leftType) && !"NUM".equals(rightType);
            if (bothItems && ("==".equals(op) || "!=".equals(op))) {
                // == between two boxed numbers compares references.
                return "Long.compare(" + leftExpr + ", " + rightExpr + ") " + op + " 0";
            }
            return leftExpr + " " + op + " " + rightExpr;
        }

        private String figurativeTest(String expr, @Nullable String type, String op, String figurative) {
            if (!FIGURATIVE_ZERO.contains(figurative) && type != null && !"String".equals(type)) {
                // A numeric Java value cannot hold spaces or low/high-values (invalid COBOL data).
                return UNRESOLVED_MARKER + figurative;
            }
            String test;
            if (FIGURATIVE_ZERO.contains(figurative)) {
                test = switch (type == null ? "String" : type) {
                    case "String" -> "(" + expr + " != null && " + expr + ".chars().allMatch(c -> c == '0'))";
                    case "BigDecimal" -> "(" + expr + ".signum() == 0)";
                    default -> "(Long.compare(" + expr + ", 0L) == 0)";
                };
            } else if (FIGURATIVE_SPACE.contains(figurative)) {
                test = "(" + expr + " == null || " + expr + ".isBlank())";
            } else if (figurative.startsWith("LOW")) {
                test = "(" + expr + " == null || " + expr + ".chars().allMatch(c -> c == 0))";
            } else {
                test = "(" + expr + " != null && " + expr + ".chars().allMatch(c -> c == 0xFF))";
            }
            return switch (op) {
                case "==" -> test;
                case "!=" -> "!" + test;
                default -> UNRESOLVED_MARKER + "FIGURATIVE-ORDERING";
            };
        }

        private @Nullable String classOrSignTest(String subject, String test, ConditionContext context) {
            String expr = operandExpression(subject, context);
            String type = operandType(subject, context);
            if (expr == null || type == null) return null;
            return switch (test.toUpperCase(Locale.ROOT)) {
                case "NUMERIC" -> "String".equals(type)
                        ? "(" + expr + " != null && !" + expr + ".isEmpty() && " + expr
                        + ".chars().allMatch(Character::isDigit))"
                        : "true";
                case "ALPHABETIC" -> "String".equals(type)
                        ? "(" + expr + " != null && " + expr + ".chars().allMatch(c -> Character.isLetter(c) || c == ' '))"
                        : "false";
                case "POSITIVE" -> signTest(expr, type, "> 0");
                case "NEGATIVE" -> signTest(expr, type, "< 0");
                default -> signTest(expr, type, "== 0");
            };
        }

        private @Nullable String signTest(String expr, String type, String comparison) {
            return switch (type) {
                case "BigDecimal" -> expr + ".signum() " + comparison;
                case "Integer", "Long", "NUM" -> "Long.compare(" + expr + ", 0L) " + comparison;
                default -> null;
            };
        }

        /** Level-88 condition name: the parent item holds one of the condition's values or ranges. */
        private @Nullable String level88Test(ResolvedCondition resolved) {
            CobolDataItem parent = resolved.parent();
            String parentExpr = toJavaIdentifierRef(parent.name());
            if (parentExpr.contains(UNRESOLVED_MARKER)) return null;
            String type = parent.picture() == null ? "String" : parent.javaType();
            List<String> tests = new ArrayList<>();
            for (Level88Value value : resolved.condition().values()) {
                Optional<String> low = CobolDataVerbValueResolver.renderCobolValue(value.value(), parent);
                if (low.isEmpty()) return null;
                if (!value.isRange()) {
                    tests.add("String".equals(type)
                            ? "java.util.Objects.equals(" + parentExpr + ", "
                            + padLiteral(low.orElseThrow(), textLength(parent.name())) + ")"
                            : "BigDecimal".equals(type)
                            ? "(" + parentExpr + " != null && " + parentExpr + ".compareTo(" + low.orElseThrow() + ") == 0)"
                            : "java.util.Objects.equals(" + parentExpr + ", " + low.orElseThrow() + ")");
                    continue;
                }
                Optional<String> high = CobolDataVerbValueResolver.renderCobolValue(value.through(), parent);
                if (high.isEmpty()) return null;
                tests.add("(" + parentExpr + " != null && " + parentExpr + ".compareTo(" + low.orElseThrow()
                        + ") >= 0 && " + parentExpr + ".compareTo(" + high.orElseThrow() + ") <= 0)");
            }
            return tests.size() == 1 ? tests.get(0) : "(" + String.join(" || ", tests) + ")";
        }

        /** "String", "Integer", "Long", "BigDecimal" for data items; "NUM" for numeric literals. */
        private @Nullable String operandType(String operand, ConditionContext context) {
            String t = operand.trim();
            if (isQuoted(t)) return "String";
            if (t.matches("[+-]?[0-9]*\\.?[0-9]+")) return "NUM";
            String upper = t.toUpperCase(Locale.ROOT);
            if (context.aliases.containsKey(upper) || variableAliases.containsKey(upper)) return "Integer";
            return dataTypes.get(toPascal(stripSubscripts(t)));
        }

        private @Nullable String operandExpression(String operand, ConditionContext context) {
            String t = unqualify(operand);
            if (t == null) {
                return null;
            }
            if (isQuoted(t) || t.matches("[+-]?[0-9]*\\.?[0-9]+")) {
                return toJavaExpression(t);
            }
            if (!t.matches("[A-Za-z0-9][A-Za-z0-9-]*(?:\\([^()]*\\))?")) {
                return null; // arithmetic expressions, functions, reference modification ranges
            }
            String expr = toJavaIdentifierRef(t, context.aliases);
            return expr.contains(UNRESOLVED_MARKER) ? null : expr;
        }

        private static String asText(String expr, String type) {
            return "String".equals(type) ? expr : "String.valueOf(" + expr + ")";
        }

        private static String asDecimal(String expr, String type, String raw) {
            if ("NUM".equals(type)) return "new java.math.BigDecimal(\"" + raw.replace("+", "") + "\")";
            return "BigDecimal".equals(type) ? expr : "java.math.BigDecimal.valueOf(" + expr + ")";
        }

        /** Splits on a logical keyword outside quotes and parentheses. */
        private static List<String> splitTopLevel(String text, String keyword) {
            List<String> parts = new ArrayList<>();
            int depth = 0;
            char quote = 0;
            int last = 0;
            String upper = text.toUpperCase(Locale.ROOT);
            String token = " " + keyword + " ";
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (quote != 0) {
                    if (c == quote) quote = 0;
                    continue;
                }
                if (c == '\'' || c == '"') {
                    quote = c;
                } else if (c == '(') {
                    depth++;
                } else if (c == ')') {
                    depth--;
                } else if (depth == 0 && upper.startsWith(token, i)) {
                    parts.add(text.substring(last, i).trim());
                    last = i + token.length();
                    i = last - 1;
                }
            }
            parts.add(text.substring(last).trim());
            return parts;
        }

        private static int matchingParen(String text, int open) {
            int depth = 0;
            char quote = 0;
            for (int i = open; i < text.length(); i++) {
                char c = text.charAt(i);
                if (quote != 0) {
                    if (c == quote) quote = 0;
                    continue;
                }
                if (c == '\'' || c == '"') quote = c;
                else if (c == '(') depth++;
                else if (c == ')' && --depth == 0) return i;
            }
            return -1;
        }

        /** True when a condition operand is an alphanumeric literal or a String data item. */
        private boolean isStringOperand(String cobolOperand) {
            String t = cobolOperand == null ? "" : cobolOperand.trim();
            if (t.startsWith("'") || t.startsWith("\"")) {
                return true;
            }
            return "String".equals(dataTypes.get(toPascal(stripSubscripts(t))));
        }

        private String toJavaIdentifierRef(String ident) {
            return toJavaIdentifierRef(ident, Map.of());
        }

        private String toJavaIdentifierRef(String ident, Map<String, String> aliases) {
            if (ident == null || ident.isBlank()) return ident;
            // If it's a pure number or quoted string, delegate to toJavaExpression
            String t = ident.trim();
            if (t.matches("[0-9]+") || t.startsWith("\"") || t.startsWith("'")) {
                return toJavaExpression(t);
            }
            String alias = aliases.get(t.toUpperCase(Locale.ROOT));
            if (alias != null) {
                return alias;
            }
            String scopedAlias = variableAliases.get(t.toUpperCase(Locale.ROOT));
            if (scopedAlias != null) {
                return scopedAlias;
            }
            return getterCall(t);
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

        private String toSetter(String qualifiedName) {
            String cobolName = unqualify(qualifiedName);
            if (cobolName == null) {
                return UNRESOLVED_MARKER + qualifiedName.trim().split("\\s", 2)[0];
            }
            String pascal = toPascal(stripSubscripts(cobolName));
            if (!knownDataNames.isEmpty() && !knownDataNames.contains(pascal)
                    || tableSizes.containsKey(pascal) || !stripSubscripts(cobolName).equals(cobolName.trim())) {
                // Tables need setterCall (subscript); a reference modification is not a whole-item write.
                return UNRESOLVED_MARKER + cobolName;
            }
            return "set" + pascal;
        }

        /** {@code target.setX(value)}, or {@code target.setX(subscript, value)} for an OCCURS item. */
        private String setterCall(String targetVar, String qualifiedName, String value) {
            String cobolName = unqualify(qualifiedName);
            if (cobolName == null) {
                return targetVar + "." + UNRESOLVED_MARKER + qualifiedName.trim().split("\\s", 2)[0] + "(" + value + ")";
            }
            String pascal = toPascal(stripSubscripts(cobolName));
            if (tableSizes.containsKey(pascal)) {
                String subscript = subscript(cobolName);
                return targetVar + ".set" + pascal + "("
                        + (subscript == null ? UNRESOLVED_MARKER + cobolName : subscript) + ", " + value + ")";
            }
            String[] slice = stripSubscripts(cobolName).equals(cobolName.trim()) ? null : referenceModification(cobolName);
            if (slice != null) {
                // Replace only the referenced characters of the receiving item.
                String current = getterCall(slice[0]);
                String width = slice[3];
                String fitted = width.matches("[0-9]+")
                        ? "String.format(\"%-" + width + "." + width + "s\", " + value + ")"
                        : "String.format(\"%-\" + (" + width + ") + \".\" + (" + width + ") + \"s\", " + value + ")";
                return targetVar + ".set" + pascal + "(" + current + ".substring(0, " + slice[1] + ") + " + fitted
                        + " + " + current + ".substring(" + slice[2] + "))";
            }
            return targetVar + "." + toSetter(cobolName) + "(" + value + ")";
        }

        /**
         * Getter for a data reference: from the output DTO once the program has written the item,
         * from the input otherwise; OCCURS items take their subscript. Reference modification and
         * subscripts on tables this generation does not model stay unresolved.
         */
        private String getterCall(String qualifiedReference) {
            if (cicsProgram) {
                String register = cicsRegister(qualifiedReference.trim());
                if (register != null) {
                    return register;
                }
            }
            String reference = unqualify(qualifiedReference);
            if (reference == null) {
                return UNRESOLVED_MARKER + qualifiedReference.trim().split("\\s", 2)[0];
            }
            String upper = stripSubscripts(reference).toUpperCase(Locale.ROOT);
            String pascal = toPascal(stripSubscripts(reference));
            if (!knownDataNames.isEmpty() && !knownDataNames.contains(pascal)) {
                return UNRESOLVED_MARKER + reference;
            }
            String owner = assignedVariables.contains(upper) ? currentOutputVar : "input";
            if (tableSizes.containsKey(pascal)) {
                String subscript = subscript(reference);
                return subscript == null ? UNRESOLVED_MARKER + reference
                        : owner + ".get" + pascal + "(" + subscript + ")";
            }
            if (!stripSubscripts(reference).equals(reference.trim())) {
                String[] slice = referenceModification(reference);
                return slice == null ? UNRESOLVED_MARKER + reference
                        : owner + ".get" + pascal + "().substring(" + slice[1] + ", " + slice[2] + ")";
            }
            return owner + ".get" + pascal + "()";
        }

        /** Characters of an alphanumeric item (elementary, group record or view), 0 when unknown or numeric. */
        private int textLength(String cobolName) {
            if (currentModel == null || cobolName == null) {
                return 0;
            }
            String name = stripSubscripts(cobolName.trim()).toUpperCase(Locale.ROOT);
            Optional<CobolRecordLayout> group = currentModel.findComposableLayout(name);
            if (group.isPresent()) {
                return group.orElseThrow().length();
            }
            CobolRedefinesView view = currentModel.getRedefinesViews().get(name);
            if (view != null && view.supported() && view.picType() == null) {
                return view.length();
            }
            return findDataItem(currentModel, name)
                    .filter(item -> item.picType() != null
                            && item.picType().category() != org.modernash.renovatio.cobol.runtime.PicType.Category.NUMERIC)
                    .map(item -> item.picType().digits())
                    .orElse(0);
        }

        /** A Java string literal right-padded with spaces to {@code length} (unchanged if not a literal). */
        private static String padLiteral(String javaLiteral, int length) {
            if (length <= 0 || !javaLiteral.startsWith("\"") || !javaLiteral.endsWith("\"")) {
                return javaLiteral;
            }
            String content = javaLiteral.substring(1, javaLiteral.length() - 1);
            int visible = content.replace("\\\\", "\\").replace("\\\"", "\"").length();
            return visible >= length ? javaLiteral
                    : "\"" + content + " ".repeat(length - visible) + "\"";
        }

        /** Reference modification {@code NAME(start:length)} of an alphanumeric item: [from, to) bounds. */
        private @Nullable String[] referenceModification(String reference) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("^([A-Za-z][A-Za-z0-9-]*)\\(\\s*([^:()]+?)\\s*:\\s*([^:()]*?)\\s*\\)$")
                    .matcher(reference.trim());
            if (!matcher.matches() || tableSizes.containsKey(toPascal(matcher.group(1)))) {
                return null;
            }
            String name = matcher.group(1);
            int length = textLength(name);
            if (length <= 0 || !"String".equals(dataTypes.get(toPascal(name)))) {
                return null;
            }
            String start = intExpression(matcher.group(2));
            String size = matcher.group(3).isBlank() ? null : intExpression(matcher.group(3));
            if (start == null || !matcher.group(3).isBlank() && size == null) {
                return null;
            }
            String from = start.matches("[0-9]+") ? String.valueOf(Integer.parseInt(start) - 1) : "(" + start + ") - 1";
            String to = size == null ? String.valueOf(length)
                    : start.matches("[0-9]+") && size.matches("[0-9]+")
                    ? String.valueOf(Integer.parseInt(start) - 1 + Integer.parseInt(size))
                    : from + " + " + size;
            String width = size != null ? size
                    : from.matches("[0-9]+") ? String.valueOf(length - Integer.parseInt(from)) : length + " - (" + from + ")";
            return new String[] {name, from, to, width};
        }

        /** Java int expression of a COBOL integer operand: literal, numeric data item or loop variable. */
        private @Nullable String intExpression(String operand) {
            String inner = operand.trim();
            if (inner.matches("[0-9]+")) {
                return inner;
            }
            if (!inner.matches("[A-Za-z][A-Za-z0-9-]*")) {
                return null;
            }
            String alias = variableAliases.get(inner.toUpperCase(Locale.ROOT));
            if (alias != null) {
                return alias;
            }
            String type = dataTypes.get(toPascal(inner));
            String value = getterCall(inner);
            if (value.contains(UNRESOLVED_MARKER) || type == null || "String".equals(type)) {
                return null;
            }
            return "Integer".equals(type) ? value : value + ".intValue()";
        }

        /** Java int expression of a single COBOL subscript (literal, data item or loop variable), else null. */
        private @Nullable String subscript(String reference) {
            String t = reference.trim();
            int open = t.indexOf('(');
            if (open < 0 || !t.endsWith(")")) {
                return null;
            }
            return intExpression(t.substring(open + 1, t.length() - 1)); // null for several subscripts or arithmetic
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
            if (trimmed.matches("[+-]?[0-9]*\\.?[0-9]+")) {
                return trimmed.startsWith("+") ? trimmed.substring(1) : trimmed;
            }
            if (trimmed.equalsIgnoreCase("TRUE") || trimmed.equalsIgnoreCase("FALSE")) {
                return trimmed.toLowerCase(Locale.ROOT);
            }
            String alias = variableAliases.get(trimmed.toUpperCase(Locale.ROOT));
            if (alias != null) {
                return alias;
            }
            return getterCall(trimmed);
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
            if (endsFromParagraphs) {
                StringBuilder builder = new StringBuilder("{\n");
                builder.append(String.format(Locale.ROOT, "    %s %s = new %s();\n", dtoType, targetVar, dtoType));
                builder.append("    try {\n");
                for (String statement : statements) {
                    builder.append("        ").append(statement).append('\n');
                }
                builder.append("    } catch (").append(dtoType).append(".ProgramExit ended) {\n");
                builder.append("        // the run ended in a performed paragraph (GOBACK, STOP RUN, XCTL or RETURN)\n");
                builder.append("    }\n");
                builder.append("    return ").append(targetVar).append(";\n}");
                return builder.toString();
            }
            StringBuilder builder = new StringBuilder();
            builder.append("{\n");
            builder.append(String.format(Locale.ROOT, "    %s %s = new %s();\n", dtoType, targetVar, dtoType));
            String lastMeaningful = "";
            for (String statement : statements) {
                builder.append("    ").append(statement).append('\n');
                if (!statement.trim().isEmpty()) {
                    lastMeaningful = statement.trim();
                }
            }
            if (!lastMeaningful.startsWith("return ")) {
                builder.append(String.format(Locale.ROOT, "    return %s;\n", targetVar));
            }
            builder.append("}");
            return builder.toString();
        }

        private String simpleName(String fqOrSimple) {
            int idx = fqOrSimple.lastIndexOf('.');
            return idx >= 0 ? fqOrSimple.substring(idx + 1) : fqOrSimple;
        }
    }
}
