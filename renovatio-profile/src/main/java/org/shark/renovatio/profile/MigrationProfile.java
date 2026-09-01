package org.shark.renovatio.profile;

import java.util.LinkedHashMap;
import java.util.Map;

/** Versioned project overlay. Null sections/fields mean "inherit" until fully resolved. */
public record MigrationProfile(
        String schemaVersion,
        Map<String, Object> extensions,
        Target target,
        Architecture architecture,
        Runtime runtime,
        Persistence persistence,
        Style style,
        Llm llm) {

    public MigrationProfile {
        extensions = extensions == null ? null : Map.copyOf(new LinkedHashMap<>(extensions));
    }

    public enum Language { JAVA, NODE, PYTHON }
    public enum ArchitectureStyle { TRANSACTION_SCRIPT, LAYERED_MVC, HEXAGONAL }
    public enum ModuleGrouping { BY_PROGRAM, BY_DOMAIN, SINGLE_MODULE }
    public enum Framework { SPRING_BOOT, NONE }
    public enum PersistenceStrategy { JPA, SPRING_DATA_JDBC, IN_MEMORY }
    public enum TransactionBoundary { METHOD, PROGRAM, NONE }
    public enum NumericPolicy { BIGDECIMAL, SCALED_LONG }
    public enum Nullability { NON_NULL_BY_DEFAULT, NULLABLE }
    public enum ErrorHandling { EXCEPTIONS, RESULT_OBJECT }
    public enum Naming { JAVA_BEANS, FLUENT }

    public record Target(Language language, String languageVersion) { }
    public record Architecture(ArchitectureStyle style, ModuleGrouping moduleGrouping) { }
    public record Runtime(Framework framework) { }
    public record Persistence(PersistenceStrategy defaultStrategy,
                              TransactionBoundary transactionBoundary) { }
    public record Style(NumericPolicy numericPolicy, Nullability nullability,
                        ErrorHandling errorHandling, Naming naming) { }
    public record Llm(Boolean enabled, Boolean suggestDecisions,
                      Integer maxSuggestionsPerRun) { }
}
