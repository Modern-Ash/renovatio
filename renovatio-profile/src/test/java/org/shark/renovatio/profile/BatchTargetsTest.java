package org.shark.renovatio.profile;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BatchTargetsTest {
    @Test
    void defaultsJavaToSpringBatchAndHonorsAnOverride() {
        assertEquals(MigrationProfile.BatchTarget.SPRING_BATCH,
                BatchTargets.resolve(MigrationProfiles.defaults()));
        MigrationProfile override = new MigrationProfile("1", Map.of("batch.target", "scheduler"),
                new MigrationProfile.Target(MigrationProfile.Language.JAVA, "21"), null, null, null, null, null);
        assertEquals(MigrationProfile.BatchTarget.SCHEDULER, BatchTargets.resolve(MigrationProfiles.resolve(override)));
    }

    @Test
    void reportsInvalidValuesAtTheExtensionPath() {
        MigrationProfile invalid = new MigrationProfile("1", Map.of("batch.target", "JCL"), null, null, null, null, null, null);
        assertEquals("/extensions/batch.target", MigrationProfiles.validateOverlay(invalid).get(0).path());
    }
}
