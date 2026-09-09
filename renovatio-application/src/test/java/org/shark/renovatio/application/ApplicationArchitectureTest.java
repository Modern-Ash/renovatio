package org.shark.renovatio.application;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ApplicationArchitectureTest {
    @Test void moduleHasNoTransportFrameworkOrFilesystemDependencies() throws Exception {
        String descriptor = Files.readString(Path.of("src/main/java/module-info.java"));
        assertFalse(descriptor.contains("spring"));
        assertFalse(descriptor.contains("picocli"));
        assertFalse(descriptor.contains("mcp"));
        String sources = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.toString().endsWith(".java"))
                .map(path -> { try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); } })
                .reduce("", String::concat);
        assertFalse(sources.contains("java.nio.file"));
        assertFalse(sources.contains("org.springframework"));
    }
}
