package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CapabilitiesCommandTest {
    @Test
    void printsSharedCapabilityContractAsJson() {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        CommandLine commandLine = new CommandLine(new CapabilitiesCommand());
        commandLine.setOut(new PrintWriter(stdout, true, StandardCharsets.UTF_8));

        int exit;
        try {
            System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
            exit = commandLine.execute("--json");
        } finally {
            System.setOut(originalOut);
        }

        assertThat(exit).isZero();
        assertThat(stdout.toString(StandardCharsets.UTF_8))
                .contains("\"id\" : \"renovatio.surface-capabilities\"")
                .contains("\"cobol.analyze\"")
                .contains("\"node.target\"")
                .contains("\"maturity\" : \"experimental\"")
                .contains("\"python.target\"")
                .contains("\"authorization\" : \"project-member\"")
                .contains("\"VALIDATION_FAILED\"")
                .contains("\"equivalence\" : \"supported\"");
    }
}
