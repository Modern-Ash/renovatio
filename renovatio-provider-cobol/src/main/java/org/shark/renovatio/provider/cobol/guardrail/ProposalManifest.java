package org.shark.renovatio.provider.cobol.guardrail;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Declares the complete, content-addressed file boundary of a modernization proposal. */
public record ProposalManifest(
        Set<String> declaredPaths,
        Map<String, String> inputHashes,
        Map<String, String> outputHashes) {

    private static final String SHA_256 = "[a-f0-9]{64}";

    public ProposalManifest {
        declaredPaths = Set.copyOf(validatePaths(declaredPaths));
        inputHashes = Map.copyOf(validateHashes(inputHashes, "inputHashes"));
        outputHashes = Map.copyOf(validateHashes(outputHashes, "outputHashes"));
        if (!declaredPaths.containsAll(outputHashes.keySet())) {
            throw new IllegalArgumentException("Every output hash path must be declared");
        }
    }

    private static Set<String> validatePaths(Set<String> paths) {
        Objects.requireNonNull(paths, "declaredPaths");
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("At least one declared path is required");
        }
        Set<String> validated = new LinkedHashSet<>();
        for (String value : paths) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Declared paths must not be blank");
            }
            Path path = Path.of(value);
            String normalized = path.normalize().toString().replace('\\', '/');
            if (path.isAbsolute() || normalized.equals("..") || normalized.startsWith("../")) {
                throw new IllegalArgumentException("Declared paths must stay inside the workspace: " + value);
            }
            validated.add(normalized);
        }
        return validated;
    }

    private static Map<String, String> validateHashes(Map<String, String> hashes, String name) {
        Objects.requireNonNull(hashes, name);
        Map<String, String> validated = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            String path = validatePaths(Set.of(entry.getKey())).iterator().next();
            String hash = entry.getValue();
            if (hash == null || !hash.matches(SHA_256)) {
                throw new IllegalArgumentException(name + " must contain lowercase SHA-256 values");
            }
            validated.put(path, hash);
        }
        return validated;
    }
}
