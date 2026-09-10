package org.shark.renovatio.provider.cobol.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EquivalenceCheckerImplTest {

    @TempDir
    Path tempDir;

    private final EquivalenceChecker checker = new EquivalenceCheckerImpl();

    @Test
    void failsWhenAnExpectedFileWasNotGenerated() throws IOException {
        Path actual = Files.createDirectories(tempDir.resolve("actual"));
        Path expected = Files.createDirectories(tempDir.resolve("expected"));
        Files.writeString(expected.resolve("Missing.java"), "class Missing {}\n");

        List<EquivalenceReport> reports = checker.checkAll(
            "fixture", actual, expected, EquivalenceChecker.EquivalenceConfig.strict()
        );

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).decision()).isEqualTo(EquivalenceReport.GateDecision.FAIL);
        assertThat(reports.get(0).divergences().get(0).actualLine())
            .contains("missing expected file");
    }

    @Test
    void failsWhenGenerationProducesAnUnexpectedFile() throws IOException {
        Path actual = Files.createDirectories(tempDir.resolve("actual"));
        Path expected = Files.createDirectories(tempDir.resolve("expected"));
        Files.writeString(actual.resolve("Unexpected.java"), "class Unexpected {}\n");

        List<EquivalenceReport> reports = checker.checkAll(
            "fixture", actual, expected, EquivalenceChecker.EquivalenceConfig.strict()
        );

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).decision()).isEqualTo(EquivalenceReport.GateDecision.FAIL);
        assertThat(reports.get(0).divergences().get(0).actualLine())
            .contains("unexpected file");
    }

    @Test
    void passesWhenBothFileSetsAndContentsMatch() throws IOException {
        Path actual = Files.createDirectories(tempDir.resolve("actual/src"));
        Path expected = Files.createDirectories(tempDir.resolve("expected/src"));
        Files.writeString(actual.resolve("Match.java"), "class Match {}\n");
        Files.writeString(expected.resolve("Match.java"), "class Match {}\n");

        List<EquivalenceReport> reports = checker.checkAll(
            "fixture",
            tempDir.resolve("actual"),
            tempDir.resolve("expected"),
            EquivalenceChecker.EquivalenceConfig.strict()
        );

        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).isPassed()).isTrue();
    }
}
