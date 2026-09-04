package org.shark.renovatio.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RenovatioCliSmokeTest {

    @Test
    void helpPrintsUsage() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new RenovatioCli());
        commandLine.setOut(new PrintWriter(stdout, true, StandardCharsets.UTF_8));
        commandLine.setErr(new PrintWriter(stderr, true, StandardCharsets.UTF_8));

        int exitCode = commandLine.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("Usage: renovatio");
        assertThat(stderr.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void subcommandsRegistered() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new RenovatioCli());
        commandLine.setOut(new PrintWriter(stdout, true, StandardCharsets.UTF_8));

        commandLine.execute("--help");

        String output = stdout.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("analyze");
        assertThat(output).contains("metrics");
        assertThat(output).contains("generate");
        assertThat(output).contains("plan");
        assertThat(output).contains("apply");
        assertThat(output).contains("diff");
        assertThat(output).contains("review");
        assertThat(output).contains("report");
        assertThat(output).contains("serve");
        assertThat(output).contains("profile");
        assertThat(output).contains("decisions");
        assertThat(output).contains("policy");
    }

    @Test
    void newProfileAndGenerationCommandsExposeHelp() {
        CommandLine commandLine = new CommandLine(new RenovatioCli());
        commandLine.setOut(new PrintWriter(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        commandLine.setErr(new PrintWriter(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        assertThat(commandLine.execute("profile", "init", "--help")).isZero();
        assertThat(commandLine.execute("generate", "--help")).isZero();
    }

    @Test
    void versionPrints() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        CommandLine commandLine = new CommandLine(new RenovatioCli());
        commandLine.setOut(new PrintWriter(stdout, true, StandardCharsets.UTF_8));

        commandLine.execute("--version");

        assertThat(stdout.toString(StandardCharsets.UTF_8)).contains("renovatio-cli");
    }
}
