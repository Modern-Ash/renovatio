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
