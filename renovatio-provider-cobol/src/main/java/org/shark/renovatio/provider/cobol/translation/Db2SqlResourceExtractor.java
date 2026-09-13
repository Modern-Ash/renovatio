package org.shark.renovatio.provider.cobol.translation;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class Db2SqlResourceExtractor {
    private static final Pattern HOST_VARIABLE = Pattern.compile(":[A-Za-z][A-Za-z0-9_-]*");
    private static final Pattern SQL_FROM = Pattern.compile("\\bFROM\\s+([A-Z0-9_.$#@-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_INSERT_INTO = Pattern.compile("\\bINSERT\\s+INTO\\s+([A-Z0-9_.$#@-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_UPDATE = Pattern.compile("\\bUPDATE\\s+([A-Z0-9_.$#@-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_DELETE_FROM = Pattern.compile("\\bDELETE\\s+FROM\\s+([A-Z0-9_.$#@-]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SQL_MERGE_INTO = Pattern.compile("\\bMERGE\\s+INTO\\s+([A-Z0-9_.$#@-]+)",
            Pattern.CASE_INSENSITIVE);

    Optional<String> physicalResource(String operation, String sql) {
        if (operation == null || operation.isBlank() || sql == null || sql.isBlank()) return Optional.empty();
        Optional<String> parsed = parseTables(sql).or(() -> parseCursorSelect(sql));
        if (parsed.isPresent()) return parsed;
        return fallbackResource(operation, sql);
    }

    private Optional<String> parseTables(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(normalizeHostVariables(sql));
            List<String> tables = new TablesNamesFinder<String>().getTableList(statement);
            return firstCleanTable(tables);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private Optional<String> parseCursorSelect(String sql) {
        String upper = sql.toUpperCase(Locale.ROOT);
        int selectIndex = upper.indexOf(" SELECT ");
        if (selectIndex < 0) return Optional.empty();
        return parseTables(sql.substring(selectIndex + 1));
    }

    private Optional<String> firstCleanTable(List<String> tables) {
        Set<String> ordered = new LinkedHashSet<>(tables == null ? List.of() : tables);
        return ordered.stream()
                .map(Db2SqlResourceExtractor::cleanSqlIdentifier)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static String normalizeHostVariables(String sql) {
        return HOST_VARIABLE.matcher(sql).replaceAll("?");
    }

    private static Optional<String> fallbackResource(String operation, String sql) {
        Pattern pattern = switch (operation.toUpperCase(Locale.ROOT)) {
            case "SELECT" -> SQL_FROM;
            case "INSERT" -> SQL_INSERT_INTO;
            case "UPDATE" -> SQL_UPDATE;
            case "DELETE" -> SQL_DELETE_FROM;
            case "MERGE" -> SQL_MERGE_INTO;
            case "DECLARE" -> SQL_FROM;
            default -> null;
        };
        if (pattern == null) return Optional.empty();
        Matcher matcher = pattern.matcher(sql);
        if (!matcher.find()) return Optional.empty();
        String resource = cleanSqlIdentifier(matcher.group(1));
        return resource.isBlank() ? Optional.empty() : Optional.of(resource);
    }

    private static String cleanSqlIdentifier(String value) {
        return value == null ? "" : value.strip().replaceAll("[,;)]$", "");
    }
}
