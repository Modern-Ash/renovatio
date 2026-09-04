package org.shark.renovatio.cli;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.decisions.DecisionPoint;
import org.shark.renovatio.decisions.DecisionTransitions;
import org.shark.renovatio.decisions.F1DecisionCatalog;
import org.shark.renovatio.profile.MigrationProfile;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReusableCommandsTest {
    @TempDir Path temporary;

    @AfterEach void clearProperty() { System.clearProperty("renovatio.assets.root"); }

    @Test
    void profileAndPolicyCommandsReuseStateAcrossProjects() {
        System.setProperty("renovatio.assets.root", temporary.resolve("assets").toString());
        Path source = temporary.resolve("a");
        Path target = temporary.resolve("b");
        var sourceStore = new ReusableProjectStore(source);
        var targetStore = new ReusableProjectStore(target);
        sourceStore.profile(new MigrationProfile("1", Map.of("dialect", "IBM"),
                new MigrationProfile.Target(MigrationProfile.Language.JAVA, "21"), null, null, null, null, null));
        Instant now = Instant.EPOCH;
        var confirmed = F1DecisionCatalog.create("a".repeat(64), now).stream()
                .map(value -> DecisionTransitions.patch(value, value.chosenOption(), value.revision(), now.plusSeconds(1))).toList();
        sourceStore.decisions(confirmed);
        targetStore.decisions(F1DecisionCatalog.create("b".repeat(64), now));
        CommandLine cli = new CommandLine(new RenovatioCli());

        assertEquals(0, cli.execute("profile", "save", "bank", "--version", "1", "--project", source.toString()));
        assertEquals(0, cli.execute("profile", "apply", "bank", "--version", "1", "--project", target.toString()));
        assertTrue(Files.isRegularFile(target.resolve(".renovatio/profile-template.json")));

        assertEquals(0, cli.execute("policy", "export", "bank", "--version", "1", "--project", source.toString()));
        assertEquals(0, cli.execute("policy", "apply", "bank", "--version", "1", "--project", target.toString()));
        assertEquals(7, targetStore.decisions().stream().filter(value -> value.source() == DecisionPoint.Source.POLICY).count());
        assertTrue(Files.isRegularFile(target.resolve(".renovatio/policy-catalog.json")));
    }
}
