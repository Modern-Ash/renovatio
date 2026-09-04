package org.shark.renovatio.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProfileTemplatesTest {
    @TempDir Path temporary;

    @Test
    void savesAppliesAndDiffsAnImmutableVersion() {
        var source = new MigrationProfile("1", Map.of("team", "core"),
                new MigrationProfile.Target(MigrationProfile.Language.NODE, "20"), null, null, null, null, null);
        var template = ProfileTemplates.snapshot("bank", "1.0.0", "Bank defaults", source, Instant.EPOCH);
        var repository = new FileProfileTemplateRepository(temporary.resolve("profiles"));

        assertEquals(template, repository.save(template));
        assertEquals(template, repository.save(template));
        assertEquals(template, repository.find(new TemplateReference("bank", "1.0.0")).orElseThrow());

        var project = new MigrationProfile("1", Map.of("local", true),
                new MigrationProfile.Target(null, "22"), null, null, null, null, null);
        var effective = ProfileTemplates.effective(template, project);
        assertEquals(MigrationProfile.Language.NODE, effective.target().language());
        assertEquals("22", effective.target().languageVersion());
        assertEquals(Map.of("team", "core", "local", true), effective.extensions());
        assertEquals(java.util.List.of("/extensions/local", "/target/languageVersion"),
                ProfileTemplates.diff(template, project).stream().map(ProfileTemplates.ProfileDiff::path).toList());
    }

    @Test
    void keepsVersionsExplicitAndRejectsConflictsAndTraversal() {
        var repository = new FileProfileTemplateRepository(temporary.resolve("profiles"));
        var first = ProfileTemplates.snapshot("bank", "1", null, MigrationProfiles.emptyOverlay(), Instant.EPOCH);
        var second = ProfileTemplates.snapshot("bank", "2", null, MigrationProfiles.defaults(), Instant.EPOCH);
        repository.save(first);
        repository.save(second);
        assertEquals(java.util.List.of("1", "2"), repository.list().stream().map(MigrationProfileTemplate::version).toList());

        var conflict = ProfileTemplates.snapshot("bank", "1", "changed", MigrationProfiles.emptyOverlay(), Instant.EPOCH);
        assertThrows(FileProfileTemplateRepository.VersionConflictException.class, () -> repository.save(conflict));
        assertThrows(IllegalArgumentException.class, () -> new TemplateReference("../bank", "1"));
        assertThrows(IllegalArgumentException.class, () -> new TemplateReference("bank", "/tmp/1"));
    }

    @Test
    void rejectsSymlinkEscape() throws Exception {
        Path root = temporary.resolve("profiles");
        Files.createDirectories(root);
        Path outside = temporary.resolve("outside");
        Files.createDirectories(outside);
        Path linkedName = root.resolve("bank");
        try {
            Files.createSymbolicLink(linkedName, outside);
        } catch (UnsupportedOperationException exception) {
            return;
        }
        var repository = new FileProfileTemplateRepository(root);
        var template = ProfileTemplates.snapshot("bank", "1", null, MigrationProfiles.emptyOverlay(), Instant.EPOCH);
        assertThrows(SecurityException.class, () -> repository.save(template));
        assertFalse(Files.exists(outside.resolve("1.json")));
    }
}
