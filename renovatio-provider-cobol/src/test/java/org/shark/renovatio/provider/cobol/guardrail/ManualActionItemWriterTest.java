package org.shark.renovatio.provider.cobol.guardrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManualActionItemWriterTest {

    @TempDir
    Path temporaryDirectory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void writesStableSortedReport() throws Exception {
        Path first = temporaryDirectory.resolve("first.json");
        Path second = temporaryDirectory.resolve("second.json");
        ManualActionItemWriter writer = new ManualActionItemWriter(objectMapper);
        ManualActionItem alpha = item("mai-000000000000000000000001");
        ManualActionItem beta = item("mai-000000000000000000000002");

        writer.write(first, List.of(beta, alpha));
        writer.write(second, List.of(alpha, beta));

        assertThat(Files.readAllBytes(first)).isEqualTo(Files.readAllBytes(second));
        JsonNode report = objectMapper.readTree(first.toFile());
        assertThat(report.path("schemaVersion").asText()).isEqualTo("manual-action-item.v1");
        assertThat(report.path("items").get(0).path("id").asText()).isEqualTo(alpha.id());
    }

    private static ManualActionItem item(String id) {
        return new ManualActionItem(id, "input.cob", "SAMPLE", "PROCEDURE", null,
                "1000-PROC", "10:1-10:20", null, "sha256:source", "GO_TO",
                "Irreducible control flow", "characterization", "surefire:test",
                "No transformed code emitted", "Review the control-flow plan",
                "Characterization tests preserve behavior", "error", "pending",
                "sha256:schema", null, null, null, null, null);
    }
}
