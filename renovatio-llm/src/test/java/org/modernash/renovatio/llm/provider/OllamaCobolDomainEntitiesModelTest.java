package org.modernash.renovatio.llm.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.modernash.renovatio.llm.enrichment.PersistenceSanitizer;
import org.modernash.renovatio.llm.prompt.PreparedEnrichment;
import org.modernash.renovatio.llm.prompt.PromptCatalogLoader;
import org.modernash.renovatio.llm.prompt.PromptOutputValidator;
import org.modernash.renovatio.llm.prompt.PromptRuntime;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OllamaCobolDomainEntitiesModelTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String QWEN_COBOL_MODEL = "sammcj/qwen2.5-cobol-coder-7b-instruct";

    @Test
    void qwenCobolDomainEntitiesContractIsSchemaValidAndGrounded() {
        PromptRuntime runtime = runtime();
        PreparedEnrichment prepared = runtime.prepare("cobol.domain.entities.v1", cardXrefFacts(),
                "ollama", QWEN_COBOL_MODEL);
        OllamaTransport transport = (request, configuration) -> {
            assertEquals(QWEN_COBOL_MODEL, configuration.model());
            assertEquals("cobol.domain.entities.v1", request.promptId());
            assertEquals(cardXrefFacts(), request.input());
            assertTrue(request.systemPrompt().contains("Every entity's sourceFacts"));
            return new LlmResponse("ollama", configuration.model(), groundedDomainEntitiesOutput());
        };
        OllamaLlmProvider provider = new OllamaLlmProvider(
                new OllamaConfiguration(QWEN_COBOL_MODEL, URI.create("http://localhost:11434/api/chat"),
                        Duration.ofSeconds(120)),
                transport, new RetryPolicy(), () -> 0.0, ignored -> { });

        JsonNode output = provider.complete(prepared.request()).content();
        JsonNode validated = new PromptOutputValidator(runtime, new PersistenceSanitizer())
                .validate(prepared, output);

        assertGrounded(validated, cardXrefFacts());
        assertEquals("CARD-XREF", validated.path("relations").get(0).path("fromEntity").textValue());
        assertEquals("ACCOUNT", validated.path("relations").get(0).path("toEntity").textValue());
        assertEquals("XREF-ACCT-ID", validated.path("relations").get(0).path("evidenceField").textValue());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RENOVATIO_RUN_OLLAMA_MODEL_TESTS", matches = "true")
    void localOllamaModelProducesSchemaValidGroundedDomainEntities() throws IOException {
        Properties properties = new Properties();
        properties.setProperty(OllamaConfiguration.MODEL_PROPERTY, QWEN_COBOL_MODEL);
        properties.setProperty(OllamaConfiguration.ENDPOINT_PROPERTY, "http://localhost:11434/api/chat");
        OllamaConfiguration configuration = OllamaConfiguration.from(properties, System.getenv());
        PromptRuntime runtime = runtime();
        PreparedEnrichment prepared = runtime.prepare("cobol.domain.entities.v1", cardXrefFacts(),
                "ollama", configuration.model());
        OllamaLlmProvider provider = new OllamaLlmProvider(configuration, new OllamaHttpTransport(),
                new RetryPolicy(), () -> 0.0, ignored -> { });

        JsonNode output = provider.complete(prepared.request()).content();
        writeModelOutput("target/ollama-domain-entities-output.json", output);
        JsonNode validated = new PromptOutputValidator(runtime, new PersistenceSanitizer())
                .validate(prepared, output);

        assertGrounded(validated, cardXrefFacts());
        assertHasRelation(validated, "XREF-ACCT-ID");
    }

    private static PromptRuntime runtime() {
        return new PromptRuntime(new PromptCatalogLoader().loadDefault());
    }

    private static ObjectNode cardXrefFacts() {
        ObjectNode root = JSON.createObjectNode();
        ArrayNode facts = root.putArray("facts");
        ObjectNode account = facts.addObject();
        account.put("fd", "ACCTFILE-FILE");
        account.put("assignTo", "ACCTFILE");
        account.put("recordKey", "FD-ACCT-ID");
        account.putArray("fields").add("FD-ACCT-ID").add("FD-ACCT-DATA");
        ObjectNode xref = facts.addObject();
        xref.put("fd", "XREF-FILE");
        xref.put("assignTo", "XREFFILE");
        xref.put("recordKey", "FD-XREF-CARD-NUM");
        xref.putArray("fields").add("FD-XREF-CARD-NUM").add("XREF-CUST-ID").add("XREF-ACCT-ID");
        ObjectNode move = facts.addObject().putObject("move");
        move.put("source", "XREF-ACCT-ID");
        move.put("target", "FD-ACCT-ID");
        return root;
    }

    private static ObjectNode groundedDomainEntitiesOutput() {
        ObjectNode root = JSON.createObjectNode();
        ArrayNode entities = root.putArray("entities");
        ObjectNode account = entities.addObject();
        account.put("name", "ACCOUNT");
        account.putArray("sourceFacts").add("ACCTFILE-FILE");
        account.putArray("fields").add("FD-ACCT-ID").add("FD-ACCT-DATA");
        ObjectNode xref = entities.addObject();
        xref.put("name", "CARD-XREF");
        xref.putArray("sourceFacts").add("XREF-FILE");
        xref.putArray("fields").add("FD-XREF-CARD-NUM").add("XREF-CUST-ID").add("XREF-ACCT-ID");
        ObjectNode relation = root.putArray("relations").addObject();
        relation.put("fromEntity", "CARD-XREF");
        relation.put("toEntity", "ACCOUNT");
        relation.put("evidenceField", "XREF-ACCT-ID");
        relation.put("cardinality", "MANY_TO_ONE");
        relation.put("confidence", 0.9);
        relation.put("rationale", "MOVE XREF-ACCT-ID TO FD-ACCT-ID links XREF-FILE to ACCTFILE-FILE.");
        return root;
    }

    private static void assertGrounded(JsonNode output, JsonNode input) {
        Set<String> facts = suppliedFactNames(input);
        for (JsonNode entity : output.withArray("entities")) {
            for (JsonNode sourceFact : entity.withArray("sourceFacts")) {
                assertTrue(facts.contains(sourceFact.asText()), "ungrounded sourceFact " + sourceFact.asText());
            }
            for (JsonNode field : entity.withArray("fields")) {
                assertTrue(facts.contains(field.asText()), "ungrounded entity field " + field.asText());
            }
        }
        for (JsonNode relation : output.withArray("relations")) {
            assertFalse(relation.path("fromEntity").asText().equals(relation.path("toEntity").asText()),
                    "self relation is not allowed");
            assertTrue(facts.contains(relation.path("evidenceField").asText()),
                    "ungrounded evidenceField " + relation.path("evidenceField").asText());
        }
    }

    private static void assertHasRelation(JsonNode output, String evidenceField) {
        for (JsonNode relation : output.withArray("relations")) {
            if (evidenceField.equals(relation.path("evidenceField").asText())) {
                return;
            }
        }
        throw new AssertionError("missing relation with evidenceField " + evidenceField);
    }

    private static void writeModelOutput(String path, JsonNode output) throws IOException {
        Path target = Path.of(path);
        Files.createDirectories(target.getParent());
        JSON.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), output);
    }

    private static Set<String> suppliedFactNames(JsonNode input) {
        Set<String> result = new HashSet<>();
        for (JsonNode fact : input.withArray("facts")) {
            addText(result, fact, "fd");
            addText(result, fact, "assignTo");
            addText(result, fact, "recordKey");
            for (JsonNode field : fact.withArray("fields")) {
                result.add(field.asText());
            }
            JsonNode move = fact.path("move");
            if (move.isObject()) {
                addText(result, move, "source");
                addText(result, move, "target");
            }
        }
        return Set.copyOf(result);
    }

    private static void addText(Set<String> values, JsonNode node, String field) {
        if (node.path(field).isTextual()) {
            values.add(node.path(field).asText());
        }
    }
}
