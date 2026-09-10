package org.shark.renovatio.emitter.node.prisma;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PrismaStrategyTest {
    private final PrismaStrategy strategy = new PrismaStrategy();

    @Test
    void supportsNodeAndRejectsResidual() {
        DataAccessClassification vsamKey = vsamKey();
        DataAccessClassification residual = residual();
        assertTrue(strategy.supports(vsamKey, MigrationProfile.Language.NODE));
        assertFalse(strategy.supports(vsamKey, MigrationProfile.Language.JAVA));
        assertFalse(strategy.supports(vsamKey, MigrationProfile.Language.PYTHON));
        assertFalse(strategy.supports(residual, MigrationProfile.Language.NODE));
    }

    @Test
    void emitsSchemaAndRepositoryForNode() {
        var artifacts = strategy.emit(vsamKey(), profile());
        assertNotNull(artifacts.entitySource());
        assertTrue(artifacts.entitySource().contains("prisma"));
        assertTrue(artifacts.entitySource().contains("generator client"));
        assertNotNull(artifacts.repositorySource());
        assertTrue(artifacts.repositorySource().contains("PrismaClient"));
    }

    private DataAccessClassification vsamKey() {
        return new DataAccessClassification("VSAM-KEY", "src/test.cob", DataAccessKind.VSAM_KEY,
                Optional.of("FILE1"), new DataAccessClassification.KeyShape(List.of("KEY")),
                new DataAccessClassification.RecordShape("FILE1", Optional.empty(), List.of()),
                Optional.empty(), List.of(), 0.9, List.of(),
                new DataAccessClassification.ClassifierProvenance("src/test.cob", "hash", "COBOL"));
    }

    private DataAccessClassification residual() {
        return new DataAccessClassification("RESIDUAL", "src/test.cob", DataAccessKind.RESIDUAL,
                Optional.empty(), DataAccessClassification.KeyShape.NONE,
                DataAccessClassification.RecordShape.UNKNOWN, Optional.empty(),
                List.of(), 0.5, List.of(),
                new DataAccessClassification.ClassifierProvenance("src/test.cob", "hash", "COBOL"));
    }

    private MigrationProfiles.EffectiveProfile profile() {
        return MigrationProfiles.effective(MigrationProfiles.emptyOverlay(), Map.of(), Map.of(), List.of());
    }
}
