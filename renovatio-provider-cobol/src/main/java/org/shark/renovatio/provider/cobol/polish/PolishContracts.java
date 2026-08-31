package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

final class PolishContracts {

    private static final String SHA_256 = "[a-f0-9]{64}";
    private static final ObjectMapper JSON = new ObjectMapper();

    private PolishContracts() {
    }

    static String nonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    static String hash(String value, String name) {
        nonBlank(value, name);
        if (!value.matches(SHA_256)) {
            throw new IllegalArgumentException(name + " must be a lowercase SHA-256");
        }
        return value;
    }

    static Set<String> nonBlankSet(Set<String> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        TreeSet<String> result = new TreeSet<>();
        for (String value : values) {
            result.add(nonBlank(value, name));
        }
        return Collections.unmodifiableSet(result);
    }

    static Set<String> nonBlankSetOrEmpty(Set<String> values, String name) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return nonBlankSet(values, name);
    }

    static Set<String> javaPaths(Set<String> paths, String name) {
        Set<String> values = nonBlankSet(paths, name);
        TreeSet<String> result = new TreeSet<>();
        for (String value : values) {
            result.add(javaPath(value));
        }
        return Collections.unmodifiableSet(result);
    }

    static String javaPath(String value) {
        nonBlank(value, "generated Java path");
        Path path = Path.of(value);
        String normalized = path.normalize().toString().replace('\\', '/');
        if (path.isAbsolute() || normalized.equals("..") || normalized.startsWith("../")
                || !normalized.endsWith(".java") || normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Invalid generated Java path: " + value);
        }
        return normalized;
    }

    static String relativeDirectory(String value, String name) {
        nonBlank(value, name);
        Path path = Path.of(value);
        String normalized = path.normalize().toString().replace('\\', '/');
        if (path.isAbsolute() || normalized.equals("..") || normalized.startsWith("../")) {
            throw new IllegalArgumentException(name + " must stay inside the workspace");
        }
        return normalized;
    }

    static Map<String, String> hashMap(Map<String, String> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        TreeMap<String, String> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(nonBlank(key, name), hash(value, name)));
        return Collections.unmodifiableMap(result);
    }

    static Map<String, String> stringMap(Map<String, String> values, String name) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        TreeMap<String, String> result = new TreeMap<>();
        values.forEach((key, value) -> result.put(nonBlank(key, name), nonBlank(value, name)));
        return Collections.unmodifiableMap(result);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    static String canonicalJsonHash(JsonNode value) {
        return sha256(CanonicalJson.write(JSON.convertValue(value, Object.class)));
    }
}
