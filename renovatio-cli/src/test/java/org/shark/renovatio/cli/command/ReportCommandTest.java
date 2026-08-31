package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCommandTest {

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
    void missingBothFlagsExits2() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("report");

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("specify exactly one");
    }

    @Test
    void bothFlagsExits2() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("report", "--html", "/tmp/out.html", "--pdf", "/tmp/out.pdf");

        assertThat(exitCode).isEqualTo(2);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("mutually exclusive");
    }
}
