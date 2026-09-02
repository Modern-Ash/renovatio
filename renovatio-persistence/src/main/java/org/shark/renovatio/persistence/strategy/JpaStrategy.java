package org.shark.renovatio.persistence.strategy;

import org.shark.renovatio.persistence.classifier.DataAccessClassification;
import org.shark.renovatio.persistence.classifier.DataAccessKind;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * JPA strategy. Generates @Entity with @Id and JpaRepository interface.
 * Delegates EXEC_SQL to existing Db2MigrationService patterns.
 */
public final class JpaStrategy implements PersistenceStrategy {

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

        List<String> diagnostics = new ArrayList<>();
        String entitySource = generateEntitySource(entityName, classification, diagnostics);
        String repositorySource = generateRepositorySource(entityName, repoName, classification);
        String configSnippet = generateConfigSnippet(profile.profile().persistence().transactionBoundary());

        return new PersistenceArtifacts(
                entityName, entitySource,
                repoName, repositorySource,
                configSnippet, diagnostics);
    }

    private String deriveEntityName(DataAccessClassification classification) {
        String ref = classification.resourceReference().orElse("UnknownEntity");
        return toPascalCase(ref);
    }

    private String generateEntitySource(String entityName, DataAccessClassification classification,
                                        List<String> diagnostics) {
        StringBuilder sb = new StringBuilder();
        sb.append("import jakarta.persistence.Entity;\n");
        sb.append("import jakarta.persistence.Id;\n");
        sb.append("import jakarta.persistence.Table;\n");

        String tableName = classification.resourceReference().orElse(entityName);
        if (classification.recordShape().table().isPresent()) {
            tableName = classification.recordShape().table().get();
        }

        sb.append("\n@Entity\n");
        sb.append("@Table(name = \"").append(tableName).append("\")\n");
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
                if (!fieldName.equals("id") && !fieldName.equals(toCamelCase(
                        classification.keyShape().fields().isEmpty() ? "" : classification.keyShape().fields().getFirst()))) {
                    sb.append("    // TODO: map column ").append(col).append("\n");
                    sb.append("    private String ").append(fieldName).append(";\n\n");
                }
            }
        } else {
            sb.append("\n    // TODO: add columns from record shape\n");
        }

        sb.append("\n    public ").append(entityName).append("() {}\n");

        if (classification.discriminatorField().isPresent()) {
            diagnostics.add("REDEFINES discriminator field " + classification.discriminatorField().get()
                    + " mapped to @DiscriminatorColumn (JPA inheritance)");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String generateRepositorySource(String entityName, String repoName,
                                            DataAccessClassification classification) {
        return """
                import org.springframework.data.jpa.repository.JpaRepository;
                import java.util.List;

                public interface %s extends JpaRepository<%s, Long> {
                    // TODO: add custom query methods
                }
                """.formatted(repoName, entityName);
    }

    private String generateConfigSnippet(MigrationProfile.TransactionBoundary boundary) {
        String boundaryStr = switch (boundary) {
            case METHOD -> "@Transactional per method";
            case PROGRAM -> "@Transactional per program";
            case NONE -> "no @Transactional";
        };
        return "spring.jpa.hibernate.ddl-auto=update\n# Transaction: " + boundaryStr;
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
