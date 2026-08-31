package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewCommandTest {

    private static final Path SAMPLE_REPORT = Path.of("src/test/resources/reports/sample-manual-action-items.json");
    private static final Path EMPTY_REPORT = Path.of("src/test/resources/reports/empty-report.json");

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUpStreams() {
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void missingReportFileExits1() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("review", "--report", "/nonexistent/report.json");

        assertThat(exitCode).isEqualTo(1);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("report not found");
    }

    @Test
    void emptyReportExits0WithMessage() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("review", "--report", EMPTY_REPORT.toString());

        assertThat(exitCode).isZero();
        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("no manual action items");
    }

    @Test
    void sampleReportRendersChecklist() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("review", "--report", SAMPLE_REPORT.toString());

        assertThat(exitCode).isZero();
        String output = stdout.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("[ ] error");
        assertThat(output).contains("[ ] warning");
        assertThat(output).contains("[ ] info");
        assertThat(output).contains("reason:");
        assertThat(output).contains("required action:");
        assertThat(output).contains("acceptance:");
    }

    @Test
    void severityFilterDropsLowerItems() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        cmd.execute("review", "--report", SAMPLE_REPORT.toString(), "--severity", "error");

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("[ ] error");
        assertThat(output).doesNotContain("[ ] info");
    }

    @Test
    void jsonOutputIsValidJson() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        cmd.execute("review", "--report", SAMPLE_REPORT.toString(), "--json");

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("\"schemaVersion\"");
        assertThat(output).contains("\"items\"");
    }
}
