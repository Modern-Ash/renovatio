package org.shark.renovatio.architecture;

import org.shark.renovatio.profile.MigrationProfile;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Shared interpretation of Architecture Canvas profile extensions for preview and generation layout. */
public final class ArchitectureLayoutOverrides {
    public static final String EXT_PACKAGE_PREFIX = "architecture.package.";
    public static final String EXT_SUFFIX_PREFIX = "architecture.suffix.";
    public static final String EXT_CLASS_PREFIX = "architecture.class.";
    public static final String EXT_RULES = "architecture.dependencyRules";

    private final Map<String, String> packageRoots;
    private final Map<String, String> suffixes;
    private final Map<String, String> classNames;

    private ArchitectureLayoutOverrides(Map<String, String> packageRoots,
                                        Map<String, String> suffixes,
                                        Map<String, String> classNames) {
        this.packageRoots = Map.copyOf(packageRoots);
        this.suffixes = Map.copyOf(suffixes);
        this.classNames = Map.copyOf(classNames);
    }

    public static ArchitectureLayoutOverrides from(MigrationProfile profile) {
        Map<String, Object> extensions = profile == null || profile.extensions() == null
                ? Map.of() : profile.extensions();
        return new ArchitectureLayoutOverrides(map(extensions, EXT_PACKAGE_PREFIX),
                map(extensions, EXT_SUFFIX_PREFIX), map(extensions, EXT_CLASS_PREFIX));
    }

    public String layer(ArchitectureGraph.Component component) {
        Objects.requireNonNull(component, "component");
        return layer(component.kind(), component.name());
    }

    public String layer(ArchitectureGraph.ComponentKind kind, String label) {
        Objects.requireNonNull(kind, "kind");
        String normalizedLabel = key(label == null ? "" : label);
        return switch (kind) {
            case INBOUND_PORT, OUTBOUND_PORT -> "port";
            case ENTITY, VALUE -> "model";
            case SERVICE, USE_CASE -> "service";
            case ADAPTER -> normalizedLabel.contains("controller") || normalizedLabel.contains("inbound")
                    ? "controller" : "adapter";
            case UNRESOLVED -> normalizedLabel.contains("repository") ? "repository" : "service";
        };
    }

    public String className(String label, String defaultClassName, String layer) {
        String direct = classNames.get(label);
        if (direct != null) return javaType(direct, defaultClassName);
        return javaType(defaultClassName + suffixes.getOrDefault(key(layer), ""), defaultClassName);
    }

    public String packageRoot(String layer, String fallback) {
        String normalized = key(layer);
        String root = packageRoots.getOrDefault(normalized, packageRoots.get("base"));
        return root == null || root.isBlank() ? fallback : root;
    }

    public String javaPath(String fallbackPath, String layer) {
        String packageRoot = packageRoot(layer, null);
        if (packageRoot == null) return fallbackPath;
        return packageRoot.replace('.', '/') + "/" + Path.of(fallbackPath).getFileName();
    }

    public String nodePath(String fallbackPath, String layer) {
        String root = packageRoot(layer, null);
        if (root == null) return fallbackPath;
        return root.replace('.', '/').replaceAll("/+", "/") + "/" + Path.of(fallbackPath).getFileName();
    }

    public Map<String, String> packageRoots() {
        return packageRoots;
    }

    public Map<String, String> suffixes() {
        return suffixes;
    }

    public Map<String, String> classNames() {
        return classNames;
    }

    public static String key(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC)
                .trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private static Map<String, String> map(Map<String, Object> extensions, String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        extensions.forEach((key, value) -> {
            if (key.startsWith(prefix) && value != null && !String.valueOf(value).isBlank()) {
                result.put(key(key.substring(prefix.length())), String.valueOf(value).trim());
            }
        });
        return result;
    }

    private static String javaType(String value, String fallback) {
        String compact = (value == null ? "" : value).replaceAll("[^A-Za-z0-9]+", " ").trim();
        if (compact.isBlank()) return fallback;
        StringBuilder result = new StringBuilder();
        for (String part : compact.split("\\s+")) {
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) result.append(part.substring(1));
        }
        return Character.isJavaIdentifierStart(result.charAt(0)) ? result.toString() : fallback;
    }
}
