package org.shark.renovatio.cobol.recipes;

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
import org.shark.renovatio.cobol.ir.annotated.AnnotatedCobolContext;
import org.shark.renovatio.cobol.ir.model.*;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationApplicationOutcome;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationApplicator;
import org.shark.renovatio.cobol.recipes.annotate.AnnotationOutcomeKey;
import org.shark.renovatio.cobol.recipes.annotate.DroppedAnnotation;
import org.shark.renovatio.semantic.ir.SemanticProgram;

import java.util.*;

public class PopulateCobolProcessRecipe extends Recipe {

    private record ResolvedCondition(CobolDataItem parent, Level88Condition condition) { }

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

            // Re-render with the chosen variable name
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
                        || simple.kind() == SimpleStatement.Kind.STOP_RUN);
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
                        return List.of("return;");
                    }
                    return List.of(String.format(Locale.ROOT, "return %s;", targetVar));
                case DISPLAY:
                    return List.of(String.format(Locale.ROOT, "System.out.println(%s);",
                            renderDisplayArguments(simple.text())));
                case UNTRANSLATED:
                default:
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
                parts.add(toJavaExpression(token));
            }
            return parts.isEmpty() ? "" : String.join(" + ", parts);
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
                lines.add(String.format(Locale.ROOT, "%s.%s(%s);",
                        targetVar, toSetter(target), initialValue.orElseThrow()));
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

        private Set<String> knownDataNamesOf(CobolIntermediateModel model) {
            Set<String> names = new LinkedHashSet<>();
            for (CobolDataItem item : model.getDataItems()) {
                names.add(toPascal(item.name()));
            }
            return names;
        }

        private Set<String> computeAssignedVariables(CobolIntermediateModel model) {
            Set<String> assigned = new LinkedHashSet<>();
            for (CobolParagraph paragraph : model.getParagraphs().values()) {
                collectAssignedStatements(paragraph.statements(), assigned, model);
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

        /** Removes trailing COBOL subscripts (e.g. {@code (1)}, {@code (1:2)}) from a data reference. */
        private static String stripSubscripts(String cobolRef) {
            if (cobolRef == null) {
                return null;
            }
            String s = cobolRef.trim();
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
                    if (terminatesFlow(stmt)) {
                        break;
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
            return translateCondition(condition, Map.of());
        }

        private String translateCondition(String condition, Map<String, String> aliases) {
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
                String leftExpr = toJavaIdentifierRef(left, aliases);
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
            String upper = stripSubscripts(t).toUpperCase(Locale.ROOT);
            String pascal = toPascal(stripSubscripts(t));
            if (!knownDataNames.isEmpty() && !knownDataNames.contains(pascal)) {
                return UNRESOLVED_MARKER + t;
            }
            if (assignedVariables.contains(upper)) {
                return currentOutputVar + ".get" + pascal + "()";
            }
            // Map COBOL variable name to getter on input
            return String.format(java.util.Locale.ROOT, "input.get%s()", pascal);
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
            return "set" + toPascal(stripSubscripts(cobolName));
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
            String alias = variableAliases.get(trimmed.toUpperCase(Locale.ROOT));
            if (alias != null) {
                return alias;
            }
            String upper = stripSubscripts(trimmed).toUpperCase(Locale.ROOT);
            String pascal = toPascal(stripSubscripts(trimmed));
            if (!knownDataNames.isEmpty() && !knownDataNames.contains(pascal)) {
                return UNRESOLVED_MARKER + trimmed;
            }
            if (assignedVariables.contains(upper)) {
                return currentOutputVar + ".get" + pascal + "()";
            }
            return String.format(Locale.ROOT, "input.get%s()", pascal);
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
