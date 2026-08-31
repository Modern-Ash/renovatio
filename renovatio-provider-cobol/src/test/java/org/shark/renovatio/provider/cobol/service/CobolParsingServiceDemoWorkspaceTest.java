package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.domain.AnalyzeResult;
import org.shark.renovatio.shared.domain.Workspace;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobolParsingServiceDemoWorkspaceTest {

    private static final Path DEMO_WORKSPACE = Paths.get(
            "/home/faguero/dev/renovatio/demo/aws-mainframe-modernization-carddemo/app"
    );

    @Test
    void analyzeDemoWorkspaceRecursivelyFindsCobolSources() throws Exception {
        Assumptions.assumeTrue(Files.isDirectory(DEMO_WORKSPACE),
                "Demo workspace not available at " + DEMO_WORKSPACE);

        CobolParsingService service = new CobolParsingService();

        List<Path> cobolFiles = service.findCobolSourceFiles(DEMO_WORKSPACE);
        List<Path> copybooks = service.findCopybooks(DEMO_WORKSPACE);
        assertNotNull(cobolFiles);
        assertNotNull(copybooks);
        assertFalse(cobolFiles.isEmpty(), "Expected COBOL sources under the demo workspace");

        System.out.println("=== COBOL DEMO PARSING SUMMARY ===");
        System.out.println("Workspace: " + DEMO_WORKSPACE);
        System.out.println("COBOL source files: " + cobolFiles.size());
        System.out.println("Copybooks: " + copybooks.size());
        System.out.println("First COBOL source files:");
        cobolFiles.stream()
                .limit(10)
                .map(path -> "  - " + DEMO_WORKSPACE.relativize(path))
                .forEach(System.out::println);

        assertTrue(cobolFiles.stream().anyMatch(path ->
                        path.toString().contains("/app/cbl/")),
                "Expected files under the top-level cbl directory");
        assertTrue(cobolFiles.stream().anyMatch(path ->
                        path.toString().contains("/app/app-transaction-type-db2/cbl/")),
                "Expected files under nested app-transaction-type-db2/cbl");
        assertTrue(cobolFiles.stream().anyMatch(path ->
                        path.toString().contains("/app/app-authorization-ims-db2-mq/cbl/")),
                "Expected files under nested app-authorization-ims-db2-mq/cbl");

        Workspace workspace = new Workspace();
        workspace.setId("carddemo-demo");
        workspace.setPath(DEMO_WORKSPACE.toString());
        workspace.setBranch("main");

        NqlQuery query = new NqlQuery();
        query.setType(NqlQuery.QueryType.FIND);
        query.setTarget("programs");
        query.setLanguage("cobol");

        AnalyzeResult result = service.analyzeCOBOL(query, workspace);
        assertNotNull(result);
        assertTrue(result.isSuccess(), () -> "Expected analyzeCOBOL to succeed: " + result.getMessage());
        assertNotNull(result.getData());
        assertTrue(result.getData().containsKey("sourceFiles"), "Expected sourceFiles in analysis data");
        assertTrue(result.getData().containsKey("copybooks"), "Expected copybooks in analysis data");
        assertTrue(result.getData().containsKey("summary"), "Expected summary in analysis data");

        Object programs = result.getData().get("programs");
        assertTrue(programs instanceof List<?>, "Expected a programs list in analyze result");
        assertFalse(((List<?>) programs).isEmpty(), "Expected at least one COBOL program in the demo workspace");

        @SuppressWarnings("unchecked")
        List<Object> programList = (List<Object>) programs;
        List<String> programIds = programList.stream()
                .map(item -> {
                    if (item instanceof org.shark.renovatio.provider.cobol.domain.CobolProgram program) {
                        return program.getProgramId();
                    }
                    return String.valueOf(item);
                })
                .limit(10)
                .collect(Collectors.toList());

        System.out.println("Parsed COBOL programs: " + programList.size());
        System.out.println("Sample program IDs: " + programIds);
        System.out.println("Analyze message: " + result.getMessage());
        System.out.println("Performance metrics: " + (result.getPerformance() != null ? result.getPerformance().getExecutionTimeMs() + " ms" : "n/a"));

        String message = result.getMessage() == null ? "" : result.getMessage().toLowerCase(Locale.ROOT);
        assertTrue(message.contains("parsed"), "Expected a parsed message but got: " + result.getMessage());

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) result.getData().get("summary");
        assertNotNull(summary);
        assertTrue(summary.containsKey("sourceFiles"));
        assertTrue(summary.containsKey("copybooks"));
        assertTrue(summary.containsKey("programs"));
        assertEquals(cobolFiles.size(), ((Number) summary.get("sourceFiles")).intValue());
        assertEquals(copybooks.size(), ((Number) summary.get("copybooks")).intValue());
        assertEquals(cobolFiles.size(), ((Number) summary.get("programs")).intValue());
        assertTrue(result.getMessage().contains(String.valueOf(cobolFiles.size())),
                "Expected analyzed source count to match detected COBOL files");
    }
}
