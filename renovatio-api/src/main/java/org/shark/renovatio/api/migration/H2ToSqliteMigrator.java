package org.shark.renovatio.api.migration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One-way local development migration. It never mutates the H2 source or overwrites SQLite. */
public final class H2ToSqliteMigrator {
    private H2ToSqliteMigrator() { }

    public static void main(String[] args) throws Exception {
        if (args.length == 2 && "--normalize-sqlite-timestamps".equals(args[0])) {
            normalize(Path.of(args[1]).toAbsolutePath().normalize());
            return;
        }
        if (args.length != 2) throw new IllegalArgumentException("usage: <h2-jdbc-url> <sqlite-file>");
        Path targetFile = Path.of(args[1]).toAbsolutePath().normalize();
        if (Files.exists(targetFile)) throw new IllegalStateException("Refusing to overwrite existing SQLite file: " + targetFile);
        Files.createDirectories(targetFile.getParent());
        Class.forName("org.h2.Driver");
        Class.forName("org.sqlite.JDBC");
        try (Connection source = DriverManager.getConnection(args[0], "sa", "");
             Connection target = DriverManager.getConnection("jdbc:sqlite:" + targetFile)) {
            target.setAutoCommit(false);
            List<String> tables = tables(source.getMetaData());
            Map<String, Integer> expected = new LinkedHashMap<>();
            for (String table : tables) {
                List<Column> columns = columns(source.getMetaData(), table);
                create(target, table, columns, primaryKeys(source.getMetaData(), table));
                expected.put(table, copy(source, target, table, columns));
            }
            target.commit();
            for (Map.Entry<String, Integer> entry : expected.entrySet()) {
                int actual = count(target, entry.getKey());
                if (actual != entry.getValue()) throw new IllegalStateException(entry.getKey() + ": expected " + entry.getValue() + ", got " + actual);
                System.out.println(entry.getKey() + "=" + actual);
            }
        }
    }

    private static void normalize(Path targetFile) throws Exception {
        try (Connection target = DriverManager.getConnection("jdbc:sqlite:" + targetFile)) {
            for (String table : tables(target.getMetaData())) {
                for (Column column : columns(target.getMetaData(), table)) {
                    if (column.name().toLowerCase(java.util.Locale.ROOT).endsWith("at")) {
                        normalizeEpoch(target, table, column.name());
                    }
                }
            }
        }
    }
    private static void normalizeEpoch(Connection target, String table, String column) throws Exception { try (Statement s=target.createStatement()) { s.executeUpdate("UPDATE \""+table+"\" SET \""+column+"\"=strftime('%Y-%m-%d %H:%M:%f', \""+column+"\"/1000.0, 'unixepoch') WHERE \""+column+"\" GLOB '[0-9]*'"); } }

    private static List<String> tables(DatabaseMetaData meta) throws Exception {
        List<String> result = new ArrayList<>();
        try (ResultSet rows = meta.getTables(null, "PUBLIC", "%", new String[] { "TABLE" })) {
            while (rows.next()) result.add(rows.getString("TABLE_NAME"));
        }
        if (result.isEmpty()) try (ResultSet rows = meta.getTables(null, null, "%", new String[] { "TABLE" })) {
            while (rows.next()) result.add(rows.getString("TABLE_NAME"));
        }
        return result;
    }

    private static List<Column> columns(DatabaseMetaData meta, String table) throws Exception {
        List<Column> result = new ArrayList<>();
        try (ResultSet rows = meta.getColumns(null, "PUBLIC", table, "%")) {
            while (rows.next()) result.add(new Column(rows.getString("COLUMN_NAME"), rows.getInt("DATA_TYPE"), rows.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
        }
        if (result.isEmpty()) try (ResultSet rows = meta.getColumns(null, null, table, "%")) {
            while (rows.next()) result.add(new Column(rows.getString("COLUMN_NAME"), rows.getInt("DATA_TYPE"), rows.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls));
        }
        return result;
    }

    private static List<String> primaryKeys(DatabaseMetaData meta, String table) throws Exception {
        Map<Short, String> ordered = new java.util.TreeMap<>();
        try (ResultSet rows = meta.getPrimaryKeys(null, "PUBLIC", table)) {
            while (rows.next()) ordered.put(rows.getShort("KEY_SEQ"), rows.getString("COLUMN_NAME"));
        }
        return List.copyOf(ordered.values());
    }

    private static void create(Connection target, String table, List<Column> columns, List<String> primaryKeys) throws Exception {
        List<String> definitions = new ArrayList<>();
        for (Column column : columns) definitions.add(q(column.name()) + " " + sqliteType(column.type()) + (column.nullable() ? "" : " NOT NULL"));
        if (!primaryKeys.isEmpty()) definitions.add("PRIMARY KEY (" + primaryKeys.stream().map(H2ToSqliteMigrator::q).reduce((a, b) -> a + ", " + b).orElseThrow() + ")");
        try (Statement statement = target.createStatement()) { statement.execute("CREATE TABLE " + q(table) + " (" + String.join(", ", definitions) + ")"); }
    }

    private static int copy(Connection source, Connection target, String table, List<Column> columns) throws Exception {
        String names = columns.stream().map(column -> q(column.name())).reduce((a, b) -> a + ", " + b).orElseThrow();
        String marks = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        int count = 0;
        try (Statement read = source.createStatement(); ResultSet rows = read.executeQuery("SELECT " + names + " FROM " + q(table));
             PreparedStatement write = target.prepareStatement("INSERT INTO " + q(table) + " (" + names + ") VALUES (" + marks + ")")) {
            while (rows.next()) {
                for (int i = 0; i < columns.size(); i++) {
                    int type = columns.get(i).type();
                    Object sourceValue = rows.getObject(i + 1);
                    if (type == Types.TIMESTAMP || type == Types.TIMESTAMP_WITH_TIMEZONE) write.setTimestamp(i + 1, rows.getTimestamp(i + 1));
                    else if (columns.get(i).name().toLowerCase(java.util.Locale.ROOT).endsWith("at") && sourceValue instanceof Number value) write.setTimestamp(i + 1, new java.sql.Timestamp(value.longValue()));
                    else if (sourceValue instanceof java.sql.Clob clob) write.setString(i + 1, clob.getSubString(1, (int) clob.length()));
                    else write.setObject(i + 1, sourceValue);
                }
                write.executeUpdate(); count++;
            }
        }
        return count;
    }

    private static int count(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + q(table))) { rows.next(); return rows.getInt(1); }
    }

    private static String sqliteType(int type) {
        return switch (type) {
            case Types.BIGINT -> "BIGINT";
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BOOLEAN, Types.BIT -> "INTEGER";
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> "REAL";
            case Types.DECIMAL, Types.NUMERIC -> "NUMERIC";
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> "BLOB";
            default -> "TEXT";
        };
    }

    private static String q(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
    private record Column(String name, int type, boolean nullable) { }
}
