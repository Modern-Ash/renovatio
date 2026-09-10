package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class PipelineValidationTest {
    @TempDir
    Path tempDir;
    private PipelineRequest request;

    @BeforeEach
    void setUp() throws IOException {
        Path fixture = tempDir.resolve("fixture");
        Path source = fixture.resolve("src/cobol/VALID01.cbl");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "IDENTIFICATION DIVISION. PROGRAM-ID. VALID01.");
        request = new PipelineRequest("fixture", fixture, Map.of(), tempDir.resolve("output"),
            null, false, false);
    }

    @Test
    void determinismComparesIndependentGeneratedTrees() {
        DeterminismValidator validator = new DeterminismValidator();

        assertThat(validator.validate(new FileWritingPipeline(false), request, 2)
            .isDeterministic()).isTrue();
        assertThat(validator.validate(new FileWritingPipeline(true), request, 2)
            .isDeterministic()).isFalse();
    }

    @Test
    void idempotencyComparesGeneratedBytesBetweenApplications() {
        IdempotencyValidator validator = new IdempotencyValidator();

        assertThat(validator.validate(new FileWritingPipeline(false), request).isIdempotent()).isTrue();
        assertThat(validator.validate(new FileWritingPipeline(true), request).isIdempotent()).isFalse();
    }

    private static final class FileWritingPipeline implements PipelineOrchestrator {
        private final boolean drift;
        private final AtomicInteger executions = new AtomicInteger();

        private FileWritingPipeline(boolean drift) {
            this.drift = drift;
        }

        @Override
        public PipelineResult execute(PipelineRequest request) {
            try {
                Path generated = request.outputDir().resolve("src/main/java/Generated.java");
                Files.createDirectories(generated.getParent());
                int run = executions.incrementAndGet();
                Files.writeString(generated, drift ? "class Generated { int run = " + run + "; }"
                    : "class Generated {}");
                return successfulResult(request);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        }

        @Override
        public List<PipelineResult> executeAll(List<PipelineRequest> requests) {
            return requests.stream().map(this::execute).toList();
        }

        private PipelineResult successfulResult(PipelineRequest request) {
            Instant now = Instant.now();
            StageResult stage = StageResult.success("test", now, now, "ok");
            return new PipelineResult(request.fixtureId(), request.outputDir(), now, now,
                Duration.ZERO, stage, stage, stage, stage, stage, stage, stage, stage, stage,
                null, List.of(), List.of(), true);
        }
    }
}
