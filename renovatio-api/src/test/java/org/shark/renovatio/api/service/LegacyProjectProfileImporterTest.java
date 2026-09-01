package org.shark.renovatio.api.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyProjectProfileImporterTest {
    @Test
    void absentLegacyColumnsAreANoop() throws Exception {
        DataSource dataSource = database("absent");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table projects (id varchar(64) primary key)");
        jdbc.update("insert into projects (id) values (?)", "p1");
        MemoryProfiles profiles = new MemoryProfiles();

        importer(dataSource, profiles).run(null);

        assertThat(profiles.values).isEmpty();
    }

    @Test
    void importsRecognizedValuesOnceAndNeverOverwritesExplicitDestinations() throws Exception {
        DataSource dataSource = database("recognized");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table projects (id varchar(64) primary key, java_output_path varchar(255), javaPackage varchar(255), javaArchitecture varchar(255))");
        jdbc.update("insert into projects values (?,?,?,?)", "p1", "generated", "com.acme", "ports-and-adapters");
        MemoryProfiles profiles = new MemoryProfiles();

        LegacyProjectProfileImporter importer = importer(dataSource, profiles);
        importer.run(null);
        importer.run(null);

        ProfileStore.VersionedProfile stored = profiles.find("p1").orElseThrow();
        assertThat(stored.revision()).isEqualTo(1);
        assertThat(stored.profile().extensions()).containsEntry("renovatio.java.outputPath", "generated")
                .containsEntry("renovatio.java.package", "com.acme");
        assertThat(stored.profile().architecture().style()).isEqualTo(MigrationProfile.ArchitectureStyle.HEXAGONAL);
    }

    @Test
    void preservesUnknownArchitectureAndExistingProfileValues() throws Exception {
        DataSource dataSource = database("unknown");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table projects (id varchar(64) primary key, javaOutputPath varchar(255), javaArchitecture varchar(255))");
        jdbc.update("insert into projects values (?,?,?)", "p1", "legacy", "custom onion");
        MemoryProfiles profiles = new MemoryProfiles();
        MigrationProfile explicit = new MigrationProfile("1", Map.of("renovatio.java.outputPath", "explicit"),
                null, null, null, null, null, null);
        profiles.values.put("p1", new ProfileStore.VersionedProfile(explicit, 4));

        importer(dataSource, profiles).run(null);

        ProfileStore.VersionedProfile stored = profiles.find("p1").orElseThrow();
        assertThat(stored.revision()).isEqualTo(5);
        assertThat(stored.profile().extensions()).containsEntry("renovatio.java.outputPath", "explicit")
                .containsEntry("renovatio.legacy.javaArchitecture", "custom onion");
    }

    private static LegacyProjectProfileImporter importer(DataSource dataSource, ProfileStore profiles) {
        return new LegacyProjectProfileImporter(dataSource, profiles, new DataSourceTransactionManager(dataSource));
    }

    private static DataSource database(String name) {
        return new DriverManagerDataSource("jdbc:h2:mem:legacy_" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    private static final class MemoryProfiles implements ProfileStore {
        private final Map<String, VersionedProfile> values = new LinkedHashMap<>();
        @Override public Optional<VersionedProfile> find(String projectId) { return Optional.ofNullable(values.get(projectId)); }
        @Override public VersionedProfile replace(String projectId, MigrationProfile profile, long expectedRevision) {
            VersionedProfile current = values.get(projectId);
            long revision = current == null ? 0 : current.revision();
            if (revision != expectedRevision) throw new AssertionError("stale test revision");
            VersionedProfile next = new VersionedProfile(profile, revision + 1);
            values.put(projectId, next);
            return next;
        }
        @Override public void deleteProject(String projectId) { values.remove(projectId); }
    }
}
