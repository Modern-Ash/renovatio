package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Objects;

/** Writes byte-stable action-item reports using an atomic replacement. */
public final class ManualActionItemWriter {

    public static final String SCHEMA_VERSION = "manual-action-item.v1";
    public static final Path DEFAULT_REPORT =
            Path.of("build", "reports", "renovatio", "manual-action-items.json");

    private final ObjectMapper objectMapper;
    private final SensitiveValueRedactor redactor = new SensitiveValueRedactor();

    public ManualActionItemWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper").copy()
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    public void write(Path report, Collection<ManualActionItem> items) throws IOException {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(items, "items");
        Path absoluteReport = report.toAbsolutePath();
        Files.createDirectories(absoluteReport.getParent());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", SCHEMA_VERSION);
        ArrayNode entries = root.putArray("items");
        items.stream().sorted().forEach(item -> entries.add(redact(objectMapper.valueToTree(item))));

        Path temporary = Files.createTempFile(absoluteReport.getParent(), ".manual-action-items-", ".json");
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), root);
            Files.move(temporary, absoluteReport, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private JsonNode redact(JsonNode node) {
        if (node.isTextual()) {
            return TextNode.valueOf(redactor.redact(node.textValue()));
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> entry.setValue(redact(entry.getValue())));
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                ((ArrayNode) node).set(index, redact(node.get(index)));
            }
        }
        return node;
    }
}
