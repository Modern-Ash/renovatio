package org.shark.renovatio.provider.java.emission;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Aligns generated Java packages and imports with the canonical architecture manifest. */
public final class JavaArchitectureSourceLayout {
    private static final Pattern PACKAGE = Pattern.compile("(?m)^package\\s+[^;]+;\\R?");

    private JavaArchitectureSourceLayout() {
    }

    public static Map<String, String> align(Map<String, String> sourceByPath) {
        Objects.requireNonNull(sourceByPath, "sourceByPath");
        if (sourceByPath.keySet().stream().noneMatch(path -> path.startsWith("modules/"))) {
            return Map.copyOf(sourceByPath);
        }

        Map<String, TypeLocation> types = new LinkedHashMap<>();
        sourceByPath.keySet().stream().sorted().forEach(path -> {
            TypeLocation location = location(path);
            TypeLocation previous = types.putIfAbsent(location.typeName(), location);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate Java type in architecture manifest: "
                        + location.typeName());
            }
        });

        Map<String, String> result = new LinkedHashMap<>();
        sourceByPath.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            TypeLocation own = location(entry.getKey());
            String source = Objects.requireNonNull(entry.getValue(), "Java source");
            Matcher matcher = PACKAGE.matcher(source);
            if (!matcher.find()) {
                throw new IllegalArgumentException("generated Java source has no package: " + entry.getKey());
            }
            List<String> imports = new ArrayList<>();
            types.values().stream()
                    .filter(candidate -> !candidate.typeName().equals(own.typeName()))
                    .filter(candidate -> source.matches("(?s).*\\b" + Pattern.quote(candidate.typeName()) + "\\b.*"))
                    .filter(candidate -> !candidate.packageName().equals(own.packageName()))
                    .map(TypeLocation::qualifiedName)
                    .sorted(Comparator.naturalOrder())
                    .forEach(imports::add);
            StringBuilder declaration = new StringBuilder("package ").append(own.packageName()).append(";\n");
            if (!imports.isEmpty()) {
                declaration.append('\n');
                imports.forEach(value -> declaration.append("import ").append(value).append(";\n"));
            }
            result.put(entry.getKey(), matcher.replaceFirst(Matcher.quoteReplacement(declaration.toString())));
        });
        return Map.copyOf(result);
    }

    private static TypeLocation location(String path) {
        Path normalized = Path.of(Objects.requireNonNull(path, "path")).normalize();
        if (normalized.getNameCount() < 4 || !"modules".equals(normalized.getName(0).toString())) {
            throw new IllegalArgumentException("hexagonal Java path must start with modules/<module>: " + path);
        }
        String module = javaIdentifier(normalized.getName(1).toString());
        StringBuilder suffix = new StringBuilder();
        for (int index = 2; index < normalized.getNameCount() - 1; index++) {
            if (!suffix.isEmpty()) suffix.append('.');
            suffix.append(javaIdentifier(normalized.getName(index).toString()));
        }
        String fileName = normalized.getFileName().toString();
        if (!fileName.endsWith(".java") || fileName.length() == 5) {
            throw new IllegalArgumentException("architecture artifact is not a Java source: " + path);
        }
        String packageName = "org.shark.renovatio.generated.modules." + module + "." + suffix;
        return new TypeLocation(fileName.substring(0, fileName.length() - 5), packageName);
    }

    private static String javaIdentifier(String value) {
        String candidate = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (candidate.isBlank()) return "module";
        return Character.isJavaIdentifierStart(candidate.charAt(0)) ? candidate : "m_" + candidate;
    }

    private record TypeLocation(String typeName, String packageName) {
        private String qualifiedName() {
            return packageName + "." + typeName;
        }
    }
}
