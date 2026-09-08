package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cli.RenovatioCli;
import org.shark.renovatio.profile.MigrationProfiles;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplyCommandTest {

    private static final String SUPPORTED_COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. ROUTED.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
                DISPLAY CUSTOMER-NAME.
                GOBACK.
            """;

    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @TempDir Path temporary;

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
    void missingPlanIdArgExits2() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("apply");

        assertThat(exitCode).isEqualTo(2);
    }

    @Test
    void unknownPlanIdExits1() {
        CommandLine cmd = new CommandLine(new RenovatioCliStub());

        int exitCode = cmd.execute("apply", "nonexistent-plan-id", "-w", "/tmp");

        assertThat(exitCode).isEqualTo(1);
        assertThat(stderr.toString(StandardCharsets.UTF_8)).contains("unknown plan id");
    }

    @Test
    void planAndRealApplyUseTheSameCanonicalJavaAsDirectGeneration() throws Exception {
        Path workspace = Files.createDirectories(temporary.resolve("workspace"));
        Files.writeString(workspace.resolve("routed.cob"), SUPPORTED_COBOL);
        Path profile = temporary.resolve("profile.json");
        Files.writeString(profile, MigrationProfiles.writeJson(MigrationProfiles.emptyOverlay()));
        Path directOutput = workspace.resolve("direct-output");
        Path applyOutput = workspace.resolve("apply-output");
        CommandLine cli = new CommandLine(new RenovatioCli());

        assertThat(cli.execute("generate", workspace.toString(), "--profile", profile.toString(),
                "--out", directOutput.toString()))
                .as("stdout=%s stderr=%s", stdout, stderr).isZero();
        assertThat(cli.execute("plan", workspace.toString()))
                .as("stdout=%s stderr=%s", stdout, stderr).isZero();

        Path planFile;
        try (var plans = Files.list(workspace.resolve(".renovatio/state/plans"))) {
            planFile = plans.findFirst().orElseThrow();
        }
        String planId = planFile.getFileName().toString().replaceFirst("\\.json$", "");

        assertThat(cli.execute("apply", planId, "--workspace", workspace.toString(),
                "--no-dry-run", "--out", applyOutput.toString()))
                .as("stdout=%s stderr=%s", stdout, stderr).isZero();
        assertThat(applyOutput).as("stdout=%s stderr=%s", stdout, stderr).isDirectory();

        Path directService = serviceImplementation(directOutput);
        Path appliedService = serviceImplementation(applyOutput);
        String directJava = Files.readString(directService);
        String appliedJava = Files.readString(appliedService);
        assertThat(appliedJava).isEqualTo(directJava)
                .contains("setCustomerName")
                .contains("System.out.println")
                .doesNotContain("// TODO: Implement COBOL business logic");
        assertThat(stdout.toString(StandardCharsets.UTF_8))
                .contains("javaOutputDirectory: " + applyOutput.toAbsolutePath().normalize());
    }

    private static Path serviceImplementation(Path output) throws Exception {
        try (var files = Files.walk(output)) {
            return files.filter(path -> path.getFileName().toString().endsWith("ServiceImpl.java"))
                    .findFirst().orElseThrow();
        }
    }
}
