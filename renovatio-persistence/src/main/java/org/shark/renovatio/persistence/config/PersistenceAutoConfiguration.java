package org.shark.renovatio.persistence.config;

import org.shark.renovatio.persistence.classifier.DataAccessClassifier;
import org.shark.renovatio.persistence.registry.PersistenceStrategyRegistry;
import org.shark.renovatio.persistence.strategy.PersistenceStrategy;
import org.shark.renovatio.profile.MigrationProfile;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class PersistenceAutoConfiguration {

    @Bean
    public DataAccessClassifier dataAccessClassifier() {
        return new DataAccessClassifier();
    }

    @Bean
    public PersistenceStrategyRegistry persistenceStrategyRegistry(
            Set<PersistenceStrategy> strategySet) {
        return new PersistenceStrategyRegistry(strategySet, MigrationProfile.PersistenceStrategy.IN_MEMORY);
    }
}
