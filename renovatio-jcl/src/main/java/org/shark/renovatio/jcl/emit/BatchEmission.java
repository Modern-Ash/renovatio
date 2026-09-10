package org.shark.renovatio.jcl.emit;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Deterministically ordered generated files plus explicit manual actions. */
public record BatchEmission(Map<String, String> files, List<String> manualActionItems) {
    public BatchEmission {
        files = files == null ? Map.of() : java.util.Collections.unmodifiableMap(new TreeMap<>(files));
        manualActionItems = manualActionItems == null ? List.of() : List.copyOf(manualActionItems);
    }
}
