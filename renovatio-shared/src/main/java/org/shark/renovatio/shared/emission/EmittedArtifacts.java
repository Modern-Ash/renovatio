package org.shark.renovatio.shared.emission;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Deterministically ordered immutable artifact collection. */
public record EmittedArtifacts(List<EmittedArtifact> artifacts) {
    public EmittedArtifacts {
        artifacts = (artifacts == null ? List.<EmittedArtifact>of() : artifacts).stream()
                .sorted(Comparator.comparing(EmittedArtifact::path)).toList();
        if (artifacts.stream().map(EmittedArtifact::path).distinct().count() != artifacts.size()) {
            throw new IllegalArgumentException("duplicate artifact path");
        }
    }

    public static EmittedArtifacts of(Collection<EmittedArtifact> artifacts) {
        return new EmittedArtifacts(artifacts == null ? List.of() : List.copyOf(artifacts));
    }

    public static EmittedArtifacts fromUtf8(Map<String, String> values) {
        return of((values == null ? Map.<String, String>of() : values).entrySet().stream()
                .map(entry -> EmittedArtifact.utf8(entry.getKey(), entry.getValue())).toList());
    }

    public Map<String, String> utf8TextByPath() {
        Map<String, String> result = new LinkedHashMap<>();
        artifacts.forEach(artifact -> result.put(artifact.path(), artifact.utf8Text()));
        return java.util.Collections.unmodifiableMap(result);
    }
}
