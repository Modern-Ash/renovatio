package org.shark.renovatio.persistence.strategy;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * In-memory test stub strategy.
 * Generates a Map-backed repository for test environments.
 */
public final class InMemoryStrategy implements PersistenceStrategy {

    @Override
    public boolean supports(DataAccessClassification classification, MigrationProfile.Language target) {
        return target == MigrationProfile.Language.JAVA;
    }

    @Override
    public PersistenceArtifacts emit(DataAccessClassification classification,
                                     MigrationProfiles.EffectiveProfile profile) {
        String entityName = deriveEntityName(classification);
        String repoName = entityName + "Repository";

        String entitySource = generateEntitySource(entityName, classification);
        String repositorySource = generateRepositorySource(entityName, repoName);
        String configSnippet = generateConfigSnippet(entityName);

        return new PersistenceArtifacts(
                entityName, entitySource,
                repoName, repositorySource,
                configSnippet,
                List.of());
    }

    private String deriveEntityName(DataAccessClassification classification) {
        String ref = classification.resourceReference().orElse("UnknownEntity");
        return toPascalCase(ref);
    }

    private String generateEntitySource(String entityName, DataAccessClassification classification) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.Map;\n");
        sb.append("import java.util.HashMap;\n");
        sb.append("import java.util.Optional;\n");
        sb.append("import java.util.List;\n\n");
        sb.append("public class ").append(entityName).append(" {\n\n");

        if (!classification.keyShape().isNone()) {
            for (String field : classification.keyShape().fields()) {
                sb.append("    private String ").append(toCamelCase(field)).append(";\n");
            }
        } else {
            sb.append("    private String id;\n");
        }

        sb.append("\n    public ").append(entityName).append("() {}\n\n");
        sb.append("    // TODO: add fields from record shape\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generateRepositorySource(String entityName, String repoName) {
        return """
                import java.util.Map;
                import java.util.HashMap;
                import java.util.Optional;
                import java.util.List;
                import java.util.stream.Collectors;

                public class %s {
                    private final Map<String, %s> store = new HashMap<>();

                    public List<%s> findAll() {
                        return List.copyOf(store.values());
                    }

                    public Optional<%s> findById(String id) {
                        return Optional.ofNullable(store.get(id));
                    }

                    public %s save(%s entity) {
                        // TODO: extract id from entity
                        store.put("id", entity);
                        return entity;
                    }

                    public void deleteById(String id) {
                        store.remove(id);
                    }
                }
                """.formatted(repoName, entityName, entityName, entityName, entityName, entityName);
    }

    private String generateConfigSnippet(String entityName) {
        return "# In-memory repository for " + entityName + " (test stub)";
    }

    private static String toPascalCase(String input) {
        if (input == null || input.isBlank()) return "UnknownEntity";
        String[] parts = input.split("[\\s_-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private static String toCamelCase(String input) {
        String pascal = toPascalCase(input);
        if (pascal.isEmpty()) return pascal;
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}
