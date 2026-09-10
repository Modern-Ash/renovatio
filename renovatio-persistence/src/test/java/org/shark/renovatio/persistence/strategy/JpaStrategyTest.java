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

class JpaStrategyTest {

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

    private static final DataAccessClassification RESIDUAL_CLASSIFICATION = new DataAccessClassification(
            "id3", "TESTPGM", DataAccessKind.RESIDUAL,
            Optional.of("UNKNOWN"),
            DataAccessClassification.KeyShape.NONE,
            DataAccessClassification.RecordShape.UNKNOWN,
            Optional.empty(), List.of(), 0.0, List.of(),
            new DataAccessClassification.ClassifierProvenance("src/test.cbl", "abc123", "COBOL"));

    private static final MigrationProfiles.EffectiveProfile DEFAULT_PROFILE =
            MigrationProfiles.effective(MigrationProfiles.defaults(), Map.of(), Map.of(), List.of());

    @Test
    void supportsJavaOnly() {
        var strategy = new JpaStrategy();
        assertTrue(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.JAVA));
        assertFalse(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.NODE));
    }

    @Test
    void doesNotSupportResidual() {
        var strategy = new JpaStrategy();
        assertFalse(strategy.supports(RESIDUAL_CLASSIFICATION, MigrationProfile.Language.JAVA));
    }

    @Test
    void emitExecSqlProducesEntityWithJpaAnnotations() {
        var strategy = new JpaStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.entitySource().contains("@Entity"));
        assertTrue(artifacts.entitySource().contains("@Table(name = \"CUSTOMER\")"));
        assertTrue(artifacts.entitySource().contains("@Id"));
        assertTrue(artifacts.repositorySource().contains("JpaRepository"));
    }

    @Test
    void emitVsamKeyUsesKeyFieldAsId() {
        var strategy = new JpaStrategy();
        var artifacts = strategy.emit(VSAM_KEY_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.entitySource().contains("private Long custId"));
    }

    @Test
    void configSnippetContainsTransactionBoundary() {
        var strategy = new JpaStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.configSnippet().contains("spring.jpa.hibernate.ddl-auto=update"));
        assertTrue(artifacts.configSnippet().contains("@Transactional"));
    }

    @Test
    void diagnosticsEmptyForSimpleAccess() {
        var strategy = new JpaStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.diagnostics().isEmpty());
    }
}
