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
        assertThat(report.path("items").get(0).path("failedGate").asText())
                .isEqualTo("characterization");
        assertThat(report.path("items").get(0).path("severity").asText()).isEqualTo("error");
        assertThat(report.path("items").get(0).path("reviewStatus").asText()).isEqualTo("pending");
    }

    @Test
    void redactsCredentialsAcrossSerializedFields() throws Exception {
        Path reportPath = temporaryDirectory.resolve("redacted.json");
        ManualActionItem item = item("mai-000000000000000000000003",
                "Provider failed Authorization: Bearer secret-token and api_key=sk-example123456");

        new ManualActionItemWriter(objectMapper).write(reportPath, List.of(item));

        String report = Files.readString(reportPath);
        assertThat(report)
                .contains("[REDACTED]")
                .doesNotContain("secret-token")
                .doesNotContain("sk-example123456");
    }

    private static ManualActionItem item(String id) {
        return item(id, "Irreducible control flow");
    }

    private static ManualActionItem item(String id, String reason) {
        return new ManualActionItem(id, "input.cob", "SAMPLE", "PROCEDURE", null,
                "1000-PROC", "10:1-10:20", null, "sha256:source", "GO_TO",
                reason, GuardrailGate.CHARACTERIZATION, "surefire:test",
                "No transformed code emitted", "Review the control-flow plan",
                "Characterization tests preserve behavior", ManualActionSeverity.ERROR,
                ManualActionReviewStatus.PENDING,
                "sha256:schema", null, null, null, null, null);
    }
}
