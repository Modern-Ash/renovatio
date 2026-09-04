package org.shark.renovatio.cli.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.shark.renovatio.cli.RenovatioCli;
import org.shark.renovatio.cli.ReusableProjectStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCommandTest {
    private static final String COBOL = """
            IDENTIFICATION DIVISION.
            PROGRAM-ID. ROUTED.
            DATA DIVISION.
            WORKING-STORAGE SECTION.
            01 CUSTOMER-NAME PIC X(20).
            PROCEDURE DIVISION.
            MAIN-PARA.
                MOVE 'A' TO CUSTOMER-NAME.
            """;

    @TempDir Path temporary;

    @Test
    void explicitJsonProfileGeneratesJavaToSelectedOutput() throws Exception {
        Path workspace = workspace("java");
        Path profileFile = temporary.resolve("java-profile.json");
        Files.writeString(profileFile, MigrationProfiles.writeJson(MigrationProfiles.emptyOverlay()));

        int exit = new CommandLine(new RenovatioCli()).execute("generate", workspace.toString(),
                "--profile", profileFile.toString(), "--out", "java-output");

        assertEquals(0, exit);
        assertTrue(Files.walk(workspace.resolve("java-output")).anyMatch(path -> path.toString().endsWith(".java")));
        assertEquals(MigrationProfiles.emptyOverlay(), new ReusableProjectStore(workspace).profile());
    }

    @Test
    void explicitYamlProfileUsesPackagedNodeEmitterForEveryProgram() throws Exception {
        Path workspace = workspace("node");
        Files.writeString(workspace.resolve("second.cob"), COBOL.replace("ROUTED", "SECOND"));
        Path profileFile = temporary.resolve("node-profile.yaml");
        MigrationProfile node = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"), null, null, null, null, null);
        Files.writeString(profileFile, MigrationProfiles.writeYaml(node));

        int exit = new CommandLine(new RenovatioCli()).execute("generate", workspace.toString(),
                "--profile", profileFile.toString(), "--out", "node-output");

        assertEquals(0, exit);
        assertTrue(Files.isRegularFile(workspace.resolve("node-output/src/main.ts")));
        assertTrue(Files.isRegularFile(workspace.resolve("node-output/package.json")));
        assertTrue(Files.isRegularFile(workspace.resolve("node-output/src/routed/domain/routed.service.ts")));
        assertTrue(Files.isRegularFile(workspace.resolve("node-output/src/second/domain/second.service.ts")));
        assertEquals(MigrationProfile.Language.NODE,
                new ReusableProjectStore(workspace).effectiveProfile().profile().target().language());
    }

    @Test
    void unsupportedTargetFailsClosedAndMalformedProfileDoesNotReplaceState() throws Exception {
        Path workspace = workspace("failures");
        var store = new ReusableProjectStore(workspace);
        store.profile(MigrationProfiles.emptyOverlay());
        Path malformed = temporary.resolve("bad.json");
        Files.writeString(malformed, "{not-json");
        CommandLine cli = new CommandLine(new RenovatioCli());

        assertEquals(1, cli.execute("generate", workspace.toString(), "--profile", malformed.toString()));
        assertEquals(MigrationProfiles.emptyOverlay(), store.profile());

        Path unsupported = temporary.resolve("profile.txt");
        Files.writeString(unsupported, MigrationProfiles.writeJson(MigrationProfiles.emptyOverlay()));
        assertEquals(1, cli.execute("generate", workspace.toString(), "--profile", unsupported.toString()));
        assertEquals(MigrationProfiles.emptyOverlay(), store.profile());

        Path valid = temporary.resolve("valid.json");
        Files.writeString(valid, MigrationProfiles.writeJson(MigrationProfiles.emptyOverlay()));
        assertEquals(1, cli.execute("generate", workspace.toString(), "--profile", valid.toString(),
                "--out", "../outside"));
        assertEquals(MigrationProfiles.emptyOverlay(), store.profile());
        assertFalse(Files.exists(temporary.resolve("outside")));

        Path pythonFile = temporary.resolve("python.json");
        MigrationProfile python = new MigrationProfile("1", Map.of(),
                new MigrationProfile.Target(MigrationProfile.Language.PYTHON, "3.12"), null, null, null, null, null);
        Files.writeString(pythonFile, MigrationProfiles.writeJson(python));
        assertEquals(1, cli.execute("generate", workspace.toString(), "--profile", pythonFile.toString()));
        assertEquals(MigrationProfile.Language.PYTHON, store.profile().target().language());
        assertFalse(Files.exists(workspace.resolve("generated-java-stubs")));
    }

    private Path workspace(String name) throws Exception {
        Path workspace = Files.createDirectories(temporary.resolve(name));
        Files.writeString(workspace.resolve("routed.cob"), COBOL);
        return workspace;
    }
}
