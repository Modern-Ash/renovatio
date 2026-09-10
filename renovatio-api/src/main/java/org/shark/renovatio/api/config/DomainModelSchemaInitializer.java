package org.shark.renovatio.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Provisions additive Workbench tables while production Hibernate remains ddl-auto=none. */
@Component
public class DomainModelSchemaInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DomainModelSchemaInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS project_domain_model_versions (
                    id VARCHAR(512) PRIMARY KEY,
                    project_id VARCHAR(255) NOT NULL,
                    revision BIGINT NOT NULL,
                    canonical_hash VARCHAR(64) NOT NULL,
                    model_json TEXT NOT NULL,
                    saved_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_domain_model_project_revision UNIQUE (project_id, revision)
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS domain_suggestion_decisions (
                    id VARCHAR(768) PRIMARY KEY,
                    project_id VARCHAR(255) NOT NULL,
                    suggestion_id VARCHAR(512) NOT NULL,
                    action VARCHAR(16) NOT NULL,
                    model_revision BIGINT NOT NULL,
                    decided_at TIMESTAMP NOT NULL
                )
                """);
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS project_architecture_profile_versions (
                    id VARCHAR(512) PRIMARY KEY,
                    project_id VARCHAR(255) NOT NULL,
                    revision BIGINT NOT NULL,
                    canonical_hash VARCHAR(64) NOT NULL,
                    profile_json TEXT NOT NULL,
                    saved_at TIMESTAMP NOT NULL,
                    CONSTRAINT uk_architecture_profile_project_revision UNIQUE (project_id, revision)
                )
                """);
    }
}
