package org.shark.renovatio.api.service;

import org.shark.renovatio.decisions.ProfileStore;
import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.MigrationProfiles;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Imports optional pre-F1 Java settings without requiring or removing legacy columns. */
@Component
public class LegacyProjectProfileImporter implements ApplicationRunner {
    private final DataSource dataSource;
    private final ProfileStore profiles;
    private final TransactionTemplate transactions;

    public LegacyProjectProfileImporter(DataSource dataSource, ProfileStore profiles,
                                        PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.profiles = profiles;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        Optional<LegacyColumns> detected = detectColumns();
        if (detected.isEmpty() || !detected.get().hasLegacyValues()) return;
        for (LegacyRow row : readRows(detected.get())) {
            transactions.executeWithoutResult(ignored -> importRow(row));
        }
    }

    private Optional<LegacyColumns> detectColumns() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, String> columns = new LinkedHashMap<>();
            String table = null;
            try (ResultSet result = metadata.getColumns(connection.getCatalog(), null, null, null)) {
                while (result.next()) {
                    String tableName = result.getString("TABLE_NAME");
                    if (!"projects".equalsIgnoreCase(tableName)) continue;
                    table = tableName;
                    String column = result.getString("COLUMN_NAME");
                    columns.put(normalizeIdentifier(column), column);
                }
            }
            if (table == null || !columns.containsKey("id")) return Optional.empty();
            return Optional.of(new LegacyColumns(table, columns.get("id"),
                    columns.get("javaoutputpath"), columns.get("javapackage"),
                    columns.get("javaarchitecture"), metadata.getIdentifierQuoteString()));
        }
    }

    private List<LegacyRow> readRows(LegacyColumns columns) {
        List<String> selected = new ArrayList<>();
        selected.add(columns.id());
        if (columns.outputPath() != null) selected.add(columns.outputPath());
        if (columns.javaPackage() != null) selected.add(columns.javaPackage());
        if (columns.architecture() != null) selected.add(columns.architecture());
        String sql = "select " + selected.stream().map(value -> quote(columns.quote(), value))
                .collect(java.util.stream.Collectors.joining(","))
                + " from " + quote(columns.quote(), columns.table());
        return new JdbcTemplate(dataSource).query(sql, (result, rowNumber) -> new LegacyRow(
                result.getString(columns.id()), value(result, columns.outputPath()),
                value(result, columns.javaPackage()), value(result, columns.architecture())));
    }

    private void importRow(LegacyRow row) {
        ProfileStore.VersionedProfile current = profiles.find(row.projectId())
                .orElse(new ProfileStore.VersionedProfile(MigrationProfiles.emptyOverlay(), 0));
        MigrationProfile original = current.profile();
        Map<String, Object> extensions = new LinkedHashMap<>(original.extensions());
        putIfPresent(extensions, "renovatio.java.outputPath", row.outputPath());
        putIfPresent(extensions, "renovatio.java.package", row.javaPackage());

        MigrationProfile.Architecture architecture = original.architecture();
        MigrationProfile.ArchitectureStyle recognized = recognizedArchitecture(row.architecture());
        boolean styleMissing = architecture == null || architecture.style() == null;
        if (styleMissing && recognized != null) {
            architecture = new MigrationProfile.Architecture(recognized,
                    architecture == null ? null : architecture.moduleGrouping());
        } else if (styleMissing && nonBlank(row.architecture())) {
            extensions.putIfAbsent("renovatio.legacy.javaArchitecture", row.architecture());
        }

        MigrationProfile imported = new MigrationProfile(original.schemaVersion(), extensions,
                original.target(), architecture, original.runtime(), original.persistence(),
                original.style(), original.llm());
        if (!imported.equals(original)) profiles.replace(row.projectId(), imported, current.revision());
    }

    private static void putIfPresent(Map<String, Object> target, String key, String value) {
        if (nonBlank(value)) target.putIfAbsent(key, value);
    }

    private static MigrationProfile.ArchitectureStyle recognizedArchitecture(String value) {
        if (!nonBlank(value)) return null;
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        return switch (normalized) {
            case "TRANSACTION_SCRIPT" -> MigrationProfile.ArchitectureStyle.TRANSACTION_SCRIPT;
            case "LAYERED", "MVC", "LAYERED_MVC" -> MigrationProfile.ArchitectureStyle.LAYERED_MVC;
            case "HEXAGONAL", "PORTS_AND_ADAPTERS" -> MigrationProfile.ArchitectureStyle.HEXAGONAL;
            default -> null;
        };
    }

    private static String value(ResultSet result, String column) throws SQLException {
        return column == null ? null : result.getString(column);
    }

    private static String normalizeIdentifier(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String quote(String quote, String identifier) {
        String marker = quote == null ? "" : quote.trim();
        return marker.isEmpty() ? identifier : marker + identifier.replace(marker, marker + marker) + marker;
    }

    private static boolean nonBlank(String value) { return value != null && !value.isBlank(); }

    private record LegacyColumns(String table, String id, String outputPath, String javaPackage,
                                 String architecture, String quote) {
        boolean hasLegacyValues() {
            return outputPath != null || javaPackage != null || architecture != null;
        }
    }
    private record LegacyRow(String projectId, String outputPath, String javaPackage, String architecture) {
        LegacyRow { Objects.requireNonNull(projectId, "projectId"); }
    }
}
