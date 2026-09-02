package org.shark.renovatio.api.service;

import org.shark.renovatio.api.dto.DataAccessDto;
import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessClassifier;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.persistence.registry.PersistenceStrategyRegistry;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataAccessService {

    private final DataAccessClassifier classifier;
    private final PersistenceStrategyRegistry registry;

    public DataAccessService(DataAccessClassifier classifier, PersistenceStrategyRegistry registry) {
        this.classifier = classifier;
        this.registry = registry;
    }

    public List<DataAccessDto> getClassifiedDataAccesses(String projectId) {
        // Placeholder: in a real implementation, this would load the project's
        // semantic programs from the analysis results store.
        // For now, return empty list to indicate "no data yet analyzed".
        return List.of();
    }

    public List<DataAccessDto> classifyFromPrograms(
            List<org.shark.renovatio.semantic.ir.SemanticProgram> programs,
            MigrationProfiles.EffectiveProfile profile) {
        List<DataAccessDto> result = new ArrayList<>();
        Map<String, String> sourceStrategies = profile.profile().persistence().sourceStrategies() != null
                ? profile.profile().persistence().sourceStrategies() : Map.of();

        for (var program : programs) {
            List<DataAccessClassification> classifications = classifier.classify(program);
            for (DataAccessClassification classification : classifications) {
                String suggestedStrategy = suggestStrategy(classification.kind());
                String currentStrategy = sourceStrategies.getOrDefault(classification.id(), suggestedStrategy);

                result.add(new DataAccessDto(
                        classification.id(),
                        classification.kind().name(),
                        classification.resourceReference().orElse(null),
                        classification.confidence(),
                        suggestedStrategy,
                        currentStrategy,
                        new DataAccessDto.KeyShapeDto(classification.keyShape().fields()),
                        new DataAccessDto.RecordShapeDto(
                                classification.recordShape().fdName(),
                                classification.recordShape().table().orElse(null),
                                classification.recordShape().columns()),
                        classification.discriminatorValues().stream()
                                .map(d -> d.flag() + "=" + d.layoutName())
                                .toList()));
            }
        }
        return result;
    }

    private String suggestStrategy(DataAccessKind kind) {
        return switch (kind) {
            case EXEC_SQL -> "JPA";
            case VSAM_KEY, VSAM_SEQUENTIAL, SEQUENTIAL_FD -> "SPRING_DATA_JDBC";
            case FLAT_FILE_REDEFINES -> "JPA";
            case RESIDUAL -> "IN_MEMORY";
        };
    }
}
