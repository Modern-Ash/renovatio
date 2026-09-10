package org.shark.renovatio.persistence.strategy;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryStrategyTest {

    private static final DataAccessClassification EXEC_SQL_CLASSIFICATION = new DataAccessClassification(
            "id1", "TESTPGM", DataAccessKind.EXEC_SQL,
            Optional.of("CUSTOMER-MASTER"),
            DataAccessClassification.KeyShape.NONE,
            new DataAccessClassification.RecordShape(null, Optional.of("CUSTOMER"), List.of("ID", "NAME")),
            Optional.empty(), List.of(), 1.0, List.of(),
            new DataAccessClassification.ClassifierProvenance("src/test.cbl", "abc123", "COBOL"));

    private static final DataAccessClassification VSAM_KEY_CLASSIFICATION = new DataAccessClassification(
            "id2", "TESTPGM", DataAccessKind.VSAM_KEY,
            Optional.of("VSAM-DATASET"),
            new DataAccessClassification.KeyShape(List.of("CUST-ID")),
            new DataAccessClassification.RecordShape("CUST-REC", Optional.empty(), List.of()),
            Optional.empty(), List.of(), 1.0, List.of(),
            new DataAccessClassification.ClassifierProvenance("src/test.cbl", "abc123", "COBOL"));

    private static final MigrationProfiles.EffectiveProfile DEFAULT_PROFILE =
            MigrationProfiles.effective(MigrationProfiles.defaults(), Map.of(), Map.of(), List.of());

    @Test
    void supportsJavaTarget() {
        var strategy = new InMemoryStrategy();
        assertTrue(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.JAVA));
    }

    @Test
    void doesNotSupportNonJavaTargets() {
        var strategy = new InMemoryStrategy();
        assertFalse(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.NODE));
        assertFalse(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.PYTHON));
    }

    @Test
    void emitExecSqlProducesEntityAndRepository() {
        var strategy = new InMemoryStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertNotNull(artifacts.entityId());
        assertNotNull(artifacts.entitySource());
        assertNotNull(artifacts.repositoryId());
        assertNotNull(artifacts.repositorySource());
        assertTrue(artifacts.entitySource().contains("public class"));
        assertTrue(artifacts.repositorySource().contains("Map"));
        assertTrue(artifacts.repositorySource().contains("findAll"));
    }

    @Test
    void emitVsamKeyUsesKeyFields() {
        var strategy = new InMemoryStrategy();
        var artifacts = strategy.emit(VSAM_KEY_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.entitySource().contains("custId"));
        assertTrue(artifacts.entitySource().contains("private String"));
    }

    @Test
    void configSnippetContainsEntityName() {
        var strategy = new InMemoryStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.configSnippet().contains("CustomerMaster"));
    }

    @Test
    void diagnosticsEmptyForValidInput() {
        var strategy = new InMemoryStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.diagnostics().isEmpty());
    }
}
