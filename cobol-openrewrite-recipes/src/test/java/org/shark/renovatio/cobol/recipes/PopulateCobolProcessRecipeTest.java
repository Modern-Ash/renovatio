package org.shark.renovatio.cobol.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.shark.renovatio.cobol.ir.model.CobolIntermediateModel;
import org.shark.renovatio.cobol.ir.parser.SimpleCobolIrParser;

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
    void shouldInlinePerformParagraphs() {
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
                    GOBACK.
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
        assertThat(updated).contains("output.setCustomerName(\"INIT\");");
        assertThat(updated).contains("output.setCustomerName(\"READY\");");
        assertThat(updated).doesNotContain("PERFORM");
    }

    @Test
    void shouldProduceByteStableOutputAcrossIndependentRuns() {
        String first = applyRecipe(COBOL_SAMPLE);
        String second = applyRecipe(COBOL_SAMPLE);

        assertThat(second).isEqualTo(first);
        assertThat(sha256(second)).isEqualTo(sha256(first));
    }

    @Test
    void productionBoundaryShouldContainNoNetworkOrLlmDependency() throws IOException {
        Path moduleRoot = locateModuleRoot();
        String productionBoundary;
        try (Stream<Path> paths = Files.walk(moduleRoot.resolve("src/main"))) {
            productionBoundary = paths
                    .filter(Files::isRegularFile)
                    .sorted()
                    .map(path -> inspectableProductionEntry(moduleRoot, path))
                    .reduce("", (left, right) -> left + "\n" + right);
        }

        String boundary = productionBoundary.toLowerCase(Locale.ROOT);
        assertThat(boundary).doesNotContain(
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
        if (Files.isRegularFile(workingDirectory.resolve("src/main/java/org/shark/renovatio/cobol/recipes/PopulateCobolProcessRecipe.java"))) {
            return workingDirectory;
        }
        Path childModule = workingDirectory.resolve("cobol-openrewrite-recipes");
        if (Files.isRegularFile(childModule.resolve("src/main/java/org/shark/renovatio/cobol/recipes/PopulateCobolProcessRecipe.java"))) {
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
}
