package org.shark.renovatio.persistence.registry;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.persistence.strategy.PersistenceArtifacts;
import org.shark.renovatio.persistence.strategy.PersistenceStrategy;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.*;

/**
 * SPI registry for persistence strategies.
 * Resolves the strategy for a given DataAccessKind + target language.
 */
public final class PersistenceStrategyRegistry {

    private final Map<StrategyKey, PersistenceStrategy> strategies;
    private final MigrationProfile.PersistenceStrategy defaultStrategy;

    public PersistenceStrategyRegistry(Set<PersistenceStrategy> strategySet,
                                       MigrationProfile.PersistenceStrategy defaultStrategy) {
        this.strategies = new LinkedHashMap<>();
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy");

        for (PersistenceStrategy strategy : strategySet) {
            // Index by all kind+target combinations the strategy supports
            for (DataAccessKind kind : DataAccessKind.values()) {
                for (MigrationProfile.Language target : MigrationProfile.Language.values()) {
                    // Create a dummy classification to test support
                    var dummy = createDummyClassification(kind);
                    if (strategy.supports(dummy, target)) {
                        var key = new StrategyKey(kind, target);
                        strategies.put(key, strategy);
                    }
                }
            }
        }
    }

    /**
     * Resolve the strategy for a classification + target language.
     * Uses per-source override if present, otherwise falls back to default strategy mapping.
     */
    public PersistenceStrategy resolve(DataAccessClassification classification,
                                       MigrationProfile.Language target,
                                       Map<String, String> sourceStrategies) {
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(target, "target");

        // Check per-source override
        if (sourceStrategies != null && sourceStrategies.containsKey(classification.id())) {
            String overrideName = sourceStrategies.get(classification.id());
            MigrationProfile.PersistenceStrategy override = parseStrategyName(overrideName);
            PersistenceStrategy resolved = findStrategy(override, classification.kind(), target);
            if (resolved != null) return resolved;
        }

        // Use default strategy
        PersistenceStrategy resolved = findStrategy(defaultStrategy, classification.kind(), target);
        if (resolved != null) return resolved;

        throw new PersistenceStrategyUnavailableException(
                classification.kind(), target, availableTargets(classification.kind()));
    }

    /**
     * Emit artifacts for a classification using the resolved strategy.
     */
    public PersistenceArtifacts emit(DataAccessClassification classification,
                                     MigrationProfiles.EffectiveProfile profile) {
        Map<String, String> sourceStrategies = profile.profile().persistence().sourceStrategies() != null
                ? profile.profile().persistence().sourceStrategies() : Map.of();
        PersistenceStrategy strategy = resolve(classification, profile.profile().target().language(), sourceStrategies);
        return strategy.emit(classification, profile);
    }

    private PersistenceStrategy findStrategy(MigrationProfile.PersistenceStrategy name,
                                             DataAccessKind kind, MigrationProfile.Language target) {
        var key = new StrategyKey(kind, target);
        PersistenceStrategy strategy = strategies.get(key);
        if (strategy != null && matchesStrategyName(strategy, name)) {
            return strategy;
        }
        // Fallback: find any strategy for this kind+target
        return strategy;
    }

    private boolean matchesStrategyName(PersistenceStrategy strategy, MigrationProfile.PersistenceStrategy name) {
        return switch (name) {
            case JPA -> strategy.getClass().getSimpleName().equals("JpaStrategy");
            case SPRING_DATA_JDBC -> strategy.getClass().getSimpleName().equals("SpringDataJdbcStrategy");
            case IN_MEMORY -> strategy.getClass().getSimpleName().equals("InMemoryStrategy");
        };
    }

    private MigrationProfile.PersistenceStrategy parseStrategyName(String name) {
        try {
            return MigrationProfile.PersistenceStrategy.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown persistence strategy: " + name
                    + ". Valid values: JPA, SPRING_DATA_JDBC, IN_MEMORY");
        }
    }

    private List<MigrationProfile.Language> availableTargets(DataAccessKind kind) {
        List<MigrationProfile.Language> available = new ArrayList<>();
        for (MigrationProfile.Language target : MigrationProfile.Language.values()) {
            if (strategies.containsKey(new StrategyKey(kind, target))) {
                available.add(target);
            }
        }
        return available;
    }

    private DataAccessClassification createDummyClassification(DataAccessKind kind) {
        return new DataAccessClassification(
                "dummy", "DUMMY", kind,
                Optional.empty(),
                DataAccessClassification.KeyShape.NONE,
                DataAccessClassification.RecordShape.UNKNOWN,
                Optional.empty(), List.of(), 0.0, List.of(),
                new DataAccessClassification.ClassifierProvenance("dummy", "abc" + "0".repeat(61), "COBOL"));
    }

    private record StrategyKey(DataAccessKind kind, MigrationProfile.Language target) { }
}
