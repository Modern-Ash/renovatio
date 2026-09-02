package org.shark.renovatio.persistence.strategy;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Spring Data JDBC strategy. Generates CrudRepository instead of JpaRepository.
 * No @Transactional by default (Spring Data JDBC manages transactions internally).
 */
public final class SpringDataJdbcStrategy implements PersistenceStrategy {

    @Override
    public boolean supports(DataAccessClassification classification, MigrationProfile.Language target) {
        return target == MigrationProfile.Language.JAVA
                && classification.kind() != DataAccessKind.RESIDUAL;
    }

    @Override
    public PersistenceArtifacts emit(DataAccessClassification classification,
                                     MigrationProfiles.EffectiveProfile profile) {
        String entityName = deriveEntityName(classification);
        String repoName = entityName + "Repository";

        String entitySource = generateEntitySource(entityName, classification);
        String repositorySource = generateRepositorySource(entityName, repoName);
        String configSnippet = generateConfigSnippet();

        return new PersistenceArtifacts(
                entityName, entitySource,
                repoName, repositorySource,
                configSnippet, List.of());
    }

    private String deriveEntityName(DataAccessClassification classification) {
        String ref = classification.resourceReference().orElse("UnknownEntity");
        return toPascalCase(ref);
    }

    private String generateEntitySource(String entityName, DataAccessClassification classification) {
        StringBuilder sb = new StringBuilder();
        sb.append("import org.springframework.data.annotation.Id;\n");
        sb.append("import org.springframework.data.relational.core.mapping.Table;\n");
        sb.append("import org.springframework.data.relational.core.mapping.Column;\n\n");

        String tableName = classification.resourceReference().orElse(entityName);
        if (classification.recordShape().table().isPresent()) {
            tableName = classification.recordShape().table().get();
        }

        sb.append("@Table(\"").append(tableName).append("\")\n");
        sb.append("public class ").append(entityName).append(" {\n\n");
        sb.append("    @Id\n");

        if (!classification.keyShape().isNone()) {
            String keyField = classification.keyShape().fields().getFirst();
            sb.append("    private Long ").append(toCamelCase(keyField)).append(";\n");
        } else {
            sb.append("    private Long id;\n");
        }

        if (!classification.recordShape().columns().isEmpty()) {
            sb.append("\n");
            for (String col : classification.recordShape().columns()) {
                String fieldName = toCamelCase(col);
                sb.append("    @Column(\"").append(col).append("\")\n");
                sb.append("    private String ").append(fieldName).append(";\n\n");
            }
        } else {
            sb.append("\n    // TODO: add columns from record shape\n");
        }

        sb.append("\n    public ").append(entityName).append("() {}\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String generateRepositorySource(String entityName, String repoName) {
        return """
                import org.springframework.data.repository.CrudRepository;
                import java.util.List;

                public interface %s extends CrudRepository<%s, Long> {
                    // TODO: add custom query methods
                }
                """.formatted(repoName, entityName);
    }

    private String generateConfigSnippet() {
        return "spring.data.jdbc.repositories.type=imperative";
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
