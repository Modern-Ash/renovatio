package org.shark.renovatio.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.shark.renovatio.provider.cobol.service.JavaGenerationService;
import org.shark.renovatio.shared.domain.Workspace;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReusableCommandsTest {
    @TempDir Path temporary;

    @AfterEach void clearProperty() { System.clearProperty("renovatio.assets.root"); }

    @Test
    void profileAndPolicyCommandsReuseStateAcrossProjectsWithoutSeededDecisions() throws Exception {
        System.setProperty("renovatio.assets.root", temporary.resolve("assets").toString());
        Path source = temporary.resolve("a");
        Path target = temporary.resolve("b");
        var sourceStore = new ReusableProjectStore(source);
        var targetStore = new ReusableProjectStore(target);
        sourceStore.profile(new MigrationProfile("1", Map.of("dialect", "IBM"),
                new MigrationProfile.Target(MigrationProfile.Language.JAVA, "21"), null, null, null, null, null));
        Files.createDirectories(source);
        Files.createDirectories(target);
        String cobol = """
                IDENTIFICATION DIVISION.
                PROGRAM-ID. HELLO.
                PROCEDURE DIVISION.
                    DISPLAY 'HELLO'.
                    STOP RUN.
                """;
        Files.writeString(source.resolve("HELLO.cbl"), cobol);
        Files.writeString(target.resolve("HELLO.cbl"), cobol);
        CommandLine cli = new CommandLine(new RenovatioCli());

        assertEquals(0, cli.execute("profile", "save", "bank", "--version", "1", "--project", source.toString()));
        assertEquals(0, cli.execute("profile", "apply", "bank", "--version", "1", "--project", target.toString()));
        assertTrue(Files.isRegularFile(target.resolve(".renovatio/profile-template.json")));
        assertEquals(MigrationProfiles.resolve(sourceStore.profile()), targetStore.profile());
        assertEquals(MigrationProfile.Language.JAVA, new RenovatioCliConfiguration().localEffectiveProfileResolver()
                .resolve(target.toAbsolutePath().normalize().toString()).profile().target().language());
        Workspace workspace = new Workspace();
        workspace.setId(target.toAbsolutePath().normalize().toString());
        workspace.setPath(target.toAbsolutePath().normalize().toString());
        assertEquals(targetStore.effectiveProfile(), RenovatioCliContext.shared()
                .bean(JavaGenerationService.class).effectiveProfile(workspace));

        assertEquals(0, cli.execute("analyze", source.toString()));
        assertEquals(0, cli.execute("analyze", target.toString()));
        assertEquals(7, sourceStore.decisions().size());
        for (DecisionPoint value : sourceStore.decisions()) {
            assertEquals(0, cli.execute("decisions", "set", value.decisionKey(), value.chosenOption(),
                    "--project", source.toString()));
        }
        assertEquals(7, sourceStore.decisions().stream()
                .filter(value -> value.status() == DecisionPoint.Status.CONFIRMED).count());

        assertEquals(0, cli.execute("policy", "export", "bank", "--version", "1", "--project", source.toString()));
        assertEquals(0, cli.execute("policy", "apply", "bank", "--version", "1", "--project", target.toString()));
        assertEquals(7, targetStore.decisions().stream().filter(value -> value.source() == DecisionPoint.Source.POLICY).count());
        assertTrue(Files.isRegularFile(target.resolve(".renovatio/policy-catalog.json")));
    }
}
