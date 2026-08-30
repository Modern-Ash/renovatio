package org.shark.renovatio.llm.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;
import org.shark.renovatio.llm.cache.CacheIdentity;
import org.shark.renovatio.llm.cache.CacheKey;
import org.shark.renovatio.llm.prompt.PreparedEnrichment;
import org.shark.renovatio.llm.prompt.PromptCatalogLoader;
import org.shark.renovatio.llm.prompt.PromptRuntime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LlmEnrichmentCliTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporary;

    @Test
    void offlineInvocationIsBoundToRunningAgoraRecordAndWritesCandidate() throws Exception {
        Invocation invocation = invocation();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        LlmEnrichmentCli.run(invocation.arguments(), invocation.environment(), new Properties(),
                new PrintStream(bytes, true, StandardCharsets.UTF_8));

        JsonNode result = JSON.readTree(bytes.toString(StandardCharsets.UTF_8));
        assertEquals(invocation.cacheKey(), result.path("cacheKey").textValue());
        assertEquals("MODEL_SUCCESS", result.path("resultDisposition").textValue());
        assertTrue(Files.isRegularFile(temporary.resolve("renovatio-llm/src/main/resources/llm-cache")
                .resolve(invocation.cacheKey().substring(0, 2)).resolve(invocation.cacheKey() + ".json")));
    }

    @Test
    void mismatchedAttributionIdentityFailsBeforeWritingCandidate() throws Exception {
        Invocation invocation = invocation();
        String[] altered = invocation.arguments().clone();
        altered[6] = "f".repeat(64);

        assertThrows(IllegalArgumentException.class, () -> LlmEnrichmentCli.run(altered,
                invocation.environment(), new Properties(), System.out));
        assertTrue(Files.notExists(temporary.resolve("renovatio-llm/src/main/resources/llm-cache")));
    }

    private Invocation invocation() throws Exception {
        initializeRepository();
        JsonNode input = JSON.createObjectNode().put("nodeId", "node-1");
        JsonNode response = JSON.createObjectNode().put("suggestedName", "calculateInterest")
                .put("rationale", "Describes the paragraph action");
        Path request = temporary.resolve("request.json");
        com.fasterxml.jackson.databind.node.ObjectNode requestBody = JSON.createObjectNode();
        requestBody.set("canonicalInput", input);
        requestBody.set("deterministicResult",
                JSON.createObjectNode().put("rationale", "deterministic translation"));
        requestBody.set("offlineResponse", response);
        JSON.writerWithDefaultPrettyPrinter().writeValue(request.toFile(), requestBody);
        PromptRuntime runtime = new PromptRuntime(new PromptCatalogLoader().loadDefault());
        PreparedEnrichment prepared = runtime.prepare("cobol.domain.naming.v1", input,
                "offline-fake", "fixture-v1");
        CacheIdentity identity = prepared.identity();
        String inputHash = CacheKey.sha256(CanonicalJson.write(JSON.convertValue(input, Object.class)));
        String cacheKey = CacheKey.derive(identity);
        Path run = temporary.resolve("tool-abc123/RUN.md");
        Files.createDirectories(run.getParent());
        Files.writeString(run, """
                ---
                schema: "agora/tool-run/v1"
                id: "tool-abc123"
                tool: "llm-enrichment"
                operation: "enrich"
                status: "running"
                inputs: {"prompt-id":"cobol.domain.naming.v1","provider":"offline-fake","model":"fixture-v1","input-hash":"%s","cache-key":"%s","schema-hash":"%s","runtime-contract-version":"%s"}
                ---
                # Tool run
                """.formatted(inputHash, cacheKey, identity.outputSchemaHash(),
                CacheIdentity.RUNTIME_CONTRACT_VERSION));
        String[] arguments = {"enrich", "--prompt-id", "cobol.domain.naming.v1",
                "--provider", "offline-fake", "--model", "fixture-v1", "--input-hash", inputHash,
                "--cache-key", cacheKey, "--schema-hash", identity.outputSchemaHash(),
                "--runtime-contract-version", CacheIdentity.RUNTIME_CONTRACT_VERSION};
        Map<String, String> environment = Map.of(
                LlmEnrichmentCli.REQUEST_ENV, request.toString(),
                "AGORA_TOOL_RUN", run.toString(),
                "AGORA_PROJECT", temporary.toString());
        return new Invocation(arguments, environment, cacheKey);
    }

    private void initializeRepository() throws Exception {
        runGit("init", "-q");
        runGit("config", "user.name", "Renovatio Test");
        runGit("config", "user.email", "renovatio-test@example.invalid");
        Files.writeString(temporary.resolve(".gitkeep"), "fixture\n");
        runGit("add", ".gitkeep");
        runGit("commit", "-q", "-m", "test: initialize fixture");
    }

    private void runGit(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "git";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).directory(temporary.toFile()).start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException(new String(process.getErrorStream().readAllBytes(),
                    StandardCharsets.UTF_8));
        }
    }

    private record Invocation(String[] arguments, Map<String, String> environment, String cacheKey) { }
}
