package org.shark.renovatio.architecture;

import java.text.Normalizer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Validated project-level architecture grouping hints. */
public record GroupingConfiguration(String singleModuleName,
                                    Map<String, String> manualModules,
                                    Map<String, String> domainCopybooks,
                                    Map<String, String> prefixModules) {
    public GroupingConfiguration {
        singleModuleName = singleModuleName == null || singleModuleName.isBlank()
                ? "migration" : ArchitectureSupport.moduleName(singleModuleName);
        manualModules = normalized(manualModules, KeyKind.PROGRAM);
        domainCopybooks = normalized(domainCopybooks, KeyKind.COPYBOOK);
        prefixModules = normalized(prefixModules, KeyKind.PREFIX);
    }

    public static GroupingConfiguration empty() {
        return new GroupingConfiguration("migration", Map.of(), Map.of(), Map.of());
    }

    /** Parses the F1 open extension namespace without retaining caller-owned maps. */
    public static GroupingConfiguration fromExtensions(Map<String, Object> extensions) {
        if (extensions == null || !extensions.containsKey("renovatio.architecture")) return empty();
        Object raw = extensions.get("renovatio.architecture");
        if (!(raw instanceof Map<?, ?> values)) {
            throw new IllegalArgumentException("renovatio.architecture must be an object");
        }
        String single = optionalString(values.get("singleModuleName"), "singleModuleName");
        return new GroupingConfiguration(single, stringMap(values.get("manualModules"), "manualModules"),
                stringMap(values.get("domainCopybooks"), "domainCopybooks"),
                stringMap(values.get("prefixModules"), "prefixModules"));
    }

    private static String optionalString(Object value, String name) {
        if (value == null) return null;
        if (!(value instanceof String string)) throw new IllegalArgumentException(name + " must be a string");
        return string;
    }

    private static Map<String, String> stringMap(Object value, String name) {
        if (value == null) return Map.of();
        if (!(value instanceof Map<?, ?> raw)) throw new IllegalArgumentException(name + " must be an object");
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((key, item) -> {
            if (!(key instanceof String stringKey) || !(item instanceof String stringValue)) {
                throw new IllegalArgumentException(name + " values must be strings");
            }
            result.put(stringKey, stringValue);
        });
        return result;
    }

    private static Map<String, String> normalized(Map<String, String> values, KeyKind kind) {
        TreeMap<String, String> ordered = new TreeMap<>();
        if (values != null) {
            values.forEach((rawKey, rawValue) -> {
                String key = switch (kind) {
                    case PROGRAM -> ArchitectureSupport.program(rawKey);
                    case COPYBOOK, PREFIX -> ArchitectureSupport.text(rawKey, kind.label)
                            .toUpperCase(Locale.ROOT);
                };
                if (kind == KeyKind.PREFIX && !key.matches("[A-Z0-9_-]+")) {
                    throw new IllegalArgumentException("prefix contains unsupported characters");
                }
                String previous = ordered.putIfAbsent(Normalizer.normalize(key, Normalizer.Form.NFC),
                        ArchitectureSupport.moduleName(rawValue));
                if (previous != null && !previous.equals(ArchitectureSupport.moduleName(rawValue))) {
                    throw new IllegalArgumentException("conflicting " + kind.label + " rule: " + key);
                }
            });
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(ordered));
    }

    private enum KeyKind {
        PROGRAM("program"), COPYBOOK("copybook"), PREFIX("prefix");
        private final String label;
        KeyKind(String label) { this.label = label; }
    }
}
