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

class SpringDataJdbcStrategyTest {

    private static final DataAccessClassification EXEC_SQL_CLASSIFICATION = new DataAccessClassification(
            "id1", "TESTPGM", DataAccessKind.EXEC_SQL,
            Optional.of("CUSTOMER-MASTER"),
            DataAccessClassification.KeyShape.NONE,
            new DataAccessClassification.RecordShape(null, Optional.of("CUSTOMER"), List.of("ID", "NAME")),
            Optional.empty(), List.of(), 1.0, List.of(),
            new DataAccessClassification.ClassifierProvenance("src/test.cbl", "abc123", "COBOL"));

    private static final MigrationProfiles.EffectiveProfile DEFAULT_PROFILE =
            MigrationProfiles.effective(MigrationProfiles.defaults(), Map.of(), Map.of(), List.of());

    @Test
    void supportsJavaOnly() {
        var strategy = new SpringDataJdbcStrategy();
        assertTrue(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.JAVA));
        assertFalse(strategy.supports(EXEC_SQL_CLASSIFICATION, MigrationProfile.Language.NODE));
    }

    @Test
    void emitProducesCrudRepository() {
        var strategy = new SpringDataJdbcStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.repositorySource().contains("CrudRepository"));
        assertFalse(artifacts.repositorySource().contains("JpaRepository"));
    }

    @Test
    void emitProducesEntityWithSpringDataAnnotations() {
        var strategy = new SpringDataJdbcStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.entitySource().contains("@Table(\"CUSTOMER\")"));
        assertTrue(artifacts.entitySource().contains("@Id"));
        assertTrue(artifacts.entitySource().contains("@Column"));
    }

    @Test
    void configSnippetContainsJdbcType() {
        var strategy = new SpringDataJdbcStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.configSnippet().contains("spring.data.jdbc.repositories.type=imperative"));
    }

    @Test
    void diagnosticsEmpty() {
        var strategy = new SpringDataJdbcStrategy();
        var artifacts = strategy.emit(EXEC_SQL_CLASSIFICATION, DEFAULT_PROFILE);

        assertTrue(artifacts.diagnostics().isEmpty());
    }
}
