package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DiffCommandTest {

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
    void missingRunIdArgExits2() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("diff");

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void unknownRunIdExits1() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("diff", "nonexistent-run-id", "-w", "/tmp");

        assertThat(exitCode).isEqualTo(1);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("unknown run id");
    }
}
