package org.shark.renovatio.provider.cobol.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratedArtifactTreeWriterTest {

    @Test
    void replacesTheCompleteTreeAndRemovesStaleArtifacts(@TempDir Path root) throws Exception {
        Path output = Files.createDirectories(root.resolve("generated"));
        Files.writeString(output.resolve("stale.java"), "stale");

        new GeneratedArtifactTreeWriter().write(Map.of(
                "modules/pay/domain/Pay.java", "package pay; class Pay {}",
                "modules/pay/app/Run.java", "package pay; class Run {}"), output);

        assertFalse(Files.exists(output.resolve("stale.java")));
        assertEquals("package pay; class Pay {}",
                Files.readString(output.resolve("modules/pay/domain/Pay.java")));
    }

    @Test
    void validatesEveryArtifactBeforeTouchingTheExistingTree(@TempDir Path root) throws Exception {
        Path output = Files.createDirectories(root.resolve("generated"));
        Files.writeString(output.resolve("accepted.java"), "accepted");
        Map<String, String> invalid = new LinkedHashMap<>();
        invalid.put("valid.java", "valid");
        invalid.put("../escape.java", "escape");

        assertThrows(IllegalArgumentException.class,
                () -> new GeneratedArtifactTreeWriter().write(invalid, output));

        assertEquals("accepted", Files.readString(output.resolve("accepted.java")));
        assertFalse(Files.exists(root.resolve("escape.java")));
    }
}
