package org.modernash.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.modernash.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.modernash.renovatio.cobol.ir.parser.SimpleCobolIrParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PopulateCobolProcessRecipeTest {

    private static final String COBOL_SAMPLE = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. SAMPLE1.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(30).
            01 CUSTOMER-RATING PIC 9(2).
            PROCEDURE DIVISION.
            MAIN-PARA.
                PERFORM PREP-PARA.
                MOVE 'JOHN' TO CUSTOMER-NAME.
                IF CUSTOMER-RATING > 80
                    MOVE 'VIP' TO CUSTOMER-NAME
                ELSE
                    MOVE 'STANDARD' TO CUSTOMER-NAME
                END-IF.
                EVALUATE CUSTOMER-RATING
                    WHEN 1
                        MOVE 'BRONZE' TO CUSTOMER-NAME
                    WHEN OTHER
                        MOVE 'PLATINUM' TO CUSTOMER-NAME
                END-EVALUATE.
                GOBACK.
            END-PARA.
            PREP-PARA.
                MOVE 'INIT' TO CUSTOMER-NAME.
            END-PARA.
            """;

    @Test
    void shouldPopulateProcessMethodWithCobolLogic() {
        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(COBOL_SAMPLE);

        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;

        JavaParser javaParser = JavaParser.fromJavaVersion().build();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);

        List<org.openrewrite.SourceFile> sources = javaParser.parse(ctx, javaSource)
                .collect(java.util.stream.Collectors.toList());
        PopulateCobolProcessRecipe recipe = new PopulateCobolProcessRecipe();

        // Adapt to OpenRewrite LargeSourceSet API
        org.openrewrite.LargeSourceSet lss = new org.openrewrite.internal.InMemoryLargeSourceSet(sources);
        var run = recipe.run(lss, ctx);
        List<Result> results = run.getChangeset().getAllResults();

        assertThat(results).hasSize(1);
        String updated = results.get(0).getAfter().printAll();
        assertThat(updated).contains("output.setCustomerName(\"JOHN\");");
        assertThat(updated).contains("if (input.getCustomerRating() > 80)");
        assertThat(updated).contains("switch (input.getCustomerRating()) {");
        assertThat(updated).contains("case 1 -> {");
        assertThat(updated).contains("output.setCustomerName(\"BRONZE\");");
        assertThat(updated).contains("output.setCustomerName(\"PLATINUM\");");
        assertThat(updated).doesNotContain("TODO");
    }

    @Test
    void shouldExtractPerformParagraphsIntoMethods() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE2.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM PREP-PARA.
                    MOVE 'READY' TO CUSTOMER-NAME.
                    GOBACK.
                PREP-PARA.
                    MOVE 'INIT' TO CUSTOMER-NAME.
                    EXIT.
                """;

        SimpleCobolIrParser parser = new SimpleCobolIrParser();
        CobolIntermediateModel model = parser.parse(cobol);

        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;

        JavaParser javaParser = JavaParser.fromJavaVersion().build();
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);

        List<org.openrewrite.SourceFile> sources = javaParser.parse(ctx, javaSource)
                .collect(java.util.stream.Collectors.toList());
        PopulateCobolProcessRecipe recipe = new PopulateCobolProcessRecipe();

        org.openrewrite.LargeSourceSet lss = new org.openrewrite.internal.InMemoryLargeSourceSet(sources);
        var run = recipe.run(lss, ctx);
        List<Result> results = run.getChangeset().getAllResults();

        assertThat(results).hasSize(1);
        String updated = results.get(0).getAfter().printAll();
        assertThat(updated).contains("performPrepPara(input, output);");
        assertThat(updated).contains("out.setCustomerName(\"INIT\");");
        assertThat(updated).contains("output.setCustomerName(\"READY\");");
        assertThat(updated).contains("private void performPrepPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("@GeneratedFrom(paragraph = \"PREP-PARA\", lines = \"");
        assertThat(updated).contains("@interface GeneratedFrom {");
        assertThat(updated).doesNotContain("// PERFORM");
        assertThat(updated).doesNotContain("TODO");
    }

    @Test
    void shouldExtractPerformThruRangeAsOrderedCalls() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE4.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM FIRST-PARA THRU THIRD-PARA.
                    MOVE 'READY' TO CUSTOMER-NAME.
                    GOBACK.
                FIRST-PARA.
                    MOVE '1' TO CUSTOMER-NAME.
                SECOND-PARA.
                    MOVE '2' TO CUSTOMER-NAME.
                THIRD-PARA.
                    MOVE '3' TO CUSTOMER-NAME.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("performFirstPara(input, output);");
        assertThat(updated).contains("performSecondPara(input, output);");
        assertThat(updated).contains("performThirdPara(input, output);");
        assertThat(updated.indexOf("performFirstPara(input, output);"))
                .isLessThan(updated.indexOf("performSecondPara(input, output);"));
        assertThat(updated.indexOf("performSecondPara(input, output);"))
                .isLessThan(updated.indexOf("performThirdPara(input, output);"));
        assertThat(updated).contains("private void performFirstPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("private void performSecondPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("private void performThirdPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("out.setCustomerName(\"1\");");
        assertThat(updated).contains("out.setCustomerName(\"2\");");
        assertThat(updated).contains("out.setCustomerName(\"3\");");
    }

    @Test
    void shouldWrapPerformTimesInLoop() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE5.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM COUNT-PARA 3 TIMES.
                    STOP RUN.
                COUNT-PARA.
                    MOVE 'X' TO CUSTOMER-NAME.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("for (int i = 0; i < 3; i++) {");
        assertThat(updated).contains("performCountPara(input, output);");
        assertThat(updated).contains("private void performCountPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("out.setCustomerName(\"X\");");
    }

    @Test
    void shouldWrapPerformUntilInWhileLoop() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE6.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                01 CUSTOMER-RATING PIC 9(2).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM COUNT-PARA UNTIL CUSTOMER-RATING > 80.
                    STOP RUN.
                COUNT-PARA.
                    MOVE 'X' TO CUSTOMER-NAME.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("while (!(input.getCustomerRating() > 80)) {");
        assertThat(updated).contains("performCountPara(input, output);");
        assertThat(updated).contains("private void performCountPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("out.setCustomerName(\"X\");");
    }

    @Test
    void shouldWrapInlinePerformVaryingInLoop() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE7.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-IDX PIC 9(2).
                01 CUSTOMER-RATING PIC 9(2).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM VARYING WS-IDX FROM 1 BY 1 UNTIL WS-IDX > 3
                        MOVE WS-IDX TO CUSTOMER-RATING
                    END-PERFORM.
                    STOP RUN.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("for (int wsIdx = 1; !(wsIdx > 3); wsIdx += 1) {");
        assertThat(updated).contains("output.setCustomerRating(wsIdx);");
        assertThat(updated).contains("return output;");
        assertThat(updated).doesNotContain("input.getWsIdx()");
    }

    @Test
    void shouldRenderPerformVaryingAfterAsNestedLoops() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE7B.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-I PIC 9(2).
                01 WS-J PIC 9(2).
                01 CUSTOMER-RATING PIC 9(2).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM VARYING WS-I FROM 1 BY 1 UNTIL WS-I > 2 AFTER WS-J FROM 1 BY 1 UNTIL WS-J > 3
                        MOVE WS-J TO CUSTOMER-RATING
                    END-PERFORM.
                    STOP RUN.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("for (int wsI = 1; !(wsI > 2); wsI += 1) {");
        assertThat(updated).contains("for (int wsJ = 1; !(wsJ > 3); wsJ += 1) {");
        assertThat(updated).contains("output.setCustomerRating(wsJ);");
        assertThat(updated).doesNotContain("input.getWsJ()");
    }

    @Test
    void shouldGuardRecursivePerformCycles() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE8.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    PERFORM A-PARA.
                    STOP RUN.
                A-PARA.
                    PERFORM B-PARA.
                B-PARA.
                    PERFORM A-PARA.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("performAPara(input, output);");
        assertThat(updated).contains("private void performAPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("private void performBPara(SampleDto input, SampleDto out) {");
        assertThat(updated).contains("// COBOL not translated: PERFORM cycle (B-PARA -> A-PARA)");
        assertThat(updated).contains("// COBOL not translated: PERFORM cycle (A-PARA -> B-PARA)");
    }

    @Test
    void shouldProduceByteStableOutputAcrossIndependentRuns() {
        String first = applyRecipe(COBOL_SAMPLE);
        String second = applyRecipe(COBOL_SAMPLE);

        assertThat(second).isEqualTo(first);
        assertThat(sha256(second)).isEqualTo(sha256(first));
    }

    @Test
    void shouldRenderDisplayContinueGobackAndSurfaceUntranslatedStatements() {
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. SAMPLE3.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 CUSTOMER-NAME PIC X(30).
                01 CUSTOMER-RATING PIC 9(2).
                01 CUSTOMER-STATUS PIC X.
                   88 CUSTOMER-READY VALUE 'Y'.
                PROCEDURE DIVISION.
                MAIN-PARA.
                    DISPLAY 'RATING ' CUSTOMER-RATING.
                    INITIALIZE CUSTOMER-NAME.
                    SET CUSTOMER-READY TO TRUE.
                    SET CUSTOMER-READY TO FALSE.
                    IF CUSTOMER-RATING > 80
                        GOBACK
                    ELSE
                        CONTINUE
                    END-IF.
                    MOVE 'DONE' TO CUSTOMER-NAME.
                    STOP RUN.
                """;

        String updated = applyRecipe(cobol);

        assertThat(updated).contains("System.out.println(\"RATING \" + input.getCustomerRating());");
        assertThat(updated).contains("output.setCustomerName(\" \".repeat(30));");
        assertThat(updated).contains("output.setCustomerStatus(\"Y\");");
        assertThat(updated).contains("output.setCustomerStatus(\" \");");
        assertThat(updated).contains("; // CONTINUE");
        assertThat(updated).contains("return output;");
        assertThat(updated).doesNotContain("// Unhandled COBOL statement");
        // GOBACK in the then-branch must not leave a statement after the return in that branch,
        // and buildBody must not append a second return after STOP RUN.
        assertThat(countOccurrences(updated, "return output;")).isEqualTo(2);
        assertThat(updated).contains("output.setCustomerName(\"DONE\");");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    @Test
    void productionBoundaryShouldContainNoNetworkOrLlmDependency() throws IOException {
        Path moduleRoot = locateModuleRoot();
        Path annotationsRoot = moduleRoot.resolveSibling("renovatio-cobol-annotations");
        String productionBoundary = productionBoundary(moduleRoot) + "\n"
                + productionBoundary(annotationsRoot);

        String boundary = productionBoundary.toLowerCase(Locale.ROOT);
        assertThat(boundary).doesNotContain(
                "org.modernash.renovatio.provider",
                "org.modernash.renovatio.llm",
                ".prompt.",
                "promptcatalog",
                "java.net.",
                "java.net.http",
                "okhttp",
                "retrofit",
                "anthropic",
                "openai",
                "bedrock",
                "gemini",
                "prompt catalog",
                "api key",
                "credential");
    }

    private static String productionBoundary(Path moduleRoot) throws IOException {
        try (Stream<Path> paths = Files.walk(moduleRoot.resolve("src/main"))) {
            return paths.filter(Files::isRegularFile)
                    .sorted()
                    .map(path -> inspectableProductionEntry(moduleRoot, path))
                    .reduce("", (left, right) -> left + "\n" + right);
        }
    }

    private static String inspectableProductionEntry(Path moduleRoot, Path path) {
        String relativePath = moduleRoot.relativize(path).toString().replace('\\', '/');
        String extension = extensionOf(path.getFileName().toString());
        Set<String> textExtensions = Set.of(
                "java", "json", "yaml", "yml", "xml", "properties", "txt", "md", "conf", "cfg");
        return relativePath + (textExtensions.contains(extension) ? "\n" + readUtf8(path) : "");
    }

    private static String extensionOf(String fileName) {
        int separator = fileName.lastIndexOf('.');
        return separator < 0 ? "" : fileName.substring(separator + 1).toLowerCase(Locale.ROOT);
    }

    private String applyRecipe(String cobol) {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(cobol);
        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;

        JavaParser parser = JavaParser.fromJavaVersion().build();
        ExecutionContext context = new InMemoryExecutionContext(Throwable::printStackTrace);
        context.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);
        List<org.openrewrite.SourceFile> sources = parser.parse(context, javaSource).toList();
        var run = new PopulateCobolProcessRecipe().run(
                new org.openrewrite.internal.InMemoryLargeSourceSet(sources), context);

        return run.getChangeset().getAllResults().get(0).getAfter().printAll();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private static Path locateModuleRoot() {
        Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if (Files.isRegularFile(workingDirectory.resolve("src/main/java/org/modernash/renovatio/cobol/recipes/PopulateCobolProcessRecipe.java"))) {
            return workingDirectory;
        }
        Path childModule = workingDirectory.resolve("cobol-openrewrite-recipes");
        if (Files.isRegularFile(childModule.resolve("src/main/java/org/modernash/renovatio/cobol/recipes/PopulateCobolProcessRecipe.java"))) {
            return childModule;
        }
        throw new IllegalStateException("Cannot locate cobol-openrewrite-recipes module from " + workingDirectory);
    }

    private static String readUtf8(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read " + path, exception);
        }
    }

    private static String populate(String cobol) {
        CobolIntermediateModel model = new SimpleCobolIrParser().parse(cobol);
        String javaSource = """
                package sample;
                public class SampleService {
                    public SampleDto process(SampleDto input) {
                        // TODO: Implement COBOL business logic
                        SampleDto output = new SampleDto();
                        return output;
                    }
                }
                """;
        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        ctx.putMessage(PopulateCobolProcessRecipe.CONTEXT_KEY, model);
        List<org.openrewrite.SourceFile> sources = JavaParser.fromJavaVersion().build().parse(ctx, javaSource)
                .collect(java.util.stream.Collectors.toList());
        var run = new PopulateCobolProcessRecipe()
                .run(new org.openrewrite.internal.InMemoryLargeSourceSet(sources), ctx);
        return run.getChangeset().getAllResults().get(0).getAfter().printAll();
    }

    @Test
    void shouldCompareAlphanumericOperandsByValue() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. EQ01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-STATUS PIC X(2).
                01 WS-COUNT PIC 9(2).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    IF WS-STATUS = '00'
                        MOVE 1 TO WS-COUNT
                    END-IF
                    IF WS-STATUS NOT = 10
                        MOVE 2 TO WS-COUNT
                    END-IF
                    GOBACK.
                """);

        assertThat(updated).contains("if (java.util.Objects.equals(input.getWsStatus(), \"00\")) {");
        assertThat(updated).contains("if (!java.util.Objects.equals(input.getWsStatus(), \"10\")) {");
        assertThat(updated).doesNotContain("== \"00\"");
    }

    @Test
    void shouldConvertMoveOperandsToTheReceivingJavaType() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. MV01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-AMOUNT PIC S9(7)V99.
                01 WS-UNITS PIC 9(5).
                01 WS-TEXT PIC X(5).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    MOVE WS-AMOUNT TO WS-UNITS.
                    MOVE WS-UNITS TO WS-AMOUNT.
                    MOVE WS-UNITS TO WS-TEXT.
                    MOVE WS-TEXT TO WS-UNITS.
                    MOVE -1025.50 TO WS-AMOUNT.
                    MOVE 12.9 TO WS-UNITS.
                    MOVE SPACES TO WS-TEXT.
                    MOVE ZEROS TO WS-AMOUNT.
                    GOBACK.
                """);

        assertThat(updated).contains("output.setWsUnits(output.getWsAmount().intValue());");
        assertThat(updated).contains("output.setWsAmount(java.math.BigDecimal.valueOf(output.getWsUnits()));");
        assertThat(updated).contains("output.setWsText(String.valueOf(output.getWsUnits()));");
        assertThat(updated).contains("output.setWsUnits(Integer.valueOf(output.getWsText().trim()));");
        assertThat(updated).contains("output.setWsAmount(new java.math.BigDecimal(\"-1025.50\"));");
        assertThat(updated).contains("output.setWsUnits(12);");
        assertThat(updated).contains("output.setWsText(\"     \");");
        assertThat(updated).contains("output.setWsAmount(java.math.BigDecimal.ZERO);");
    }

    @Test
    void shouldTranslateCompoundAbbreviatedAndLevel88Conditions() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COND01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-STATUS PIC X(2).
                01 WS-COUNT PIC 9(2).
                01 WS-AMOUNT PIC S9(5)V99.
                01 WS-RESULT PIC S9(9) COMP.
                   88 APPL-AOK VALUE 0.
                   88 APPL-EOF VALUE 16.
                01 WS-LESS-AMT PIC 9(3).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    IF WS-STATUS NOT = '00' AND WS-STATUS NOT = '10'
                        MOVE 1 TO WS-COUNT
                    END-IF
                    IF WS-COUNT = 1 OR 2 OR 3
                        MOVE 2 TO WS-COUNT
                    END-IF
                    IF APPL-AOK
                        MOVE 3 TO WS-COUNT
                    END-IF
                    IF WS-AMOUNT > 100.50
                        MOVE 4 TO WS-COUNT
                    END-IF
                    IF WS-STATUS = SPACES
                        MOVE 5 TO WS-COUNT
                    END-IF
                    IF WS-LESS-AMT GREATER THAN 5
                        MOVE 6 TO WS-COUNT
                    END-IF
                    GOBACK.
                """);

        assertThat(updated).contains("if ((!java.util.Objects.equals(input.getWsStatus(), \"00\")"
                + " && !java.util.Objects.equals(input.getWsStatus(), \"10\"))) {");
        assertThat(updated).contains("if ((output.getWsCount() == 1"
                + " || output.getWsCount() == 2 || output.getWsCount() == 3)) {");
        assertThat(updated).contains("if (java.util.Objects.equals(input.getWsResult(), 0)) {");
        assertThat(updated).contains("if (input.getWsAmount().compareTo(new java.math.BigDecimal(\"100.50\")) > 0) {");
        assertThat(updated).contains("if ((input.getWsStatus() == null || input.getWsStatus().isBlank())) {");
        assertThat(updated).contains("if (input.getWsLessAmt() > 5) {");
        assertThat(updated).doesNotContain("not translated");
    }

    @Test
    void shouldLeaveUnparseableConditionsExplicitlyUntranslated() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. COND02.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-COUNT PIC 9(2).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    IF FUNCTION LENGTH(WS-COUNT) = 0
                        MOVE 1 TO WS-COUNT
                    END-IF
                    GOBACK.
                """);

        assertThat(updated).contains("// COBOL not translated:");
        assertThat(updated).doesNotContain("if (FUNCTION");
    }

    @Test
    void shouldTranslateStaticCallWithByReferenceWriteBackAndOnException() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. CALL01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 WS-AREA.
                   05 WS-CODE PIC X(2).
                   05 WS-RC   PIC 9(2).
                01 WS-NAME PIC X(8).
                01 WS-FLAG PIC X.
                PROCEDURE DIVISION.
                MAIN-PARA.
                    CALL 'SUBPGM' USING WS-AREA BY CONTENT 'LIT'
                        ON EXCEPTION
                            MOVE 'E' TO WS-FLAG
                    END-CALL
                    CALL WS-NAME USING WS-AREA
                    GOBACK.
                """);

        assertThat(updated).contains("String[] callUsing1 = {output.getWsArea(), \"LIT\"};");
        assertThat(updated).contains("subpgmProgram.call(callUsing1);");
        assertThat(updated).contains("output.setWsArea(callUsing1[0]);");
        assertThat(updated).contains("} catch (UnsupportedOperationException programNotAvailable) {");
        assertThat(updated).contains("output.setWsFlag(\"E\");");
        assertThat(updated).contains("// COBOL not translated: CALL WS-NAME (program name resolved at run time)");
    }

    @Test
    void shouldTranslateSubscriptedTableReferencesAndRejectReferenceModification() {
        String updated = populate("""
                IDENTIFICATION DIVISION.
                PROGRAM-ID. TBL01.
                DATA DIVISION.
                WORKING-STORAGE SECTION.
                01 ARR-REC.
                   05 ARR-BAL OCCURS 3 TIMES.
                      10 ARR-AMT PIC S9(5)V99.
                01 WS-I     PIC 9(2).
                01 WS-TOTAL PIC S9(7)V99.
                01 WS-TEXT  PIC X(10).
                PROCEDURE DIVISION.
                MAIN-PARA.
                    MOVE 1005.00 TO ARR-AMT(1)
                    MOVE 2 TO WS-I
                    MOVE ARR-AMT(1) TO ARR-AMT(WS-I)
                    COMPUTE WS-TOTAL = ARR-AMT(1) + ARR-AMT(WS-I)
                    MOVE WS-TEXT(1:3) TO WS-TEXT
                    MOVE ARR-AMT TO WS-TOTAL
                    GOBACK.
                """);

        assertThat(updated).contains("output.setArrAmt(1, new java.math.BigDecimal(\"1005.00\"));");
        assertThat(updated).contains("output.setArrAmt(output.getWsI(), output.getArrAmt(1));");
        assertThat(updated).contains("output.setWsTotal(output.getArrAmt(1).add(output.getArrAmt(output.getWsI()))");
        assertThat(updated).contains("// COBOL not translated: WS-TEXT (data item not modeled)");
        assertThat(updated).contains("// COBOL not translated: ARR-AMT");
    }
}
