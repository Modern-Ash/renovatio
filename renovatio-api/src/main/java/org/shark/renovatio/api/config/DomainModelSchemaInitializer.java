package org.shark.renovatio.api.config;

import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** Provisions the SQLite schema while production Hibernate remains ddl-auto=none. */
@Component
public class DomainModelSchemaInitializer implements ApplicationRunner {
    private final DataSource dataSource;

    public DomainModelSchemaInitializer(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public void run(ApplicationArguments args) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/sqlite/schema.sql"));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
